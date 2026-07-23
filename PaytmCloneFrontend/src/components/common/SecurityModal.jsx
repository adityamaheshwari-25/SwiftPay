import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { KycUploadComponent } from "./KycUploadComponent";
import { SetMpinCard } from "./SetMpinCard";
import { Button } from "@/components/ui/button";
import { ChevronRight, Info, AlertCircle, Clock } from "lucide-react";
import { LABELS } from "@/config/labels.config";

export const SecurityModal = ({ open, onOpenChange, security, refreshStatus }) => {
  const labels = LABELS.commonComponents.securityModal;
  const [step, setStep] = useState(() => (security?.kycStatus === "APPROVED" ? "MPIN" : "KYC"));

  const isKycPending = security?.kycStatus === "PENDING";
  const isKycRejected = security?.kycStatus === "REJECTED";
  const isKycApproved = security?.kycStatus === "APPROVED";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            {labels.title}
            {isKycApproved && security?.mpinSet && (
              <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">{labels.verified}</span>
            )}
          </DialogTitle>
          <DialogDescription>
            {step === "KYC" ? labels.kycDescription : labels.mpinDescription}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          <div className="flex gap-2 mb-4">
            <div
              className={`h-1.5 flex-1 rounded-full transition-all ${isKycApproved ? "bg-green-500" : isKycPending ? "bg-amber-400" : "bg-muted"}`}
              onClick={() => setStep("KYC")}
              style={{ cursor: "pointer" }}
            />
            <div
              className={`h-1.5 flex-1 rounded-full transition-all ${security?.mpinSet ? "bg-green-500" : "bg-muted"}`}
              onClick={() => (isKycApproved || isKycPending) && setStep("MPIN")}
              style={{ cursor: isKycApproved || isKycPending ? "pointer" : "not-allowed" }}
            />
          </div>

          {step === "KYC" ? (
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <h3 className="font-bold text-lg">{labels.identityVerification}</h3>
                {(isKycPending || isKycApproved) && (
                  <Button variant="ghost" size="sm" className="text-primary font-bold" onClick={() => setStep("MPIN")}>
                    {labels.nextMpin} <ChevronRight className="ml-1 w-4 h-4" />
                  </Button>
                )}
              </div>

              {isKycPending && (
                <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl flex gap-3 items-start">
                  <Clock className="w-5 h-5 text-amber-600 mt-0.5" />
                  <div>
                    <p className="text-sm font-bold text-amber-900">{labels.kycUnderReview}</p>
                    <p className="text-xs text-amber-700">{labels.kycPendingDetail}</p>
                  </div>
                </div>
              )}

              {isKycRejected && (
                <div className="p-4 bg-destructive/10 border border-destructive/20 rounded-xl flex gap-3 items-start">
                  <AlertCircle className="w-5 h-5 text-destructive mt-0.5" />
                  <div>
                    <p className="text-sm font-bold text-destructive">{labels.verificationRejected}</p>
                    <p className="text-xs text-destructive/80">
                      {labels.rejectionReasonPrefix} {security?.rejectionReason || labels.defaultRejectionReason}
                    </p>
                  </div>
                </div>
              )}

              <KycUploadComponent
                onUploadSuccess={() => refreshStatus?.()}
                isPending={isKycPending}
                isApproved={isKycApproved}
              />
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center gap-2">
                <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => setStep("KYC")}>
                  <ChevronRight className="rotate-180 w-4 h-4" />
                </Button>
                <h3 className="font-bold text-lg">{security?.mpinSet ? labels.resetMpin : labels.setMpin}</h3>
              </div>

              {security?.mpinSet && (
                <div className="p-3 bg-blue-50 border border-blue-100 rounded-lg flex gap-2 items-center">
                  <Info className="w-4 h-4 text-blue-600" />
                  <p className="text-xs text-blue-700 font-medium">{labels.mpinAlreadySet}</p>
                </div>
              )}

              <SetMpinCard
                isReset={security?.mpinSet}
                onComplete={() => {
                  refreshStatus?.();
                  onOpenChange(false);
                }}
              />
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
};
