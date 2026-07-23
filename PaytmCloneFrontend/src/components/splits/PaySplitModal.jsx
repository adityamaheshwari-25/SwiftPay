import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { LABELS } from "@/config/labels.config";
import { Lock, Loader2 } from "lucide-react";

export const PaySplitModal = ({ open, onOpenChange, split, onPay, isLoading }) => {
  const labels = LABELS.splits.payModal;
  const common = LABELS.splits.common;
  const [mpin, setMpin] = useState("");

  const submit = async () => {
    const ok = await onPay({ splitId: split.splitId, mpin });
    if (ok) {
      setMpin("");
      onOpenChange(false);
    }
  };

  if (!split) return null;
  const payableAmount = Number(split.myShare ?? 0);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[420px]">
        <DialogHeader>
          <DialogTitle>{labels.title}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 pt-2">
          <div className="rounded-lg border bg-muted/30 p-3">
            <div className="text-xs text-muted-foreground">{labels.splitLabel}</div>
            <div className="font-bold">{common.rupee}{payableAmount.toFixed(2)}</div>
            {split.note ? <div className="text-xs text-muted-foreground mt-1">{split.note}</div> : null}
          </div>

          <div className="space-y-2">
            <Label>{labels.enterMpinLabel}</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-3 w-4 h-4 text-muted-foreground" />
              <Input
                value={mpin}
                onChange={(e) => setMpin(e.target.value)}
                type="password"
                placeholder={labels.mpinPlaceholder}
                maxLength={4}
                className="pl-10 h-12 text-lg tracking-[0.35em]"
                autoComplete="off"
              />
            </div>
          </div>

          <Button className="w-full h-12 font-bold" disabled={isLoading || mpin.length !== 4} onClick={submit}>
            {isLoading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
            {labels.payNow}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
