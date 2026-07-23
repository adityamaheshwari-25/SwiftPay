import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { ArrowDownRight, ArrowUpRight, IndianRupee } from "lucide-react";
import { motion, useMotionValue, useTransform, animate } from "framer-motion";
import { LABELS } from "@/config/labels.config";
import { useEffect } from "react";

export const StatCard = ({ label, value = 0, growth, compareLabel }) => {
  const labels = LABELS.merchantComponents.statCard;
  const rupee = LABELS.splits.common.rupee;
  const isPositive = growth > 0;
  const isNegative = growth < 0;
  const MotionDiv = motion.div;
  const MotionSpan = motion.span;

  const motionValue = useMotionValue(0);
  const rounded = useTransform(motionValue, (v) => Math.round(v).toLocaleString("en-IN"));

  useEffect(() => {
    animate(motionValue, value, {
      duration: 0.6,
      ease: "easeOut",
    });
  }, [motionValue, value]);

  return (
    <MotionDiv initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} whileHover={{ y: -4 }} transition={{ duration: 0.35 }} className="h-full">
      <Card className={cn("relative h-full overflow-hidden", "border-transparent", "bg-card shadow-lg", "hover:shadow-xl transition-shadow")}>
        <div className="pointer-events-none absolute inset-0 rounded-lg ring-1 ring-black/5" />
        <div className="absolute inset-0 bg-gradient-to-br from-primary/[0.08] via-transparent to-transparent" />
        <div className="absolute left-0 top-0 h-full w-1 bg-primary/80" />

        <CardContent className="relative flex h-full flex-col p-6">
          <div className="flex items-center justify-between">
            <p className="text-sm font-medium text-muted-foreground">{label}</p>

            <div className="rounded-md bg-primary/10 p-2 text-primary">
              <IndianRupee className="h-4 w-4" />
            </div>
          </div>

          <div className="mt-4">
            <h2 className="text-3xl font-semibold tracking-tight text-foreground">
              {rupee}<MotionSpan>{rounded}</MotionSpan>
            </h2>
          </div>

          <div className="flex-1" />

          <div className="min-h-[28px]">
            {growth ? (
              <div className="flex items-center gap-2 text-sm">
                <span
                  className={cn(
                    "flex items-center gap-1 rounded-full px-2 py-0.5 font-semibold",
                    isPositive && "bg-accent/15 text-accent-foreground",
                    isNegative && "bg-destructive/10 text-destructive"
                  )}
                >
                  {isPositive ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />}
                  {Math.abs(growth).toFixed(1)}%
                </span>

                <span className="text-muted-foreground">{compareLabel}</span>
              </div>
            ) : (
              <span className="text-sm text-muted-foreground">{labels.noComparisonData}</span>
            )}
          </div>
        </CardContent>
      </Card>
    </MotionDiv>
  );
};
