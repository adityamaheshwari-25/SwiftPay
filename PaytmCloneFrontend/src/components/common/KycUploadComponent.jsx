import { useState } from "react";
import { Upload, CheckCircle2, Loader2, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { kycService } from "@/services/api/kycService";
import { LABELS } from "@/config/labels.config";
import { toast } from "sonner";

export const KycUploadComponent = ({ onUploadSuccess, isPending, isApproved }) => {
  const labels = LABELS.commonComponents.kycUpload;
  const [file, setFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);

  const handleUpload = async () => {
    if (!file) return;
    setIsUploading(true);
    try {
      await kycService.uploadKyc(file);
      toast.success(labels.documentsSubmitted);
      onUploadSuccess();
    } catch (err) {
      toast.error(err.response?.data?.message || labels.uploadFailed);
    } finally {
      setIsUploading(false);
    }
  };

  if (isApproved) {
    return (
      <div className="py-8 flex flex-col items-center justify-center border-2 border-green-100 bg-green-50/30 rounded-2xl border-dashed">
        <CheckCircle2 className="w-12 h-12 text-green-500 mb-2" />
        <p className="font-bold text-green-800">{labels.identityVerified}</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div
        className={`border-2 border-dashed rounded-xl p-8 flex flex-col items-center justify-center gap-3 transition-colors bg-muted/30
          ${isPending ? "opacity-50 cursor-not-allowed" : "hover:border-primary/50 cursor-pointer"}`}
        onClick={() => !isPending && document.getElementById("kyc-file").click()}
      >
        <Upload className="w-10 h-10 text-muted-foreground" />
        <div className="text-center">
          <p className="font-medium text-sm">{file ? file.name : labels.selectIdentityProof}</p>
          <p className="text-[10px] text-muted-foreground uppercase font-bold tracking-wider">{labels.identityProofHint}</p>
        </div>
        <input
          type="file"
          id="kyc-file"
          hidden
          disabled={isPending}
          onChange={(e) => setFile(e.target.files[0])}
        />
      </div>

      {!isPending && (
        <Button className="w-full h-12 font-bold" onClick={handleUpload} disabled={!file || isUploading}>
          {isUploading ? <Loader2 className="animate-spin mr-2" /> : <FileText className="mr-2 w-4 h-4" />}
          {labels.uploadAndVerify}
        </Button>
      )}
    </div>
  );
};
