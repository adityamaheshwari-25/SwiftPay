import React from "react";
import { Landmark, CheckCircle, Clock, Plus, Star } from "lucide-react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { LABELS } from "@/config/labels.config";
import clsx from "clsx";

export const BankAccountsList = ({
  accounts,
  isLoading,
  onAddClick,
  onSetPrimary,
  isSettingPrimary,
}) => {
  const labels = LABELS.commonComponents.bankAccountsList;
  const rupee = LABELS.splits.common.rupee;

  const renderSkeleton = () =>
    [1, 2].map((i) => (
      <div key={i} className="p-4 flex items-center gap-4 animate-pulse" role="listitem">
        <Skeleton className="h-10 w-10 rounded-xl bg-muted" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-4 w-36 bg-muted" />
          <Skeleton className="h-3 w-28 bg-muted" />
        </div>
      </div>
    ));

  const renderEmptyState = () => (
    <div className="p-10 text-center space-y-2">
      <p className="text-sm text-muted-foreground">{labels.noAccounts}</p>
      <Button variant="link" onClick={onAddClick} className="text-sm font-medium text-primary hover:underline">
        {labels.linkFirstAccount}
      </Button>
    </div>
  );

  const renderAccount = (acc) => (
    <div
      key={acc.bankAccountId}
      className={clsx(
        "group flex flex-col p-4 rounded-xl transition-all hover:bg-muted/30",
        acc.isPrimary && "ring-2 ring-primary/30"
      )}
      role="listitem"
      tabIndex={0}
    >
      <div className="flex items-center gap-4">
        <div
          className={clsx(
            "p-2.5 rounded-xl border transition-colors",
            acc.isPrimary ? "bg-primary/5 border-primary/20" : "bg-card border-border"
          )}
        >
          <Landmark className={`w-5 h-5 ${acc.isPrimary ? "text-primary" : "text-muted-foreground"}`} />
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <p className="font-bold text-sm truncate">{acc.bankName}</p>
            {acc.isPrimary && (
              <Badge className="bg-primary text-primary text-[9px] px-1.5 h-4 border-primary/20">
                {labels.primary}
              </Badge>
            )}
            {acc.verified ? <CheckCircle className="w-4 h-4 text-green-500" /> : <Clock className="w-4 h-4 text-amber-500" />}
          </div>
          <p className="text-[11px] text-muted-foreground font-mono tracking-wider">
            {acc.maskedAccountNumber} - {acc.ifsc}
          </p>
        </div>

        <div className="text-right">
          <p className="text-xs font-bold text-foreground">{rupee}{acc.balance.toLocaleString("en-IN")}</p>
          <p className="text-[9px] uppercase text-muted-foreground font-bold">{labels.balance}</p>
        </div>
      </div>

      {!acc.isPrimary && acc.verified && (
        <div className="mt-3 pl-14 hidden group-hover:flex animate-in fade-in slide-in-from-top-1">
          <Button
            size="sm"
            variant="solid"
            className="h-7 text-[10px] font-bold bg-primary text-primary-foreground hover:bg-primary/90 active:scale-95 flex items-center gap-1 px-2 shadow-sm rounded-md transition-all"
            onClick={() => onSetPrimary(acc.bankAccountId)}
            disabled={isSettingPrimary}
          >
            <Star className="w-3 h-3 mr-1 fill-current" />
            {labels.setDefaultForSettlements}
          </Button>
        </div>
      )}
    </div>
  );

  return (
    <Card className="border-border shadow-sm bg-card text-foreground">
      <CardHeader className="flex flex-row items-center justify-between py-4 border-b border-border">
        <CardTitle className="text-lg font-bold flex items-center gap-2">
          <Landmark className="w-5 h-5 text-primary" />
          {labels.title}
        </CardTitle>
        <Button
          size="sm"
          variant="outline"
          className="h-8 gap-1 font-bold text-primary border-primary hover:bg-primary/20 hover:text-primary-foreground hover:shadow-sm transition-all duration-150 rounded-md flex items-center"
          onClick={onAddClick}
        >
          <Plus className="w-4 h-4" /> {labels.addBank}
        </Button>
      </CardHeader>
      <CardContent className="p-0" role="list">
        {isLoading ? renderSkeleton() : accounts?.length === 0 ? renderEmptyState() : accounts.map(renderAccount)}
      </CardContent>
    </Card>
  );
};
