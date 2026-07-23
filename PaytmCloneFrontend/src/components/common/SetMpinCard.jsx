import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Lock, ShieldCheck, Loader2 } from "lucide-react";
import { setMpinSchema } from "@/schemas/mpinSchema";
import { mpinService } from "@/services/api/mpinService";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { LABELS } from "@/config/labels.config";
import { toast } from "sonner";

export const SetMpinCard = ({ onComplete, isReset }) => {
  const labels = LABELS.commonComponents.setMpinCard;

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(setMpinSchema),
  });

  const onSubmit = async (data) => {
    try {
      await mpinService.setMpin({ mpin: data.mpin });
      toast.success(isReset ? labels.resetSuccess : labels.setSuccess);
      onComplete();
    } catch {
      toast.error(labels.processFailed);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="space-y-1.5">
        <Label className="text-xs font-bold uppercase text-muted-foreground">{labels.newMpin}</Label>
        <div className="relative">
          <Lock className="absolute left-3 top-3 w-4 h-4 text-muted-foreground" />
          <Input
            {...register("mpin")}
            type="password"
            placeholder={labels.mpinPlaceholder}
            maxLength={4}
            className="pl-10 h-12 text-xl tracking-[0.5em]"
          />
        </div>
        {errors.mpin && <p className="text-[10px] font-bold text-destructive uppercase">{errors.mpin.message}</p>}
      </div>

      <div className="space-y-1.5">
        <Label className="text-xs font-bold uppercase text-muted-foreground">{labels.confirmMpin}</Label>
        <div className="relative">
          <Lock className="absolute left-3 top-3 w-4 h-4 text-muted-foreground" />
          <Input
            {...register("confirmMpin")}
            type="password"
            placeholder={labels.mpinPlaceholder}
            maxLength={4}
            className="pl-10 h-12 text-xl tracking-[0.5em]"
          />
        </div>
        {errors.confirmMpin && <p className="text-[10px] font-bold text-destructive uppercase">{errors.confirmMpin.message}</p>}
      </div>

      <Button type="submit" className="w-full h-12 font-bold mt-4" disabled={isSubmitting}>
        {isSubmitting ? <Loader2 className="animate-spin mr-2" /> : <ShieldCheck className="mr-2 w-4 h-4" />}
        {isReset ? labels.updateAndSecureWallet : labels.setMpinAndSecureWallet}
      </Button>
    </form>
  );
};
