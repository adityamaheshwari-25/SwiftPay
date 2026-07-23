import { format } from "date-fns";
import { ArrowDownLeft } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { LABELS } from "@/config/labels.config";

export const TransactionItem = ({ tx }) => {
  const labels = LABELS.merchantComponents.transactionItem;
  const rupee = LABELS.splits.common.rupee;

  if (!tx) return null;

  const isSuccess = tx.status === "SUCCESS";

  return (
    <div className="flex items-center justify-between px-5 py-4 hover:bg-slate-50 transition-colors">
      <div className="flex items-center gap-4">
        <div className="h-10 w-10 rounded-full bg-emerald-100/60 text-emerald-600 flex items-center justify-center">
          <ArrowDownLeft className="w-5 h-5" />
        </div>

        <div>
          <p className="text-sm font-bold text-foreground">{tx.customerName || labels.directPayment}</p>
          <div className="flex items-center gap-2 text-[10px] font-bold uppercase tracking-wide text-muted-foreground">
            <span>{tx.paymentMode}</span>
            <span>•</span>
            <span>{format(new Date(tx.createdAt), "dd MMM, hh:mm a")}</span>
          </div>
        </div>
      </div>

      <div className="text-right space-y-1">
        <p className="font-black text-slate-900">{rupee}{tx.amount.toLocaleString("en-IN")}</p>

        <Badge
          className={`h-4 px-2 text-[9px] font-black border-none ${
            isSuccess ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"
          }`}
        >
          {tx.status}
        </Badge>
      </div>
    </div>
  );
};
