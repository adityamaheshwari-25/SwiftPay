import React, { useState } from "react";
import { Header } from "@/components/common/Header";
import { useMerchantDashboard, useSettlementHistory } from "@/hooks/queries/useMerchantQueries";
import { format } from "date-fns";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Download, Zap, Landmark, Loader2 } from "lucide-react";
import { PublicFooter } from "@/components/public/PublicFooter";
import { LABELS } from "@/config/labels.config";

export const SettlementHistoryPage = () => {
  const labels = LABELS.pages.settlementHistoryPage;
  const [page, setPage] = useState(0);
  const pageSize = 15;

  const { data: dashData } = useMerchantDashboard();
  const { data, isLoading, isPlaceholderData } = useSettlementHistory(page, pageSize);

  return (
    <div className="min-h-screen bg-slate-50/50">
      <Header user={dashData?.profile} security={dashData?.security} />

      <main className="max-w-7xl mx-auto p-4 md:p-8">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">{labels.title}</h1>
            <p className="text-sm text-slate-500 font-medium">{labels.subtitle}</p>
          </div>
          <Button variant="outline" className="font-bold border-slate-200">
            <Download className="h-4 w-4 mr-2" /> {labels.statement}
          </Button>
        </div>

        <Card className="border-none shadow-sm overflow-hidden">
          <CardHeader className="bg-white border-b border-slate-50 flex-row items-center justify-between">
            <CardTitle className="text-sm font-bold uppercase tracking-wider text-slate-400">{labels.transferLogs}</CardTitle>
            {isPlaceholderData && <Loader2 className="h-4 w-4 animate-spin text-primary" />}
          </CardHeader>
          <CardContent className="p-0">
            <Table className="table-fixed">
              <colgroup>
                <col className="w-[25%]" />
                <col className="w-[24%]" />
                <col className="w-[13%]" />
                <col className="w-[13%]" />
                <col className="w-[10%]" />
                <col className="w-[15%]" />
              </colgroup>

              <TableHeader className="bg-slate-50/50">
                <TableRow className="hover:bg-slate-50 transition-colors">
                  <TableHead className="text-xs font-bold uppercase tracking-wide text-slate-500">{labels.settledDate}</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-wide text-slate-500">{labels.destinationBank}</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-wide text-slate-500">
                    <div className="flex justify-center">{labels.type}</div>
                  </TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-wide text-slate-500">
                    <div className="flex justify-end">{labels.amount}</div>
                  </TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-wide text-slate-500">
                    <div className="flex justify-end">{labels.fee}</div>
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
                  data?.content.map((set) => (
                    <TableRow key={set.txId}>
                      <TableCell className="text-xs font-medium">
                        {format(new Date(set.settledAt), "dd MMM yyyy, hh:mm a")}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Landmark className="w-3 h-3 text-slate-400" />
                          <div>
                            <p className="text-sm font-bold text-slate-900">{set.destinationBankName}</p>
                            <p className="text-[10px] text-slate-400 font-bold uppercase">{labels.accountPrefix}{set.accountNumberTail}</p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-center">
                          {set.instant ? (
                            <Badge className="bg-amber-100 text-amber-700 border-none font-black text-[9px] gap-1">
                              <Zap className="w-2 h-2 fill-current" /> {labels.instant}
                            </Badge>
                          ) : (
                            <span className="text-[10px] font-bold text-slate-400 uppercase">{labels.standard}</span>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="font-black text-slate-900 text-base">
                        <div className="flex justify-end">
                          {"\u20B9"}{set.amount.toLocaleString("en-IN")}
                        </div>
                      </TableCell>
                      <TableCell className="font-black text-slate-900 text-base">
                        <div className="flex justify-end">
                          {"\u20B9"}{set.fee.toLocaleString("en-IN")}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-center">
                          <Badge className="bg-green-100 text-green-700 border-none font-black text-[10px]">{labels.success}</Badge>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>

            <div className="p-4 bg-white border-t flex items-center justify-between">
              <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">
                {labels.records} {page * pageSize + 1} - {page * pageSize + (data?.numberOfElements || 0)}
              </span>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>{labels.prev}</Button>
                <Button variant="outline" size="sm" disabled={data?.last} onClick={() => setPage((p) => p + 1)}>{labels.next}</Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </main>
      <PublicFooter />
    </div>
  );
};

const TableLoader = ({ rows }) => (
  Array.from({ length: rows }).map((_, i) => (
    <TableRow key={i}>
      <TableCell><Skeleton className="h-4 w-24" /></TableCell>
      <TableCell><Skeleton className="h-8 w-32" /></TableCell>
      <TableCell><Skeleton className="h-4 w-16 mx-auto" /></TableCell>
      <TableCell><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-4 w-16 mx-auto" /></TableCell>
    </TableRow>
  ))
);
