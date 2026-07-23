import { format } from "date-fns";
import { ArrowUpRight, Zap } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { LABELS } from "@/config/labels.config";

export const SettlementItem = ({ set }) => {
  const labels = LABELS.merchantComponents.settlementItem;
  const rupee = LABELS.splits.common.rupee;

  if (!set) return null;

  return (
    <div className="flex items-center justify-between px-5 py-4 hover:bg-slate-50 transition-colors">
      <div className="flex items-center gap-4">
        <div className="h-10 w-10 rounded-full bg-indigo-100/60 text-indigo-600 flex items-center justify-center">
          {set.isInstant ? <Zap className="w-5 h-5 fill-current" /> : <ArrowUpRight className="w-5 h-5" />}
        </div>

        <div>
          <div className="flex items-center gap-2">
            <p className="text-sm font-bold text-foreground">{set.destinationBankName}</p>

            {set.isInstant && (
              <Badge className="h-3 px-1 text-[8px] font-black border-none bg-amber-100 text-amber-700">
                {labels.instant}
              </Badge>
            )}
          </div>

          <p className="text-[10px] font-bold uppercase tracking-wide text-muted-foreground">
            {labels.accountPrefix} •••• {set.accountNumberTail} • {format(new Date(set.settledAt), "dd MMM")}
          </p>
        </div>
      </div>

      <div className="text-right space-y-1">
        <p className="text-sm font-black text-foreground">-{rupee}{set.amount.toLocaleString("en-IN")}</p>

        <p className="text-[10px] text-slate-400">{labels.feePrefix} {rupee}{set.fee.toLocaleString("en-IN")}</p>

        <Badge variant="outline" className="h-4 px-2 text-[9px] font-black uppercase text-muted-foreground border-slate-200">
          {set.status}
        </Badge>
      </div>
    </div>
  );
};
