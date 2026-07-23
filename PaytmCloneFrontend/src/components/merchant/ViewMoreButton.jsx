import { History } from "lucide-react";
import { LABELS } from "@/config/labels.config";

export const ViewMoreButton = ({ count, onClick }) => {
  const labels = LABELS.merchantComponents.viewMoreButton;

  return (
    <button
      onClick={onClick}
      className="w-full py-3 bg-slate-50/50 hover:bg-slate-100 transition-colors flex items-center justify-center gap-2 group border-t"
    >
      <History className="w-3 h-3 text-muted-foreground group-hover:text-primary" />
      <span className="text-[10px] font-black text-muted-foreground uppercase tracking-widest group-hover:text-primary">
        {labels.viewAllPrefix} {count} {labels.recordsSuffix}
      </span>
    </button>
  );
};
