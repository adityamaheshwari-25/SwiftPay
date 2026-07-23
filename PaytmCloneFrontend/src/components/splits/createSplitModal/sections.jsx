import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2, Plus, X } from "lucide-react";
import { LABELS } from "@/config/labels.config";
import { PER_HEAD_NOT_DIVISIBLE, SPLIT_TYPES } from "./constants";

const labels = LABELS.splits.createSections;
const common = LABELS.splits.common;
const toasts = LABELS.splits.toasts;

// SplitTypeSection:
// Lets user choose between equal split and custom split.
// Parent owns state and passes current value + setter callback.
// This component is intentionally stateless and purely presentational.
export const SplitTypeSection = ({ splitType, onSplitTypeChange }) => (
  <div className="space-y-2">
    <Label>{labels.splitTypeLabel}</Label>
    <div className="grid grid-cols-2 gap-2">
      <Button
        type="button"
        variant={splitType === SPLIT_TYPES.EQUAL ? "default" : "outline"}
        onClick={() => onSplitTypeChange(SPLIT_TYPES.EQUAL)}
        className="h-10"
      >
        {labels.equal}
      </Button>
      <Button
        type="button"
        variant={splitType === SPLIT_TYPES.CUSTOM ? "default" : "outline"}
        onClick={() => onSplitTypeChange(SPLIT_TYPES.CUSTOM)}
        className="h-10"
      >
        {labels.custom}
      </Button>
    </div>
    <div className="text-xs text-muted-foreground">
      {splitType === SPLIT_TYPES.EQUAL
        ? labels.equalHint
        : labels.customHint}
    </div>
  </div>
);

// ParticipantLookupSection:
// Handles "find user by mobile" UI.
// Expected flow:
// 1) user types 10-digit mobile (parent triggers lookup)
// 2) lookupLoading shows spinner
// 3) resolved user is shown with name + mobile
// 4) "Add" calls onAddMember and parent moves user into members list
export const ParticipantLookupSection = ({
  mobile,
  setMobile,
  lookupLoading,
  lookupUser,
  lookupNotFound,
  onAddMember,
}) => (
  <div className="space-y-2">
    <Label>{labels.participantMobileLabel}</Label>
    <div className="flex gap-2">
      <div className="flex-1 relative">
        {/* inputMode numeric improves mobile keyboard UX */}
        <Input
          value={mobile}
          onChange={(event) => setMobile(event.target.value)}
          placeholder={labels.mobilePlaceholder}
          maxLength={10}
          inputMode="numeric"
        />
        {/* Spinner is displayed only during active async lookup */}
        {lookupLoading ? (
          <Loader2 className="absolute right-3 top-3 h-4 w-4 animate-spin text-muted-foreground" />
        ) : null}
      </div>
      <Button type="button" onClick={onAddMember} disabled={!lookupUser}>
        <Plus className="h-4 w-4 mr-1" /> {labels.add}
      </Button>
    </div>

    {/* Resolved lookup preview helps user verify identity before adding */}
    {lookupUser ? (
      <div className="rounded-lg border bg-muted/30 p-3 flex items-center justify-between">
        <div className="flex flex-col">
          <span className="text-[10px] uppercase font-bold text-muted-foreground">{labels.foundUser}</span>
          <span className="font-bold">{lookupUser.displayName}</span>
        </div>
        <Badge variant="outline">{lookupUser.mobile}</Badge>
      </div>
    ) : null}

    {!lookupLoading && lookupNotFound ? (
      <p className="text-xs text-amber-700">{toasts.noUserFound}</p>
    ) : null}
  </div>
);

// AmountAndNoteSection:
// Collects total split amount + optional note.
// Also shows equal-split per-head preview/validation hint when applicable.
export const AmountAndNoteSection = ({
  amount,
  onAmountChange,
  note,
  onNoteChange,
  membersCount,
  splitType,
  perHeadPreview,
}) => (
  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
    <div className="space-y-2">
      <Label>{labels.totalAmountLabel} ({common.rupee})</Label>
      <Input
        type="text"
        // Decimal keyboard + free-text input to allow controlled validation in parent.
        inputMode="decimal"
        value={amount}
        onChange={(event) => onAmountChange(event.target.value)}
        placeholder={labels.amountPlaceholder}
      />

      {/* Equal-only helper text:
          - Shows non-divisible warning using paise precision rule.
          - Otherwise shows "each pays" preview. */}
      {membersCount > 0 && amount && splitType === SPLIT_TYPES.EQUAL ? (
        <div className="text-xs">
          {perHeadPreview === PER_HEAD_NOT_DIVISIBLE ? (
            <span className="text-destructive font-bold">{labels.notDivisiblePrefix} {membersCount}.</span>
          ) : perHeadPreview ? (
            <span className="text-muted-foreground">
              {labels.eachPrefix} <span className="font-bold text-foreground">{common.rupee}{perHeadPreview}</span>
            </span>
          ) : null}
        </div>
      ) : null}
    </div>

    <div className="space-y-2">
      <Label>{labels.noteLabel}</Label>
      <Input value={note} onChange={(event) => onNoteChange(event.target.value)} placeholder={labels.notePlaceholder} />
    </div>
  </div>
);

// ParticipantsSection:
// Displays members list and (for CUSTOM) per-member editable share inputs.
// Also shows entered/remaining summary and validation errors from parent.
// Parent keeps all source-of-truth state (members, customShares, validation).
export const ParticipantsSection = ({
  splitType,
  members,
  totalPaise,
  customSumPaise,
  customValidation,
  customShares,
  setCustomShares,
  onRemoveMember,
}) => (
  <div className="space-y-2">
    <div className="flex items-start justify-between gap-3">
      <Label>{labels.participants}</Label>

      {/* CUSTOM summary block:
          - Entered = sum(custom shares)
          - Remaining = total - entered
          This gives real-time feedback before submit. */}
      {splitType === SPLIT_TYPES.CUSTOM && members.length > 0 ? (
        <div className="text-xs text-muted-foreground text-right">
          <div>
            {labels.entered}{" "}
            <span className="font-bold text-foreground">
              {common.rupee}{Number.isFinite(customSumPaise) ? (customSumPaise / 100).toFixed(2) : common.dash}
            </span>
          </div>
          <div>
            {labels.remaining}{" "}
            <span className="font-bold text-foreground">
              {Number.isFinite(totalPaise) && Number.isFinite(customSumPaise)
                ? `${common.rupee}${((totalPaise - customSumPaise) / 100).toFixed(2)}`
                : common.dash}
            </span>
          </div>
        </div>
      ) : null}
    </div>

    {/* Server-compatible validation messages are computed by hook and shown here */}
    {!customValidation.ok && splitType === SPLIT_TYPES.CUSTOM && customValidation.msg ? (
      <p className="text-xs text-destructive mt-1">{customValidation.msg}</p>
    ) : null}

    {/* Empty state keeps modal layout stable and guides the user on next action */}
    {members.length === 0 ? (
      <div className="rounded-lg border border-dashed p-4 text-center bg-muted/20">
        <p className="text-sm text-muted-foreground">{labels.noParticipants}</p>
        <p className="text-xs text-muted-foreground mt-1">{labels.searchHint}</p>
      </div>
    ) : (
      <div className="border border-border/60 rounded-lg overflow-hidden bg-background">
        {/* Header row appears only in CUSTOM mode to label share input column */}
        {splitType === SPLIT_TYPES.CUSTOM ? (
          <div className="grid grid-cols-[1fr_120px_40px] gap-3 px-3 py-2 bg-muted/20 text-xs font-semibold text-muted-foreground">
            <div>{labels.participantHeader}</div>
            <div className="text-right">{labels.shareHeader} ({common.rupee})</div>
            <div />
          </div>
        ) : null}

        {/* Scroll container prevents modal from expanding too tall with many members */}
        <div className="max-h-[280px] overflow-y-auto divide-y divide-border/60 bg-white">
          {members.map((member) => (
            <div
              key={member.mobile}
              className="grid grid-cols-[1fr_120px_40px] items-center gap-3 px-3 py-2"
            >
              <div className="min-w-0">
                {/* Truncate protects layout on long names */}
                <div className="font-semibold text-sm truncate">{member.name}</div>
                <div className="text-xs text-muted-foreground">{member.mobile}</div>
              </div>

              <div className="justify-self-end w-[120px]">
                {/* Share input shown only for CUSTOM;
                    EQUAL keeps reserved space to avoid layout jump */}
                {splitType === SPLIT_TYPES.CUSTOM ? (
                  <Input
                    type="text"
                    inputMode="decimal"
                    placeholder={labels.shareInputPlaceholder}
                    className="h-9 text-right"
                    value={customShares[member.mobile] ?? ""}
                    onChange={(event) =>
                      setCustomShares((prev) => ({ ...prev, [member.mobile]: event.target.value }))
                    }
                  />
                ) : (
                  <div className="h-9 flex items-center justify-end text-xs text-muted-foreground pr-2">{common.dash}</div>
                )}
              </div>

              {/* Remove member action */}
              <div className="justify-self-end">
                <Button variant="ghost" size="icon" onClick={() => onRemoveMember(member)}>
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </div>
          ))}
        </div>

        {/* Bottom helper for custom mode to reduce invalid submissions */}
        {splitType === SPLIT_TYPES.CUSTOM ? (
          <div className="px-3 py-2 bg-muted/30 text-xs text-muted-foreground flex items-center justify-between">
            <span>{labels.shareHintLeft}</span>
            <span className="font-medium">{labels.shareHintRight}</span>
          </div>
        ) : null}
      </div>
    )}
  </div>
);
