import { useMemo, useRef, useState } from "react";
import { toast } from "sonner";
import { LABELS } from "@/config/labels.config";
import { userService } from "@/services/api/userService";
import { PER_HEAD_NOT_DIVISIBLE, SPLIT_TYPES } from "./constants";
import { isMoneyLike, normalizeMoneyString, toPaise } from "./utils";

export const useCreateSplitForm = ({ onCreate, onOpenChange }) => {
  const labels = LABELS.splits;

  const [mobile, setMobile] = useState("");
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupUser, setLookupUser] = useState(null);
  const [lookupNotFound, setLookupNotFound] = useState(false);

  const [members, setMembers] = useState([]);
  const [amount, setAmount] = useState("");
  const [note, setNote] = useState("");

  const [splitType, setSplitType] = useState(SPLIT_TYPES.EQUAL);
  const [customShares, setCustomShares] = useState({});
  // Tracks the latest lookup request so stale async responses can be ignored.
  const lookupRequestRef = useRef(0);

  const handleMobileChange = (nextMobile) => {
    const sanitizedMobile = String(nextMobile || "").replace(/\D/g, "").slice(0, 10);
    setMobile(sanitizedMobile);

    // Lookup only starts once we have a full 10-digit mobile.
    if (sanitizedMobile?.length !== 10) {
      setLookupUser(null);
      setLookupNotFound(false);
      setLookupLoading(false);
      return;
    }

    const requestId = lookupRequestRef.current + 1;
    lookupRequestRef.current = requestId;
    setLookupLoading(true);
    setLookupNotFound(false);

    userService
      .lookupByMobile(sanitizedMobile)
      .then((res) => {
        if (lookupRequestRef.current !== requestId) return;
        const found = !!res;
        setLookupUser(found ? res : null);
        setLookupNotFound(!found);
      })
      .catch(() => {
        if (lookupRequestRef.current !== requestId) return;
        setLookupUser(null);
        setLookupNotFound(true);
      })
      .finally(() => {
        if (lookupRequestRef.current !== requestId) return;
        setLookupLoading(false);
      });
  };

  const memberMobiles = useMemo(() => members.map((member) => member.mobile), [members]);
  const totalPaise = useMemo(() => toPaise(amount), [amount]);

  // Equal-split preview is only shown when amount is valid and divisible by member count.
  const perHeadPreview = useMemo(() => {
    const participantCount = members.length;
    if (!participantCount) return null;
    if (!Number.isFinite(totalPaise) || totalPaise <= 0) return null;
    if (totalPaise % participantCount !== 0) return PER_HEAD_NOT_DIVISIBLE;

    return (totalPaise / participantCount / 100).toFixed(2);
  }, [members.length, totalPaise]);

  const customSumPaise = (() => {
    // Running sum of custom shares in paise to avoid floating-point issues.
    if (splitType !== SPLIT_TYPES.CUSTOM) return 0;

    let sum = 0;
    for (const memberMobile of memberMobiles) {
      const paise = toPaise(customShares[memberMobile]);
      if (!Number.isFinite(paise)) return Number.NaN;
      sum += paise;
    }

    return sum;
  })();

  const customValidation = (() => {
    // For EQUAL flow, custom validation is irrelevant.
    if (splitType !== SPLIT_TYPES.CUSTOM) return { ok: true };

    if (members.length < 1) return { ok: false, msg: labels.toasts.addParticipant };
    if (!Number.isFinite(totalPaise) || totalPaise <= 0) return { ok: false, msg: labels.toasts.enterValidAmount };

    for (const memberMobile of memberMobiles) {
      const shareValue = customShares[memberMobile];
      if (!shareValue || !String(shareValue).trim()) {
        return { ok: false, msg: labels.validation.enterAllShares };
      }

      if (!isMoneyLike(shareValue)) {
        return { ok: false, msg: labels.validation.sharesMaxTwoDecimals };
      }

      const paise = toPaise(shareValue);
      if (!Number.isFinite(paise) || paise <= 0) {
        return { ok: false, msg: labels.validation.shareMustBePositive };
      }
    }

    if (!Number.isFinite(customSumPaise)) {
      return { ok: false, msg: labels.validation.invalidShareAmounts };
    }

    if (customSumPaise !== totalPaise) {
      const remaining = (totalPaise - customSumPaise) / 100;
      return {
        ok: false,
        msg: `${labels.validation.customShareTotalMismatchPrefix} ${labels.common.rupee}${remaining.toFixed(2)}`,
      };
    }

    return { ok: true };
  })();

  const handleSplitTypeChange = (nextSplitType) => {
    setSplitType(nextSplitType);
    if (nextSplitType !== SPLIT_TYPES.CUSTOM) return;

    // Ensure every current member has an editable custom-share field.
    setCustomShares((prev) => {
      const next = { ...prev };
      for (const member of members) {
        if (next[member.mobile] === undefined) {
          next[member.mobile] = "";
        }
      }
      return next;
    });
  };

  const addMember = () => {
    if (!lookupUser) {
      toast.error(labels.toasts.noUserFound);
      return;
    }

    if (members.some((member) => member.mobile === lookupUser.mobile)) {
      toast.error(labels.toasts.alreadyAdded);
      return;
    }

    setMembers((prev) => [
      ...prev,
      {
        mobile: lookupUser.mobile,
        name: lookupUser.displayName,
        userId: lookupUser.userId,
      },
    ]);

    if (splitType === SPLIT_TYPES.CUSTOM) {
      // If already in CUSTOM mode, initialize share input for the new member.
      setCustomShares((prev) => ({
        ...prev,
        [lookupUser.mobile]: prev[lookupUser.mobile] ?? "",
      }));
    }

    setMobile("");
    setLookupUser(null);
    setLookupNotFound(false);
    setLookupLoading(false);
    // Invalidate any in-flight lookup response from a previous input.
    lookupRequestRef.current += 1;
  };

  const removeMember = (member) => {
    setMembers((prev) => prev.filter((current) => current.mobile !== member.mobile));

    setCustomShares((prev) => {
      const next = { ...prev };
      delete next[member.mobile];
      return next;
    });
  };

  const resetForm = () => {
    // Full reset after successful create.
    setMembers([]);
    setAmount("");
    setNote("");
    setMobile("");
    setLookupUser(null);
    setLookupNotFound(false);
    setLookupLoading(false);
    lookupRequestRef.current += 1;
    setSplitType(SPLIT_TYPES.EQUAL);
    setCustomShares({});
  };

  const submit = async () => {
    // Step 1: common field validation.
    if (members.length < 1) return toast.error(labels.toasts.addParticipant);
    if (!amount || Number(amount) < 1) return toast.error(labels.toasts.enterValidAmount);
    if (!isMoneyLike(amount)) return toast.error(labels.toasts.amountMaxTwoDecimals);

    if (splitType === SPLIT_TYPES.EQUAL && perHeadPreview === PER_HEAD_NOT_DIVISIBLE) {
      return toast.error(labels.toasts.equalAmountDivisibility);
    }

    if (splitType === SPLIT_TYPES.CUSTOM && !customValidation.ok) {
      return toast.error(customValidation.msg || labels.toasts.invalidCustomSplit);
    }

    // Step 2: normalize numbers to backend-safe decimal format.
    const payload = {
      amount: Number(normalizeMoneyString(amount)),
      memberMobiles,
      note: note?.trim() || null,
      splitType,
    };

    if (splitType === SPLIT_TYPES.CUSTOM) {
      const normalizedShares = {};
      for (const memberMobile of memberMobiles) {
        normalizedShares[memberMobile] = Number(normalizeMoneyString(customShares[memberMobile]));
      }
      payload.customShares = normalizedShares;
    }

    // Step 3: submit to parent action and reset on success only.
    const ok = await onCreate(payload);
    if (!ok) return;

    resetForm();
    onOpenChange(false);
  };

  return {
    mobile,
    setMobile: handleMobileChange,
    lookupLoading,
    lookupUser,
    lookupNotFound,
    members,
    amount,
    setAmount,
    note,
    setNote,
    splitType,
    setSplitType: handleSplitTypeChange,
    customShares,
    setCustomShares,
    perHeadPreview,
    totalPaise,
    customSumPaise,
    customValidation,
    addMember,
    removeMember,
    submit,
  };
};
