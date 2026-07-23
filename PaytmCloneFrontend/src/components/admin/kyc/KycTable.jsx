import React from "react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Eye, CheckCircle, XCircle, Loader2 } from "lucide-react";
import { LABELS } from "@/config/labels.config";

const labels = LABELS.adminComponents.kycTable;

export const KycTable = ({ data, isLoading, onPreview, onApprove, onReject, isApprovePending }) => {
  if (isLoading) return <TableLoader rows={6} />;

  return (
    <div className="bg-card border border-border rounded-2xl overflow-hidden">
      <Table>
        <TableHeader className="bg-muted/30">
          <TableRow className="hover:bg-transparent border-border">
            <TableHead className="w-[350px] font-black text-muted-foreground uppercase text-[10px] tracking-[0.15em] px-8 py-5">
              {labels.colIdentityInformation}
            </TableHead>
            <TableHead className="font-black text-muted-foreground uppercase text-[10px] tracking-[0.15em] px-6">
              {labels.colVerificationAssets}
            </TableHead>
            <TableHead className="text-right font-black text-muted-foreground uppercase text-[10px] tracking-[0.15em] px-8">
              {labels.colDecisionConsole}
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((kyc) => (
            <TableRow key={kyc.userId} className="group border-border hover:bg-muted/50 transition-colors duration-200">
              <TableCell className="px-8 py-5">
                <div className="flex items-center gap-4">
                  <Avatar className="h-10 w-10 border-2 border-background shadow-sm">
                    <AvatarFallback className="bg-primary/10 text-primary font-bold text-xs">
                      {kyc.name.split(" ").map((n) => n[0]).join("")}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex flex-col">
                    <span className="font-bold text-foreground text-sm leading-tight">{kyc.name}</span>
                    <span className="text-[10px] font-black text-muted-foreground uppercase tracking-widest mt-0.5">
                      {labels.uidPrefix} {kyc.userId} • <span className="text-primary/80">{kyc.role}</span>
                    </span>
                  </div>
                </div>
              </TableCell>

              <TableCell className="px-6">
                <Button
                  variant="outline"
                  size="sm"
                  className="h-9 px-4 rounded-xl border-border bg-background hover:bg-primary hover:text-primary-foreground font-bold text-[10px] uppercase gap-2 transition-all duration-300 group/btn"
                  onClick={() => onPreview(kyc)}
                >
                  <Eye className="w-3.5 h-3.5 group-hover/btn:scale-110 transition-transform" />
                  {labels.auditDocument}
                </Button>
              </TableCell>

              <TableCell className="px-8 text-right">
                <div className="flex justify-end gap-3">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-9 rounded-xl text-destructive hover:bg-destructive/10 font-black text-[10px] uppercase px-4"
                    onClick={() => onReject(kyc.userId)}
                  >
                    <XCircle className="w-4 h-4 mr-1.5" />
                    {labels.reject}
                  </Button>

                  <Button
                    size="sm"
                    className="h-9 rounded-xl bg-primary text-primary-foreground hover:opacity-90 shadow-lg shadow-primary/20 font-black text-[10px] uppercase px-5 min-w-[110px]"
                    onClick={() => onApprove(kyc.userId)}
                    disabled={isApprovePending}
                  >
                    {isApprovePending ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        <CheckCircle className="w-4 h-4 mr-1.5" />
                        {labels.verifyUser}
                      </>
                    )}
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};

const TableLoader = ({ rows }) => (
  <div className="bg-card border border-border rounded-2xl overflow-hidden shadow-sm">
    <Table>
      <TableBody>
        {Array.from({ length: rows }).map((_, i) => (
          <TableRow key={i} className="border-border">
            <TableCell className="px-8 py-6">
              <div className="flex items-center gap-4">
                <Skeleton className="h-10 w-10 rounded-full" />
                <div className="space-y-2">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-3 w-20" />
                </div>
              </div>
            </TableCell>
            <TableCell className="px-6">
              <Skeleton className="h-9 w-32 rounded-xl" />
            </TableCell>
            <TableCell className="px-8">
              <div className="flex justify-end gap-3">
                <Skeleton className="h-9 w-24 rounded-xl" />
                <Skeleton className="h-9 w-32 rounded-xl" />
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  </div>
);
