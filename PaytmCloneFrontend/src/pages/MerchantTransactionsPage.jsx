import React, { useState } from "react";
import { Header } from "@/components/common/Header";
import { useMerchantDashboard, useMerchantTransactions } from "@/hooks/queries/useMerchantQueries";
import { format } from "date-fns";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ChevronLeft, ChevronRight, Download } from "lucide-react";
import { PublicFooter } from "@/components/public/PublicFooter";
import { LABELS } from "@/config/labels.config";

export const MerchantTransactionsPage = () => {
  const labels = LABELS.pages.merchantTransactionsPage;
  const [page, setPage] = useState(0);
  const pageSize = 20;

  const { data: dashData } = useMerchantDashboard();
  const { data, isLoading, isPlaceholderData } = useMerchantTransactions(page, pageSize);

  return (
    <div className="min-h-screen bg-slate-50/50">
      <Header user={dashData?.profile} security={dashData?.security} />

      <main className="max-w-7xl mx-auto p-4 md:p-8">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">{labels.title}</h1>
            <p className="text-sm text-slate-500 font-medium">{labels.subtitle}</p>
          </div>
          <Button variant="outline" className="font-bold gap-2 border-slate-200">
            <Download className="h-4 w-4" /> {labels.exportCsv}
          </Button>
        </div>

        <Card className="border-none shadow-sm overflow-hidden">
          <CardHeader className="bg-white border-b border-slate-50">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-bold uppercase tracking-wider text-slate-400">{labels.allTransactions}</CardTitle>
              {isPlaceholderData && <span className="text-[10px] font-bold text-primary animate-pulse">{labels.updating}</span>}
            </div>
          </CardHeader>
          <CardContent className="p-0">
            <Table className="table-fixed">
              <colgroup>
                <col className="w-[30%]" />
                <col className="w-[23%]" />
                <col className="w-[17%]" />
                <col className="w-[15%]" />
                <col className="w-[15%]" />
              </colgroup>

              <TableHeader className="bg-slate-50/70">
                <TableRow className="hover:bg-slate-50 transition-colors">
                  <TableHead className="text-xs font-bold uppercase tracking-wide text-slate-500">{labels.dateTime}</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-wide text-slate-500">{labels.customerId}</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-wide text-slate-500">{labels.method}</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-wide text-slate-500">
                    <div className="flex justify-end">{labels.amount}</div>
                  </TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-wide text-slate-500">
                    <div className="flex justify-center">{labels.status}</div>
                  </TableHead>
                </TableRow>
              </TableHeader>

              <TableBody>
                {isLoading ? (
                  <TableLoader rows={pageSize} />
                ) : (
                  data?.content.map((tx) => (
                    <TableRow key={tx.txId} className="hover:bg-slate-50/50">
                      <TableCell className="text-xs font-medium text-slate-600">
                        {format(new Date(tx.createdAt), "dd MMM yyyy, hh:mm a")}
                      </TableCell>
                      <TableCell>
                        <p className="text-sm font-bold text-slate-900">{tx.customerName || labels.direct}</p>
                        <p className="text-[10px] text-slate-400 font-mono uppercase">{tx.txId}</p>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline" className="text-[10px] font-bold border-slate-200 uppercase">
                          {tx.paymentMode}
                        </Badge>
                      </TableCell>
                      <TableCell className="font-black text-slate-900">
                        <div className="flex justify-end">
                          {"\u20B9"}{tx.amount.toLocaleString("en-IN")}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-center">
                          <Badge
                            className={`font-black text-[10px] border-none ${
                              tx.status === "SUCCESS" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
                            }`}
                          >
                            {tx.status}
                          </Badge>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>

            <div className="p-4 bg-white border-t flex items-center justify-between">
              <p className="text-xs font-bold text-slate-400 uppercase tracking-tighter">
                {labels.showingPage} {page + 1} {labels.of} {data?.totalPages || 1}
              </p>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="icon"
                  className="h-8 w-8"
                  disabled={page === 0 || isLoading}
                  onClick={() => setPage((p) => p - 1)}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="icon"
                  className="h-8 w-8"
                  disabled={data?.last || isLoading}
                  onClick={() => setPage((p) => p + 1)}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </main>
      <PublicFooter />
    </div>
  );
};

const TableLoader = ({ rows }) =>
  Array.from({ length: rows }).map((_, i) => (
    <TableRow key={i}>
      <TableCell><Skeleton className="h-4 w-24" /></TableCell>
      <TableCell><Skeleton className="h-8 w-32" /></TableCell>
      <TableCell><Skeleton className="h-4 w-16" /></TableCell>
      <TableCell><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-4 w-16 mx-auto" /></TableCell>
    </TableRow>
  ));
