import { Card, CardContent } from "@/components/ui/card";
import { TrendingUp, TrendingDown, Minus, Sparkles, Wallet } from "lucide-react";
import { LABELS } from "@/config/labels.config";

export const SpendingInsightCard = ({ insight, isLoading }) => {
  const labels = LABELS.userComponents.spendingInsightCard;
  const rupee = LABELS.splits.common.rupee;

  if (isLoading) return <div className="h-32 bg-slate-100 animate-pulse rounded-xl" />;

  const { currentMonthSpent, percentageChange, isIncrease } = insight || { currentMonthSpent: 0, percentageChange: 0 };
  const isEmpty = currentMonthSpent === 0;

  return (
    <Card className="relative overflow-hidden border-border shadow-sm bg-transparent">
      <div className="absolute inset-0 bg-gradient-to-br from-primary/10 via-accent/5 to-transparent pointer-events-none" />
      <div className="absolute -top-12 -right-12 h-40 w-40 rounded-full bg-primary/10 blur-3xl pointer-events-none" />
      <CardContent className="p-6">
        <div className="flex items-center gap-2 mb-3">
          <div className="h-8 w-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <Wallet size={16} className="text-primary" />
          </div>
          <p className="text-[11px] font-bold uppercase tracking-wide text-muted-foreground">{labels.monthlySpending}</p>
        </div>
        <h3 className="text-2xl font-extrabold">{rupee}{currentMonthSpent?.toLocaleString("en-IN") || labels.defaultAmount}</h3>

        {isEmpty ? (
          <div className="mt-4 space-y-2">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <Sparkles size={14} className="text-accent" />
              {labels.noSpending}
            </div>

            <p className="text-[12px] text-muted-foreground">{labels.startUsingWallet}</p>
          </div>
        ) : (
          <div className="flex items-center mt-3">
            <div
              className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold ${
                percentageChange === 0
                  ? "bg-muted text-muted-foreground"
                  : isIncrease
                  ? "bg-destructive/10 text-destructive"
                  : "bg-green-100 text-green-700"
              }`}
            >
              {percentageChange === 0 ? <Minus size={12} /> : isIncrease ? <TrendingDown size={12} /> : <TrendingUp size={12} />}
              {percentageChange.toFixed(1)}%
            </div>
            <span className="text-[10px] text-muted-foreground ml-2 font-medium uppercase">{labels.vsLastMonth}</span>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
