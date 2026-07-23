import React, { useState, useMemo } from 'react';
import { toast } from "sonner";
import { Search, Loader2, ShieldCheck, Inbox } from "lucide-react";

// Queries & Hooks
import { usePendingKycs, useKycActions } from "@/hooks/queries/useAdminQueries";
import { adminKycService } from "@/services/api/adminKycService";

// Modular Components
import { KycTable } from "@/components/admin/kyc/KycTable";
import { KycPreviewModal } from "@/components/admin/kyc/KycPreviewModal";
import { KycRejectModal } from "@/components/admin/kyc/KycRejectModal";

// UI Components
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { LABELS } from "@/config/labels.config";

export const AdminKycPage = () => {
  const labels = LABELS.pages.adminKycPage;
  // 1. Data Fetching
  const { data: kycs, isLoading, isError, refetch } = usePendingKycs();
  const { approve, reject } = useKycActions();

  // 2. Local State
  const [searchTerm, setSearchTerm] = useState("");
  const [preview, setPreview] = useState({ open: false, url: '', type: '' });
  const [rejectFlow, setRejectFlow] = useState({ open: false, userId: null, reason: '' });

  // 3. Optimized Search Logic
  const filteredKycs = useMemo(() => {
    if (!kycs) return [];
    return kycs.filter(kyc => 
      kyc.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      kyc.userId.toString().includes(searchTerm)
    );
  }, [kycs, searchTerm]);

  // 4. Secure Document Handling
  const handleOpenPreview = async (kyc) => {
    const loadingToast = toast.loading(labels.downloadingSecureDocument);
    try {
      const { blob, contentType } = await adminKycService.viewKycDocument(kyc.userId);
      const objectUrl = URL.createObjectURL(blob);
      
      setPreview({
        open: true,
        url: objectUrl,
        type: contentType
      });
      toast.dismiss(loadingToast);
    } catch {
      toast.error(labels.accessDenied);
      toast.dismiss(loadingToast);
    }
  };

  const handleClosePreview = () => {
    if (preview.url) URL.revokeObjectURL(preview.url);
    setPreview({ open: false, url: '', type: '' });
  };

  // 5. Action Handlers
  const handleRejectConfirm = () => {
    if (!rejectFlow.reason.trim()) {
      return toast.warning(labels.rejectReasonRequired);
    }

    reject.mutate(
      { userId: rejectFlow.userId, reason: rejectFlow.reason },
      {
        onSuccess: () => {
          setRejectFlow({ open: false, userId: null, reason: '' });
          toast.success(labels.rejectSuccess);
        },
        onError: () => toast.error(labels.rejectFailed)
      }
    );
  };

  const handleApprove = (userId) => {
    approve.mutate(userId, {
      onSuccess: () => toast.success(labels.approveSuccess),
      onError: () => toast.error(labels.approveFailed)
    });
  };

  // 6. Conditional Rendering for States
  if (isError) return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
      <p className="text-destructive font-bold">{labels.failedLoad}</p>
      <Button onClick={() => refetch()} variant="outline">{labels.tryAgain}</Button>
    </div>
  );

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* HEADER SECTION */}
      <section className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-primary/10 rounded-lg text-primary">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <h1 className="text-3xl font-black text-foreground tracking-tight">
              {labels.title}
            </h1>
            <Badge className="bg-amber-500/10 text-amber-600 border-amber-500/20 px-3 py-1 font-bold">
              {kycs?.length || 0} {labels.pendingSuffix}
            </Badge>
          </div>
          <p className="text-muted-foreground text-sm font-medium">
            {labels.subtitle}
          </p>
        </div>

        <div className="relative group w-full md:w-80">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground group-focus-within:text-primary transition-colors" />
          <Input 
            placeholder={labels.searchPlaceholder}
            className="pl-10 h-11 bg-card border-border shadow-sm focus-visible:ring-primary"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </section>

      {/* DATA TABLE SECTION */}
      <div className="bg-card rounded-2xl border border-border shadow-sm overflow-hidden transition-all">
        {filteredKycs.length > 0 ? (
          <KycTable 
            data={filteredKycs} 
            isLoading={isLoading} 
            onPreview={handleOpenPreview}
            onApprove={handleApprove}
            onReject={(id) => setRejectFlow(p => ({ ...p, open: true, userId: id }))}
            isApprovePending={approve.isPending}
          />
        ) : !isLoading ? (
          <div className="py-20 flex flex-col items-center text-center space-y-3">
            <div className="p-4 bg-muted rounded-full">
               <Inbox className="w-10 h-10 text-muted-foreground/40" />
            </div>
            <h3 className="font-bold text-lg text-foreground">{labels.noPendingTitle}</h3>
            <p className="text-muted-foreground text-sm max-w-[250px]">
              {searchTerm ? labels.noResults : labels.queueEmpty}
            </p>
          </div>
        ) : (
          <div className="p-20 flex justify-center"><Loader2 className="w-8 h-8 animate-spin text-primary" /></div>
        )}
      </div>

      {/* MODALS & OVERLAYS */}
      <KycPreviewModal 
        isOpen={preview.open} 
        onClose={handleClosePreview} 
        docUrl={preview.url}
        docType={preview.type}
      />

      <KycRejectModal 
        isOpen={rejectFlow.open}
        onClose={() => setRejectFlow(p => ({ ...p, open: false }))}
        onConfirm={handleRejectConfirm}
        isPending={reject.isPending}
        reason={rejectFlow.reason}
        setReason={(val) => setRejectFlow(p => ({ ...p, reason: val }))}
      />
      
      {/* Global Mutation Loading state */}
      {(approve.isPending || reject.isPending) && (
        <div className="fixed inset-0 bg-background/40 backdrop-blur-[2px] z-[999] flex items-center justify-center cursor-wait">
            <div className="bg-card p-6 rounded-2xl shadow-2xl border border-border flex flex-col items-center gap-4 animate-in zoom-in-95">
                <Loader2 className="w-10 h-10 animate-spin text-primary" />
                <p className="text-sm font-black text-foreground uppercase tracking-widest">{labels.processing}</p>
            </div>
        </div>
      )}
    </div>
  );
};
