import { useState } from "react";
import { useApi } from "@/hooks/useApi";
import { kycService } from "@/services/api/kycService";
import { LABELS } from "@/config/labels.config";
import AppButton from "@/components/ui/AppButton";

export default function UploadKyc() {
  const labels = LABELS.kycComponents.uploadKyc;
  const [file, setFile] = useState(null);
  const { callApi, isLoading } = useApi(kycService.uploadKyc, {
    onSuccess: () => alert(labels.uploaded),
  });

  return (
    <div className="p-4 border rounded-xl bg-card">
      <input type="file" onChange={(e) => setFile(e.target.files[0])} />
      <AppButton loading={isLoading} onClick={() => callApi(file)}>
        {labels.uploadButton}
      </AppButton>
    </div>
  );
}
