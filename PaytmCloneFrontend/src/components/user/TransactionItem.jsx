import { motion } from "framer-motion";
import { ArrowUpRight, ArrowDownLeft, Info, Clock } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { LABELS } from "@/config/labels.config";

export const TransactionItem = ({ tx }) => {
  const labels = LABELS.userComponents.transactionItem;
  const rupee = LABELS.splits.common.rupee;
  const MotionDiv = motion.div;

  if (!tx) return null;

  const isSuccess = tx.status === "SUCCESS";
  const isFailed = tx.status === "FAILED";
  const isPending = tx.status === "PENDING";

  const amountColor = isSuccess ? (tx.credit ? "text-green-600" : "text-foreground") : isFailed ? "text-destructive" : "text-yellow-600";

  return (
    <MotionDiv initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.25 }} className="group rounded-xl px-4 py-3 hover:bg-muted/40 transition-all">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-4">
          <div
            className={`mt-0.5 flex h-10 w-10 items-center justify-center rounded-full ring-1 ring-border ${
              tx.credit ? "bg-green-100 text-green-700" : "bg-secondary text-secondary-foreground"
            }`}
          >
            {tx.credit ? <ArrowDownLeft size={18} /> : <ArrowUpRight size={18} />}
          </div>

          <div className="space-y-0.5">
            <div className="flex items-center gap-2">
              <p className="text-sm font-semibold leading-none">{tx.counterPartyName}</p>

              <Badge variant="outline" className="text-[10px] px-2 py-0.5 font-medium uppercase">
                {tx.paymentMode}
              </Badge>

              {isPending && <Badge className="bg-yellow-100 text-yellow-700 text-[10px] px-2">{labels.pending}</Badge>}

              {isFailed && <Badge className="bg-destructive/10 text-destructive text-[10px] px-2">{labels.failed}</Badge>}
            </div>

            <div className="flex items-center gap-1.5 text-[11px] text-muted-foreground">
              <Clock size={12} />
              {new Date(tx.createdAt).toLocaleDateString("en-IN", {
                day: "2-digit",
                month: "short",
                hour: "2-digit",
                minute: "2-digit",
              })}
            </div>
          </div>
        </div>

        <div className="text-right">
          <p className={`text-base font-extrabold ${amountColor}`}>
            {tx.credit ? "+" : "-"} {rupee}{tx.amount.toLocaleString("en-IN")}
          </p>
        </div>
      </div>

      {isFailed && tx.failureReason && (
        <div className="mt-3 ml-14 flex gap-2 rounded-lg bg-destructive/10 p-2.5 text-destructive">
          <Info size={14} className="mt-0.5 shrink-0" />
          <p className="text-[11px] font-medium leading-snug">{tx.failureReason}</p>
        </div>
      )}

      {tx.narration && !isFailed && <p className="mt-2 ml-14 text-[11px] italic text-muted-foreground">"{tx.narration}"</p>}
    </MotionDiv>
  );
};
