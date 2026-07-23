import { useState } from "react";
import { useParams, useSearchParams, Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardContent, CardTitle } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import {
  useHighValueMerchantTransactions,
  useHighValueMerchantTransactionsDownload,
} from "@/hooks/queries/useAdminMerchantDashboardQueries";
import { LABELS } from "@/config/labels.config";
import { toast } from "sonner";
import { Download } from "lucide-react";

function formatINR(n) {
  const x = Number(n || 0);
  return x.toLocaleString("en-IN", { style: "currency", currency: "INR" });
}

function StatusBadge({ status }) {
  const labels = LABELS.pages.highValueMerchantTransactionsPage;
  const s = (status || "").toUpperCase();
  if (s === "SUCCESS") return <Badge>{labels.statusSuccess}</Badge>;
  if (s === "FAILED") return <Badge variant="destructive">{labels.statusFailed}</Badge>;
  if (s === "PENDING") return <Badge variant="secondary">{labels.statusPending}</Badge>;
  return <Badge variant="outline">{status || "-"}</Badge>;
}

export default function HighValueMerchantTransactionsPage() {
  const labels = LABELS.pages.highValueMerchantTransactionsPage;
  const { merchantId } = useParams();
  const [searchParams] = useSearchParams();

  const minAmount = Number(searchParams.get("minAmount") || 50000);
  const [limit] = useState(50);
  const [offset, setOffset] = useState(Number(searchParams.get("offset") || 0));

  const txQuery = useHighValueMerchantTransactions(Number(merchantId), { minAmount, limit, offset });
  const downloadQuery = useHighValueMerchantTransactionsDownload(Number(merchantId), {
    minAmount,
  });
  const rows = Array.isArray(txQuery.data) ? txQuery.data : [];

  const header = rows[0] || {};
  const hasPrev = offset > 0;
  const hasNext = rows.length === limit;

  /**
   * Reads the HTTP Content-Disposition header from backend.
Tries to extract server-sent filename (supports both filename*= UTF-8 and normal filename=).
If header is missing/unparseable, returns your fallback filename.

parseFilename decides file name.
   */
  const parseFilename = (contentDisposition, fallback) => {
    if (!contentDisposition) return fallback;
    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) return decodeURIComponent(utf8Match[1]);
    const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
    if (plainMatch?.[1]) return plainMatch[1];
    return fallback;
  };

  // Creates a temporary browser URL (URL.createObjectURL(blob)), creates a hidden <a>, sets a.download, clicks it programmatically, then cleans up.
  // handleDownload fetches file bytes and starts actual .xlsx download in browser.
  const handleDownload = async () => {
    try {
      const result = await downloadQuery.refetch(); // Manually triggers the disabled React Query download 
      const { blob, contentDisposition } = result.data || {};
      if (!blob) throw new Error("Download payload missing");

      const filename = parseFilename(
        contentDisposition,
        `merchant_${merchantId}_high_value_txns.xlsx`,
      );
      const objectUrl = URL.createObjectURL(blob); // Creates a temporary browser URL (URL.createObjectURL(blob)), creates a hidden <a>, sets a.download, clicks it programmatically, then cleans up.
      const a = document.createElement("a");
      a.href = objectUrl;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(objectUrl);
      toast.success(labels.downloadStarted);
    } catch {
      toast.error(labels.downloadFailed);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">{labels.title}</h1>
          <p className="text-sm text-muted-foreground">
            {labels.subtitlePrefix} {minAmount}{labels.subtitleSuffix}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={handleDownload} disabled={downloadQuery.isFetching}>
            <Download className="h-4 w-4" />
            {downloadQuery.isFetching ? labels.downloading : labels.downloadXlsx}
          </Button>
          <Button variant="outline" asChild>
            <Link to={`/admin/high-value-merchants?minAmount=${minAmount}`}>{labels.back}</Link>
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{header.businessName || `${labels.merchantPrefix}${merchantId}`}</CardTitle>
          <div className="text-xs text-muted-foreground">
            {labels.codePrefix} {header.merchantCode || "-"} - {labels.categoryPrefix} {header.category || "-"}
          </div>
        </CardHeader>

        <CardContent>
          {txQuery.isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          ) : txQuery.isError ? (
            <div className="rounded-md p-3 text-sm text-destructive">{labels.failedLoad}</div>
          ) : rows.length === 0 ? (
            <div className="rounded-md p-3 text-sm text-muted-foreground">{labels.noTransactions}</div>
          ) : (
            <>
              <div className="rounded-md border border-border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{labels.tx}</TableHead>
                      <TableHead>{labels.reference}</TableHead>
                      <TableHead>{labels.payer}</TableHead>
                      <TableHead className="text-right">{labels.amount}</TableHead>
                      <TableHead>{labels.status}</TableHead>
                      <TableHead>{labels.mode}</TableHead>
                      <TableHead>{labels.type}</TableHead>
                      <TableHead>{labels.created}</TableHead>
                    </TableRow>
                  </TableHeader>

                  <TableBody>
                    {rows.map((t) => (
                      <TableRow key={t.transactionDbId}>
                        <TableCell className="font-mono text-xs">{t.txId}</TableCell>
                        <TableCell className="text-muted-foreground">{t.referenceId || "-"}</TableCell>
                        <TableCell>
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
                <div className="text-xs text-muted-foreground">{labels.offset} {offset}</div>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" disabled={!hasPrev} onClick={() => setOffset((p) => Math.max(0, p - limit))}>{labels.prev}</Button>
                  <Button variant="outline" size="sm" disabled={!hasNext} onClick={() => setOffset((p) => p + limit)}>{labels.next}</Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
