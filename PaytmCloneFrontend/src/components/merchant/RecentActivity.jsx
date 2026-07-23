import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Loader2, ArrowRight } from "lucide-react";
import { LABELS } from "@/config/labels.config";
import { EmptyState } from "./EmptyState";
import { TransactionItem } from "./TransactionItem";
import { SettlementItem } from "./SettlementItem";

export const RecentActivity = ({ transactions, settlements, isTxLoading, isSetLoading }) => {
  const labels = LABELS.merchantComponents.recentActivity;
  const [activeTab, setActiveTab] = useState("transactions");
  const navigate = useNavigate();

  const handleNavigate = () => {
    navigate(activeTab === "transactions" ? "/merchant/transactions" : "/merchant/settlements");
  };

  return (
    <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
      <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
        <div className="flex items-center justify-between px-6 pt-6 pb-4">
          <TabsList className="bg-slate-100/80 p-1">
            <TabsTrigger value="transactions" className="px-5 font-bold">
              {labels.tabPayments}
            </TabsTrigger>
            <TabsTrigger value="settlements" className="px-5 font-bold">
              {labels.tabSettlements}
            </TabsTrigger>
          </TabsList>

          <Button variant="ghost" size="sm" onClick={handleNavigate} className="font-bold text-primary">
            {labels.viewAll}
            <ArrowRight className="w-4 h-4 ml-1" />
          </Button>
        </div>

        <TabsContent value="transactions" className="outline-none">
          <div className="min-h-[280px] space-y-1 pb-2">
            {isTxLoading ? (
              <div className="flex justify-center py-20">
                <Loader2 className="animate-spin text-muted-foreground" />
              </div>
            ) : !transactions?.content?.length ? (
              <EmptyState message={labels.emptyPayments} />
            ) : (
              transactions.content.map((tx) => <TransactionItem key={tx.txId} tx={tx} />)
            )}
          </div>
        </TabsContent>

        <TabsContent value="settlements" className="outline-none">
          <div className="min-h-[280px] space-y-1 pb-2">
            {isSetLoading ? (
              <div className="flex justify-center py-20">
                <Loader2 className="animate-spin text-muted-foreground" />
              </div>
            ) : !settlements?.content?.length ? (
              <EmptyState message={labels.emptySettlements} />
            ) : (
              settlements.content.map((set) => <SettlementItem key={set.txId} set={set} />)
            )}
          </div>
        </TabsContent>
      </div>
    </Tabs>
  );
};
