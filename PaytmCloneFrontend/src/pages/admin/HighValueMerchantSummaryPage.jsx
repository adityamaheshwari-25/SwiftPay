import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardHeader, CardContent, CardTitle } from "@/components/ui/card";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useHighValueMerchantSummary,
  useHighValueMerchantSummaryDownload,
} from "@/hooks/queries/useAdminMerchantDashboardQueries";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { LABELS } from "@/config/labels.config";
import { toast } from "sonner";
import { Download } from "lucide-react";

function formatINR(n) {
  const x = Number(n || 0);
  return x.toLocaleString("en-IN", { style: "currency", currency: "INR" });
}

function getRows(page) {
  if (!page) return [];
  return page.items || page.content || page.data || page.records || [];
}

export default function HighValueMerchantsSummaryPage() {
  const labels = LABELS.pages.highValueMerchantSummaryPage;
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const initialMinAmount = Number(searchParams.get("minAmount") || 50000);
  const initialQ = searchParams.get("q") || "";

  const [minAmountInput, setMinAmountInput] = useState(initialMinAmount);
  const [appliedMinAmount, setAppliedMinAmount] = useState(initialMinAmount);

  const [qInput, setQInput] = useState(initialQ);
  const debouncedQ = useDebouncedValue(qInput, 400);

  const [limit] = useState(20);
  const [offset, setOffset] = useState(Number(searchParams.get("offset") || 0));

  useEffect(() => {
    const params = {};
    if (appliedMinAmount) params.minAmount = String(appliedMinAmount);
    if (debouncedQ) params.q = debouncedQ;
    if (offset) params.offset = String(offset);
    setSearchParams(params, { replace: true });
  }, [appliedMinAmount, debouncedQ, offset, setSearchParams]);

  const summaryQuery = useHighValueMerchantSummary({
    minAmount: appliedMinAmount,
    q: debouncedQ,
    limit,
    offset,
  });
  const downloadQuery = useHighValueMerchantSummaryDownload({
    minAmount: appliedMinAmount,
    q: debouncedQ,
  });

  const rows = getRows(summaryQuery.data);
  const hasPrev = offset > 0;
  const hasNext = rows.length === limit;

  const onApplyMinAmount = () => {
    setAppliedMinAmount(Number(minAmountInput || 0));
    setOffset(0);
  };

  const parseFilename = (contentDisposition, fallback) => {
    if (!contentDisposition) return fallback;
    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match?.[1]) return decodeURIComponent(utf8Match[1]);
    const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
    if (plainMatch?.[1]) return plainMatch[1];
    return fallback;
  };

  const handleDownload = async () => {
    try {
      const result = await downloadQuery.refetch();
      const { blob, contentDisposition } = result.data || {};
      if (!blob) throw new Error("Download payload missing");

      const filename = parseFilename(contentDisposition, "high_value_merchants.xlsx");
      const objectUrl = URL.createObjectURL(blob);
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
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">{labels.title}</h1>
          <p className="text-sm text-muted-foreground">{labels.subtitle}</p>
        </div>

        <div className="flex gap-2">
          <Input
            type="number"
            value={minAmountInput}
            onChange={(e) => setMinAmountInput(e.target.value)}
            placeholder={labels.minAmountPlaceholder}
          />
          <Button onClick={onApplyMinAmount}>{labels.apply}</Button>
          <Button
            variant="outline"
            onClick={handleDownload}
            disabled={downloadQuery.isFetching}
          >
            <Download className="h-4 w-4" />
            {downloadQuery.isFetching ? labels.downloading : labels.downloadXlsx}
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader className="flex items-center justify-between gap-4">
          <CardTitle>{labels.summary}</CardTitle>

          <div className="flex items-center gap-2">
            <Input
              placeholder={labels.searchPlaceholder}
              value={qInput}
              onChange={(e) => {
                setQInput(e.target.value);
                setOffset(0);
              }}
            />
            <div className="text-xs text-muted-foreground">
              {labels.showing} {rows.length} - {labels.offset} {offset}
            </div>
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
            <div className="rounded-md p-3 text-sm text-destructive">{labels.failedLoad}</div>
          ) : rows.length === 0 ? (
            <div className="rounded-md p-3 text-sm text-muted-foreground">{labels.noMerchants}</div>
          ) : (
            <>
              <div className="rounded-md border border-border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{labels.business}</TableHead>
                      <TableHead>{labels.code}</TableHead>
                      <TableHead>{labels.category}</TableHead>
                      <TableHead className="text-right">{labels.highTxns}</TableHead>
                      <TableHead className="text-right">{labels.totalAmount}</TableHead>
                      <TableHead className="text-right">{labels.distinctPayers}</TableHead>
                      <TableHead className="text-right">{labels.action}</TableHead>
                    </TableRow>
                  </TableHeader>

                  <TableBody>
                    {rows.map((m) => (
                      <TableRow
                        key={m.merchantId}
                        className="cursor-pointer hover:bg-muted/60"
                        onClick={() =>
                          navigate(`/admin/high-value-merchants/${m.merchantId}?minAmount=${appliedMinAmount}&q=${encodeURIComponent(debouncedQ || "")}`)
                        }
                      >
                        <TableCell className="font-medium">{m.businessName}</TableCell>
                        <TableCell className="text-muted-foreground">{m.merchantCode}</TableCell>
                        <TableCell className="text-muted-foreground">{m.category || "-"}</TableCell>
                        <TableCell className="text-right">{m.highValueTxnCount ?? 0}</TableCell>
                        <TableCell className="text-right">{formatINR(m.totalHighValueAmount)}</TableCell>
                        <TableCell className="text-right">{m.distinctPayers ?? 0}</TableCell>
                        <TableCell className="text-right">
                          <Button
                            size="sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              navigate(`/admin/high-value-merchants/${m.merchantId}?minAmount=${appliedMinAmount}&q=${encodeURIComponent(debouncedQ || "")}`);
                            }}
                          >
                            {labels.viewMore}
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              <div className="mt-3 flex items-center justify-between">
                <div className="text-xs text-muted-foreground">{labels.offsetLabel} {offset}</div>
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
