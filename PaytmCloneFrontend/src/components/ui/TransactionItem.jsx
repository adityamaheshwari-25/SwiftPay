import { motion } from "framer-motion";
import { ArrowDownLeft, ArrowUpRight, Building2, ShoppingBag, User } from "lucide-react";

const categoryIcons = { transfer: ArrowUpRight, shopping: ShoppingBag, bank: Building2, person: User };

export function TransactionItem({ transaction, index = 0 }) {
  const Icon = categoryIcons[transaction.category];
  const isCredit = transaction.type === "credit";

  return (
    <motion.div initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: index * 0.05, duration: 0.3 }} className="flex items-center gap-4 p-4 bg-card rounded-xl border border-border/50 hover:shadow-card transition-shadow">
      <div className={`p-3 rounded-xl ${isCredit ? "bg-success/10 text-success" : "bg-destructive/10 text-destructive"}`}>
        {isCredit ? <ArrowDownLeft className="w-5 h-5" /> : <Icon className="w-5 h-5" />}
      </div>
      <div className="flex-1 min-w-0">
        <p className="font-medium text-foreground truncate">{transaction.description}</p>
        <p className="text-sm text-muted-foreground">{transaction.date}</p>
      </div>
      <div className="text-right">
        <p className={`font-semibold ${isCredit ? "text-success" : "text-foreground"}`}>{isCredit ? "+" : "-"}₹{transaction.amount.toLocaleString("en-IN")}</p>
        <p className={`text-xs ${transaction.status === "completed" ? "text-success" : transaction.status === "pending" ? "text-warning" : "text-destructive"}`}>{transaction.status}</p>
      </div>
    </motion.div>
  );
}

export function TransactionSkeleton() {
  return (
    <div className="flex items-center gap-4 p-4 bg-card rounded-xl border border-border/50">
      <div className="w-11 h-11 rounded-xl animate-shimmer" />
      <div className="flex-1 space-y-2"><div className="h-4 w-3/4 rounded animate-shimmer" /><div className="h-3 w-1/2 rounded animate-shimmer" /></div>
      <div className="text-right space-y-2"><div className="h-4 w-16 rounded animate-shimmer ml-auto" /><div className="h-3 w-12 rounded animate-shimmer ml-auto" /></div>
    </div>
  );
}
