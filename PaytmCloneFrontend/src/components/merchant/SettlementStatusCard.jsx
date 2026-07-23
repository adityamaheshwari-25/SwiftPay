import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Activity, Calendar, Info } from "lucide-react";
import { motion } from "framer-motion";
import { format, addDays, setHours, setMinutes, isAfter } from "date-fns";
import { LABELS } from "@/config/labels.config";
import { cn } from "@/lib/utils";

export const SettlementStatusCard = ({ kycStatus }) => {
  const labels = LABELS.merchantComponents.settlementStatusCard;
  const isBlocked = kycStatus !== "APPROVED";
  const MotionDiv = motion.div;

  const getNextSettlementDate = () => {
    const now = new Date();
    let next = setMinutes(setHours(now, 2), 0);
    if (isAfter(now, next)) next = addDays(next, 1);
    return format(next, "MMM dd, hh:mm a");
  };

  return (
    <MotionDiv initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} whileHover={{ y: -3 }} transition={{ duration: 0.35 }}>
      <Card className="relative overflow-hidden bg-card shadow-sm hover:shadow-xl transition-shadow border-transparent">
        <div className="pointer-events-none absolute inset-0 rounded-lg ring-1 ring-black/5" />

        <div className={cn("absolute inset-0", isBlocked ? "bg-gradient-to-br from-amber-400/[0.1] via-transparent to-transparent" : "bg-gradient-to-br from-green-400/[0.1] via-transparent to-transparent")} />

        <CardContent className="relative p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Activity className={cn("h-4 w-4", isBlocked ? "text-amber-600" : "text-green-600")} />
              <p className="text-xs font-bold uppercase tracking-wide text-muted-foreground">{labels.title}</p>
            </div>

            <Badge className={cn("border-none font-semibold", isBlocked ? "bg-amber-400/20 text-amber-700" : "bg-green-400/20 text-green-700")}>
              {isBlocked ? labels.paused : labels.active}
            </Badge>
          </div>

          <div className="flex items-start gap-3">
            <Calendar className="h-4 w-4 text-muted-foreground mt-0.5" />
            <div>
              <p className="text-[11px] uppercase font-semibold text-muted-foreground">{labels.nextScheduledPayout}</p>
              <p className="text-sm font-semibold text-foreground">{getNextSettlementDate()}</p>
            </div>
          </div>

          {isBlocked && (
            <div className="flex gap-2 rounded-lg bg-muted p-3">
              <Info className="h-4 w-4 text-muted-foreground shrink-0 mt-0.5" />
              <p className="text-[11px] font-medium text-muted-foreground leading-snug">
                {labels.blockedMessagePrefix} {kycStatus || labels.pending}{labels.blockedMessageSuffix}
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </MotionDiv>
  );
};
