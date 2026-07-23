import { useEffect } from "react";
import { useApi } from "@/hooks/useApi";
import { adminKycService } from "@/services/api/adminKycService";
import { Button } from "@/components/ui/button";
import { LABELS } from "@/config/labels.config";

export default function PendingKycList() {
  const labels = LABELS.kycComponents.pendingKycList;
  const { data: pendingKycs, isLoading, callApi } = useApi(adminKycService.getPendingKycs);

  useEffect(() => {
    callApi();
  }, [callApi]);

  if (isLoading) return <div>{labels.loading}</div>;

  return (
    <div className="border bg-card p-6 rounded-xl shadow-sm space-y-4">
      {pendingKycs?.map((kyc) => (
        <div key={kyc.userId} className="flex justify-between items-center border-b pb-2 last:border-0">
          <span>{kyc.userName}</span>
          <div className="flex gap-2">
            <Button onClick={() => adminKycService.approveKyc(kyc.userId)}>{labels.approve}</Button>
            <Button variant="outline" onClick={() => adminKycService.rejectKyc(kyc.userId, labels.rejectReasonDefault)}>
              {labels.reject}
            </Button>
          </div>
        </div>
      ))}
    </div>
  );
}
