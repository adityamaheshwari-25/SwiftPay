import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { LABELS } from "@/config/labels.config";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

import { useHighValueMerchantSummary } from "@/hooks/queries/useAdminMerchantDashboardQueries";

const labels = LABELS.adminComponents.highValueSummaryWidget;

function formatINR(n) {
  const x = Number(n || 0);
  return x.toLocaleString("en-IN", { style: "currency", currency: "INR" });
}

function getRows(page) {
  if (!page) return [];
  return page.items || page.content || page.data || page.records || page.result || [];
}

export default function HighValueMerchantSummaryWidget({ onSelectMerchant, selectedMerchantId }) {
  const [limit] = useState(20);
  const [offset, setOffset] = useState(0);

  const summaryQuery = useHighValueMerchantSummary(limit, offset);
  const rows = getRows(summaryQuery.data);

  const hasPrev = offset > 0;
  const hasNext = rows.length === limit;

  useEffect(() => {
    if (summaryQuery.data) console.log("DATA:", summaryQuery.data);
    if (summaryQuery.error) console.log("ERROR:", summaryQuery.error);
  }, [summaryQuery.data, summaryQuery.error]);

  return (
    <Card className="bg-card">
      <CardHeader className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
        <CardTitle className="text-base">{labels.title}</CardTitle>
        <div className="text-xs text-muted-foreground">
          {labels.showingPrefix} {rows.length} • {labels.offsetPrefix} {offset}
        </div>
      </CardHeader>

      <CardContent>
        {summaryQuery.isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : summaryQuery.isError ? (
          <div className="rounded-md border border-border bg-muted p-3 text-sm text-foreground">
            {labels.loadingFailed}
          </div>
        ) : rows.length === 0 ? (
          <div className="rounded-md border border-border bg-muted p-3 text-sm text-muted-foreground">
            {labels.noMerchants}
          </div>
        ) : (
          <>
            <div className="rounded-md border border-border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{labels.colBusiness}</TableHead>
                    <TableHead>{labels.colCode}</TableHead>
                    <TableHead>{labels.colCategory}</TableHead>
                    <TableHead className="text-right">{labels.colHighTxns}</TableHead>
                    <TableHead className="text-right">{labels.colTotalAmount}</TableHead>
                    <TableHead className="text-right">{labels.colDistinctPayers}</TableHead>
                    <TableHead className="text-right">{labels.colAction}</TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {rows.map((m) => {
                    const selected = String(selectedMerchantId) === String(m.merchantId);

                    return (
                      <TableRow key={m.merchantId} className={selected ? "bg-muted/60" : ""}>
                        <TableCell className="font-medium">{m.businessName}</TableCell>
                        <TableCell className="text-muted-foreground">{m.merchantCode}</TableCell>
                        <TableCell className="text-muted-foreground">{m.category || "-"}</TableCell>
                        <TableCell className="text-right">{m.highValueTxnCount ?? 0}</TableCell>
                        <TableCell className="text-right">{formatINR(m.totalHighValueAmount)}</TableCell>
                        <TableCell className="text-right">{m.distictPayers ?? 0}</TableCell>
                        <TableCell className="text-right">
                          <Button size="sm" variant={selected ? "secondary" : "default"} onClick={() => onSelectMerchant(m.merchantId)}>
                            {selected ? labels.viewing : labels.viewMore}
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>

            <div className="mt-3 flex items-center justify-end gap-2">
              <Button variant="outline" size="sm" disabled={!hasPrev || summaryQuery.isFetching} onClick={() => setOffset((p) => Math.max(0, p - limit))}>
                {labels.prev}
              </Button>
              <Button variant="outline" size="sm" disabled={!hasNext || summaryQuery.isFetching} onClick={() => setOffset((p) => p + limit)}>
                {labels.next}
              </Button>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}
