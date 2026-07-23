import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogPortal, DialogOverlay } from "@/components/ui/dialog";
import { FileText, Loader2, X } from "lucide-react";
import { LABELS } from "@/config/labels.config";

const labels = LABELS.adminComponents.kycPreviewModal;

export const KycPreviewModal = ({ isOpen, onClose, docUrl, docType }) => {
  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogPortal>
        <DialogOverlay className="fixed inset-0 z-[100] bg-black/60 backdrop-blur-sm" />

        <DialogContent className="fixed left-[50%] top-[50%] z-[101] w-full max-w-4xl translate-x-[-50%] translate-y-[-50%] h-[85vh] p-0 overflow-hidden border-none shadow-2xl flex flex-col bg-white [&>button]:hidden">
          <DialogHeader className="sr-only">
            <DialogTitle>{labels.title}</DialogTitle>
          </DialogHeader>

          <div className="h-12 border-b bg-slate-900 text-white flex items-center justify-between px-6 shrink-0">
            <h3 className="text-[10px] font-black uppercase tracking-widest flex items-center gap-2">
              <FileText className="w-3.5 h-3.5 text-primary" /> {labels.inspectorTitle}
            </h3>
            <Button variant="ghost" className="h-8 text-white hover:bg-white/10" onClick={onClose}>
              <X className="w-5 h-5" />
            </Button>
          </div>

          <div className="flex-1 bg-slate-100 p-4 flex items-center justify-center overflow-hidden">
            {!docUrl ? (
              <Loader2 className="animate-spin text-slate-400" />
            ) : docType?.includes("pdf") ? (
              <iframe src={docUrl} className="w-full h-full rounded bg-white shadow-inner" title={labels.iframeTitle} />
            ) : (
              <img src={docUrl} alt={labels.imageAlt} className="max-w-full max-h-full object-contain rounded shadow-lg bg-white" />
            )}
          </div>
        </DialogContent>
      </DialogPortal>
    </Dialog>
  );
};
