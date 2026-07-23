import React from 'react'
import { useInfiniteTransactions } from "@/hooks/queries/useTransactionQueries";
import { Card, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ChevronLeft, Loader2, Filter, ArrowUpRight, ArrowDownLeft } from "lucide-react";
import { TransactionItem } from "@/components/user/TransactionItem";
import { Header } from '@/components/common/Header';
import { PublicFooter } from '@/components/public/PublicFooter';
import { LABELS } from '@/config/labels.config';

export default function TransactionsPage() {
  const labels = LABELS.pages.transactionsPage;
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading, isError } = useInfiniteTransactions();

  // Flatten the pages into a single array
  const allTransactions = data?.pages.flatMap((page) => page.content) || [];

  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* Existing Header */}
      {/* <Header /> */}

      <main className="flex-1 w-full max-w-4xl mx-auto px-4 md:px-8 py-8 md:py-12">
        {/* Navigation & Header Section */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-10">
          <div className="space-y-2">
            <Button 
              onClick={() => window.history.back()}
              className="flex items-center gap-1 text-sm font-medium text-muted-foreground hover:text-primary-foreground transition-colors group"
            >
              <ChevronLeft className="w-4 h-4 group-hover:-translate-x-0.5 transition-transform" />
              {labels.backToDashboard}
            </Button>
            <h1 className="text-3xl font-black tracking-tight text-foreground">
              {labels.title}
            </h1>
            <p className="text-muted-foreground">
              {labels.subtitle}
            </p>
          </div>

          {/* <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" className="h-9 border-border bg-card shadow-sm">
              <Filter className="mr-2 h-4 w-4" /> Filter
            </Button>
            <Button size="sm" className="h-9 font-bold bg-primary text-primary-foreground hover:bg-accent shadow-lg shadow-primary/20">
              Download PDF
            </Button>
          </div> */}
        </div>

        {/* Loading State */}
        {isLoading ? (
          <div className="h-[400px] flex flex-col items-center justify-center gap-4 border-2 border-dashed border-border rounded-3xl">
            <Loader2 className="animate-spin text-primary w-8 h-8" />
            <p className="text-muted-foreground text-sm font-medium">{labels.fetchingHistory}</p>
          </div>
        ) : isError ? (
          <div className="p-12 text-center rounded-3xl border border-destructive/20 bg-destructive/5 space-y-4">
            <p className="text-destructive font-medium text-lg">{labels.loadError}</p>
            <Button variant="secondary" onClick={() => window.location.reload()}>{labels.tryAgain}</Button>
          </div>
        ) : (
          <div className="space-y-6">
            
            {/* Main Transaction Card */}
            <Card className="border border-border shadow-xl shadow-slate-200/50 bg-card rounded-2xl overflow-hidden">
              <CardHeader className="border-b border-border bg-muted/20 px-6 py-4">
                <CardTitle className="text-xs font-extrabold uppercase tracking-widest text-muted-foreground flex items-center justify-between">
                  <span>{labels.recentActivity}</span>
                  <span className="text-[10px] bg-secondary text-secondary-foreground px-2 py-0.5 rounded-full lowercase font-normal">
                    {allTransactions.length} {labels.itemsFoundSuffix}
                  </span>
                </CardTitle>
              </CardHeader>

              <div className="divide-y divide-border/60">
                {allTransactions.length > 0 ? (
                  allTransactions.map((tx) => (
                    <div key={tx.txId} className="group hover:bg-muted/30 transition-all cursor-pointer">
                      <TransactionItem tx={tx} />
                    </div>
                  ))
                ) : (
                  <div className="py-20 text-center space-y-2">
                    <p className="text-muted-foreground font-medium italic">{labels.noTransactions}</p>
                  </div>
                )}

                {/* Infinite Load More */}
                {hasNextPage && (
                  <div className="p-8 flex justify-center bg-secondary/10">
                    <Button 
                      variant="ghost" 
                      onClick={() => fetchNextPage()} 
                      disabled={isFetchingNextPage}
                      className="text-primary hover:text-accent font-bold h-11 px-8 rounded-full border border-primary/10 hover:border-primary/30"
                    >
                      {isFetchingNextPage ? (
                        <Loader2 className="animate-spin h-5 w-5" />
                      ) : (
                        labels.viewMore
                      )}
                    </Button>
                  </div>
                )}

                {!hasNextPage && allTransactions.length > 0 && (
                  <div className="py-10 text-center opacity-40 grayscale group">
                     <p className="text-[10px] font-black uppercase tracking-[0.3em] text-muted-foreground">
                       {labels.endOfHistory}
                     </p>
                  </div>
                )}
              </div>
            </Card>
          </div>
        )}
      </main>

      <PublicFooter />
    </div>
  );
}
