import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Lock } from "lucide-react";
import { motion } from "framer-motion";
import { LABELS } from "@/config/labels.config";
import { cn } from "@/lib/utils";

export const SettlementControl = ({ pendingAmount, kycStatus, onSettle, isLoading }) => {
  const labels = LABELS.merchantComponents.settlementControl;
  const rupee = LABELS.splits.common.rupee;
  const isKycApproved = kycStatus === "APPROVED";
  const MotionDiv = motion.div;

  return (
    <MotionDiv initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} whileHover={{ y: -3 }} transition={{ duration: 0.35 }}>
      <Card className={cn("relative overflow-hidden bg-card shadow-sm", "hover:shadow-xl transition-shadow", "border-transparent")}>
        <div className="pointer-events-none absolute inset-0 rounded-lg ring-1 ring-black/5" />

        <div className={cn("absolute inset-0", isKycApproved ? "bg-gradient-to-br from-primary/[0.08] via-transparent to-transparent" : "bg-gradient-to-br from-amber-400/[0.12] via-transparent to-transparent")} />

        <CardContent className="relative p-6 space-y-4">
          <p className="text-xs font-bold uppercase tracking-wide text-muted-foreground">{labels.title}</p>

          <div>
            <h2 className={cn("text-3xl font-semibold tracking-tight", isKycApproved ? "text-primary" : "text-muted-foreground")}>
              {rupee}{pendingAmount?.toLocaleString("en-IN") || "0"}
            </h2>
            <p className="text-xs text-muted-foreground mt-1">{labels.availableForTransfer}</p>
          </div>

          {!isKycApproved && (
            <div className="flex gap-2 rounded-lg bg-amber-400/10 p-3">
              <Lock className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
              <p className="text-[11px] font-semibold text-amber-700 leading-snug">{labels.frozenMessage}</p>
            </div>
          )}

          <Button className="w-full font-semibold" disabled={!isKycApproved || pendingAmount < 100 || isLoading} onClick={onSettle}>
            {isLoading ? labels.processing : isKycApproved ? labels.settleToBank : labels.kycRequired}
          </Button>
        </CardContent>
      </Card>
    </MotionDiv>
  );
};
