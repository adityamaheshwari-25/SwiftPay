import { useMemo, useState } from "react";
import { Card, CardTitle } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { LABELS } from "@/config/labels.config";
import { Loader2, ReceiptIndianRupee } from "lucide-react";
import { PaySplitModal } from "./PaySplitModal";

export const SplitsWidget = ({
  created,
  involved,
  isLoadingCreated,
  isLoadingInvolved,
  onPay,
  payLoading,
  onCreateClick,
}) => {
  const labels = LABELS.splits.widget;
  const common = LABELS.splits.common;
  const [payingSplit, setPayingSplit] = useState(null);

  const totalOwe = useMemo(() => {
    if (!involved?.length) return 0;
    return involved.reduce((sum, s) => sum + Number(s.myShare || 0), 0);
  }, [involved]);

  const visibleCreatedSplits = useMemo(() => {
    if (!created?.length) return [];

    return created.filter((s) => {
      const paid = Number(s.paidParticipants || 0);
      const total = Number(s.totalParticipants || 0);
      const isFullyPaid = total > 0 && paid >= total;

      const status = String(s.status || "").toUpperCase();
      const isCompletedStatus = labels.completedStatuses.includes(status);

      return !(isFullyPaid || isCompletedStatus);
    });
  }, [created, labels.completedStatuses]);

  return (
    <>
      <Card className="border border-border/60 bg-card shadow-sm gap-0 py-0 overflow-hidden">
        <div className="border-b border-border/50 px-4 py-2.5 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="h-9 w-9 rounded-lg bg-primary/10 ring-1 ring-primary/15 flex items-center justify-center">
              <ReceiptIndianRupee className="h-5 w-5 text-primary" />
            </div>
            <div className="min-w-0">
              <CardTitle className="text-base font-bold">{labels.title}</CardTitle>
              <div className="text-xs text-muted-foreground">
                {labels.youOwe} <span className="font-bold text-foreground">{common.rupee}{totalOwe.toFixed(2)}</span>
              </div>
            </div>
            <Button size="sm" className="font-bold shadow-sm shrink-0 ml-2" onClick={onCreateClick}>
              {labels.createSplit}
            </Button>
          </div>
        </div>

        <div className="p-2 pt-1.5">
          <Tabs defaultValue="owe" className="gap-0">
            <TabsList className="w-full bg-muted/60 border border-border/60">
              <TabsTrigger className="flex-1" value="owe">{labels.tabOwe}</TabsTrigger>
              <TabsTrigger className="flex-1" value="created">{labels.tabCreated}</TabsTrigger>
            </TabsList>

            <TabsContent value="owe" className="mt-1.5">
              {isLoadingInvolved ? (
                <div className="text-sm text-muted-foreground flex items-center gap-2">
                  <Loader2 className="h-4 w-4 animate-spin" /> {labels.loading}
                </div>
              ) : !involved?.length ? (
                <div className="text-sm text-muted-foreground">{labels.noPendingSplits}</div>
              ) : (
                <div className="max-h-[260px] overflow-y-auto space-y-1.5 pr-1">
                  {involved.map((s) => (
                    <div
                      key={s.splitId}
                      className="rounded-xl p-3 bg-muted/20 border border-border/60 flex items-center justify-between"
                    >
                      <div className="flex flex-col min-w-0">
                        <div className="font-bold text-sm truncate">{common.rupee}{Number(s.myShare).toFixed(2)} to {s.initiatorName}</div>
                        <div className="text-xs text-muted-foreground truncate">
                          {s.note || labels.noNote} • {s.status}
                        </div>
                      </div>
                      <Button
                        size="sm"
                        className="font-bold shrink-0"
                        onClick={() => setPayingSplit(s)}
                        disabled={payLoading}
                      >
                        {labels.pay}
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </TabsContent>

            <TabsContent value="created" className="mt-1.5">
              {isLoadingCreated ? (
                <div className="text-sm text-muted-foreground flex items-center gap-2">
                  <Loader2 className="h-4 w-4 animate-spin" /> {labels.loading}
                </div>
              ) : !visibleCreatedSplits.length ? (
                <div className="text-sm text-muted-foreground">{labels.noActiveCreatedSplits}</div>
              ) : (
                <div className="max-h-[260px] overflow-y-auto space-y-1.5 pr-1">
                  {visibleCreatedSplits.map((s) => (
                    <div
                      key={s.splitId}
                      className="rounded-xl p-3 bg-muted/20 border border-border/60 flex items-center justify-between"
                    >
                      <div className="flex flex-col min-w-0">
                        <div className="font-bold text-sm">{common.rupee}{Number(s.totalAmount).toFixed(2)}</div>
                        <div className="text-xs text-muted-foreground truncate">
                          {s.note || labels.noNote}
                        </div>
                        <div className="mt-1 flex gap-2">
                          <Badge variant="outline" className="text-[10px] uppercase font-bold border-border/70">
                            {s.status}
                          </Badge>
                          <Badge className="text-[10px] uppercase font-bold">
                            {s.paidParticipants}/{s.totalParticipants} {labels.paidSuffix}
                          </Badge>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </TabsContent>
          </Tabs>
        </div>
      </Card>

      <PaySplitModal
        open={!!payingSplit}
        onOpenChange={(o) => !o && setPayingSplit(null)}
        split={
          payingSplit
            ? {
                splitId: payingSplit.splitId,
                myShare: payingSplit.myShare,
                note: payingSplit.note,
              }
            : null
        }
        onPay={onPay}
        isLoading={payLoading}
      />
    </>
  );
};
