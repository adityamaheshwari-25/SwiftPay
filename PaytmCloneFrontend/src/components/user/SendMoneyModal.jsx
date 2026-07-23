import { useState, useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { BadgeCheck, Loader2 } from "lucide-react";
import { LABELS } from "@/config/labels.config";
import { userService } from "@/services/api/userService";
import { walletTransferSchema } from "@/schemas/walletTransferSchema";
import { toast } from "sonner";

export const SendMoneyModal = ({ open, onOpenChange, onSubmit, isLoading }) => {
  const labels = LABELS.userComponents.sendMoneyModal;
  const rupee = LABELS.splits.common.rupee;

  const [receiver, setReceiver] = useState(null);
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupNotFound, setLookupNotFound] = useState(false);

  const {
    control,
    handleSubmit,
    watch,
    formState: { errors },
    reset,
  } = useForm({
    resolver: zodResolver(walletTransferSchema),
    defaultValues: {
      receiverMobile: "",
      amount: "",
      mpin: "",
      paymentMode: "WALLET",
    },
  });

  const mobileNumber = watch("receiverMobile");

  useEffect(() => {
    if (mobileNumber?.length === 10) {
      setLookupLoading(true);
      setLookupNotFound(false);
      userService
        .lookupByMobile(mobileNumber)
        .then((res) => {
          const found = !!res;
          setReceiver(found ? res : null);
          setLookupNotFound(!found);
        })
        .catch(() => {
          setReceiver(null);
          setLookupNotFound(true);
        })
        .finally(() => setLookupLoading(false));
    } else {
      setReceiver(null);
      setLookupNotFound(false);
    }
  }, [mobileNumber]);

  const onInternalSubmit = async (values) => {
    if (!values.receiverMobile || !values.amount || !values.mpin) {
      toast.error(labels.fillAllFields);
      return;
    }

    const payload = {
      receiverMobile: String(values.receiverMobile).trim(),
      amount: parseFloat(values.amount),
      mpin: String(values.mpin).trim(),
    };

    try {
      const success = await onSubmit(payload);

      if (success) {
        reset();
        setReceiver(null);
        onOpenChange(false);
      }
    } catch (err) {
      console.error("Submission failed", err);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader><DialogTitle>{labels.title}</DialogTitle></DialogHeader>

        <form onSubmit={handleSubmit(onInternalSubmit)} className="space-y-4 pt-4">
          <div className="space-y-2">
            <Label>{labels.recipientMobile}</Label>
            <div className="relative">
              <Controller
                name="receiverMobile"
                control={control}
                render={({ field }) => (
                  <Input
                    {...field}
                    placeholder={labels.mobilePlaceholder}
                    maxLength={10}
                    inputMode="numeric"
                    onChange={(e) => field.onChange(String(e.target.value || "").replace(/\D/g, "").slice(0, 10))}
                  />
                )}
              />
              {lookupLoading && <Loader2 className="absolute right-3 top-3 animate-spin h-4 w-4 text-muted-foreground" />}
            </div>
            {errors.receiverMobile && <p className="text-xs text-destructive">{errors.receiverMobile.message}</p>}
            {!lookupLoading && lookupNotFound && (
              <p className="text-xs text-amber-700">{labels.noRecipientFound}</p>
            )}
          </div>

          {receiver && (
            <div className="bg-green-50 p-3 rounded-lg flex items-center justify-between border border-green-200">
              <div className="flex flex-col">
                <span className="text-[10px] uppercase font-bold text-green-700">{labels.verifiedRecipient}</span>
                <span className="font-bold text-green-900">{receiver.displayName}</span>
              </div>
              <BadgeCheck className="text-green-600" />
            </div>
          )}

          {receiver && (
            <div className="space-y-4 animate-in fade-in slide-in-from-top-2">
              <div className="space-y-2">
                <Label>{labels.amountLabel} ({rupee})</Label>
                <Controller
                  name="amount"
                  control={control}
                  render={({ field }) => (
                    <Input type="number" placeholder={labels.amountPlaceholder} onChange={(e) => field.onChange(parseFloat(e.target.value))} />
                  )}
                />
                {errors.amount && <p className="text-xs text-destructive">{errors.amount.message}</p>}
              </div>

              <div className="space-y-2">
                <Label>{labels.secureMpin}</Label>
                <Controller
                  name="mpin"
                  control={control}
                  render={({ field }) => (
                    <Input {...field} type="password" placeholder={labels.mpinPlaceholder} maxLength={4} autoComplete="off" />
                  )}
                />
                {errors.mpin && <p className="text-xs text-destructive">{errors.mpin.message}</p>}
              </div>

              <Button type="submit" className="w-full h-12" disabled={isLoading}>
                {isLoading ? labels.transferring : `${labels.title} ${rupee}${watch("amount") || 0}`}
              </Button>
            </div>
          )}
        </form>
      </DialogContent>
    </Dialog>
  );
};
