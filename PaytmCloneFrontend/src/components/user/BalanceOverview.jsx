import { Card, CardContent } from "@/components/ui/card";
import { Wallet, Plus, Send, ArrowDownToLine, Users } from "lucide-react";
import { Button } from "../ui/button";
import { LABELS } from "@/config/labels.config";

export const BalanceOverview = ({ wallet, onAction }) => {
  const labels = LABELS.userComponents.balanceOverview;
  const rupee = LABELS.splits.common.rupee;

  return (
    <div>
      <Card className="bg-primary text-primary-foreground border-none shadow-xl overflow-hidden relative min-h-[220px] flex flex-col justify-between">
        <div className="absolute top-0 right-0 p-4 opacity-10"><Wallet size={100} /></div>
        <CardContent className="p-6 relative h-full flex flex-col">
          <div className="mb-4">
            <p className="text-primary-foreground/70 text-sm font-medium">{labels.walletBalance}</p>
            <h2 className="text-4xl font-black">{rupee}{wallet?.balance?.toLocaleString("en-IN") || labels.defaultBalance}</h2>
          </div>

          <div className="mt-auto flex gap-2 pt-4">
            <Button variant="secondary" size="sm" className="flex-1 gap-2 font-bold" onClick={() => onAction("ADD")}>
              <Plus size={16} /> {labels.add}
            </Button>
            <Button variant="secondary" size="sm" className="flex-1 gap-2 font-bold" onClick={() => onAction("SEND")}>
              <Send size={16} /> {labels.send}
            </Button>
            <Button variant="secondary" size="sm" className="flex-1 gap-2 font-bold" onClick={() => onAction("WITHDRAW")}>
              <ArrowDownToLine size={16} /> {labels.withdraw}
            </Button>
            <Button variant="secondary" size="sm" className="flex-1 gap-2 font-bold" onClick={() => onAction("SPLIT_CREATE")}>
              <Users size={16} /> {labels.split}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
