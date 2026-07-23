import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Loader2 } from "lucide-react";
import { LABELS } from "@/config/labels.config";

const labels = LABELS.adminComponents.kycRejectModal;

export const KycRejectModal = ({ isOpen, onClose, onConfirm, isPending, reason, setReason }) => (
  <Dialog open={isOpen} onOpenChange={onClose}>
    <DialogContent>
      <DialogHeader>
        <DialogTitle className="font-black text-slate-900">{labels.title}</DialogTitle>
      </DialogHeader>
      <div className="py-4 space-y-3">
        <p className="text-xs font-bold text-slate-500 uppercase">{labels.reasonLabel}</p>
        <Textarea
          placeholder={labels.reasonPlaceholder}
          className="min-h-[120px]"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
      </div>
      <DialogFooter>
        <Button variant="ghost" onClick={onClose} disabled={isPending}>{labels.cancel}</Button>
        <Button variant="destructive" onClick={onConfirm} disabled={isPending || !reason.trim()}>
          {isPending ? <Loader2 className="animate-spin w-4 h-4 mr-2" /> : labels.confirmRejection}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
);
