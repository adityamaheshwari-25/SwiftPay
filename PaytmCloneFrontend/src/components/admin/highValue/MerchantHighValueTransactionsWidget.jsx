import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { LABELS } from "@/config/labels.config";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

import { useHighValueMerchantTransactions } from "@/hooks/queries/useAdminMerchantDashboardQueries";

const labels = LABELS.adminComponents.highValueTransactionsWidget;

function formatINR(n) {
  const x = Number(n || 0);
  return x.toLocaleString("en-IN", { style: "currency", currency: "INR" });
}

function StatusBadge({ status }) {
  const s = (status || "").toUpperCase();
  if (s === "SUCCESS") return <Badge>{labels.statusSuccess}</Badge>;
  if (s === "FAILED") return <Badge variant="destructive">{labels.statusFailed}</Badge>;
  if (s === "PENDING") return <Badge variant="secondary">{labels.statusPending}</Badge>;
  return <Badge variant="outline">{status || "-"}</Badge>;
}

export default function MerchantHighValueTransactionsWidget({ merchantId, onClear }) {
  const [limit] = useState(50);
  const [offset, setOffset] = useState(0);

  const txQuery = useHighValueMerchantTransactions(merchantId, limit, offset);
  const rows = Array.isArray(txQuery.data) ? txQuery.data : [];

  const hasPrev = offset > 0;
  const hasNext = rows.length === limit;

  return (
    <Card className="bg-card">
      <CardHeader className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <CardTitle className="text-base">
          {labels.title} <span className="text-xs text-muted-foreground">• {labels.merchantPrefix}{merchantId}</span>
        </CardTitle>

        <Button variant="outline" size="sm" onClick={() => { setOffset(0); onClear(); }}>
          {labels.close}
        </Button>
      </CardHeader>

      <CardContent>
        {txQuery.isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : txQuery.isError ? (
          <div className="rounded-md border border-border bg-muted p-3 text-sm text-foreground">
            {labels.loadingFailed}
          </div>
        ) : rows.length === 0 ? (
          <div className="rounded-md border border-border bg-muted p-3 text-sm text-muted-foreground">
            {labels.noTransactions}
          </div>
        ) : (
          <>
            <div className="rounded-md border border-border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{labels.colTx}</TableHead>
                    <TableHead>{labels.colPayer}</TableHead>
                    <TableHead className="text-right">{labels.colAmount}</TableHead>
                    <TableHead>{labels.colStatus}</TableHead>
                    <TableHead>{labels.colMode}</TableHead>
                    <TableHead>{labels.colType}</TableHead>
                    <TableHead>{labels.colCreated}</TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {rows.map((t) => (
                    <TableRow key={t.transactionDbId}>
                      <TableCell className="space-y-1">
                        <div className="font-mono text-xs">{t.txId}</div>
                        <div className="text-[11px] text-muted-foreground">{t.referenceId || "-"}</div>
                      </TableCell>

                      <TableCell className="space-y-1">
                        <div className="font-medium">{t.payerName || "-"}</div>
                        <div className="text-[11px] text-muted-foreground">{t.payerEmail || "-"}</div>
                        <div className="text-[11px] text-muted-foreground">{t.payerMobile || "-"}</div>
                      </TableCell>

                      <TableCell className="text-right">{formatINR(t.amount)}</TableCell>
                      <TableCell><StatusBadge status={t.status} /></TableCell>
                      <TableCell className="text-muted-foreground">{t.paymentMode || "-"}</TableCell>
                      <TableCell className="text-muted-foreground">{t.transactionType || "-"}</TableCell>
                      <TableCell className="text-muted-foreground">{t.createdAt ? new Date(t.createdAt).toLocaleString("en-IN") : "-"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            <div className="mt-3 flex items-center justify-between">
              <div className="text-xs text-muted-foreground">
                {labels.showingPrefix} {rows.length} • {labels.offsetPrefix} {offset}
              </div>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" disabled={!hasPrev || txQuery.isFetching} onClick={() => setOffset((p) => Math.max(0, p - limit))}>
                  {labels.prev}
                </Button>
                <Button variant="outline" size="sm" disabled={!hasNext || txQuery.isFetching} onClick={() => setOffset((p) => p + limit)}>
                  {labels.next}
                </Button>
              </div>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}
