import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Landmark, Hash, Banknote, Loader2, ShieldCheck } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { createBankAccountSchema } from "@/schemas/bankAccountSchema";
import { InputField } from "../common/InputField";
import { Button } from "@/components/ui/button";
import { LABELS } from "@/config/labels.config";

export const AddBankAccountModal = ({ open, onOpenChange, onSubmit, isLoading }) => {
  const labels = LABELS.commonComponents.addBankAccountModal;
  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isValid },
    reset,
  } = useForm({
    resolver: zodResolver(createBankAccountSchema),
    mode: "onChange",
    reValidateMode: "onChange",
    defaultValues: {
      bankName: "",
      accountNumber: "",
      ifsc: "",
    },
  });
  const accountNumberValue = useWatch({ control, name: "accountNumber", defaultValue: "" });
  const ifscValue = useWatch({ control, name: "ifsc", defaultValue: "" });
  const accountDigits = (accountNumberValue || "").replace(/\D/g, "");
  const accountNumberLooksValid = /^\d{9,18}$/.test(accountDigits);
  const ifscLooksValid = /^[A-Za-z]{4}0[A-Za-z0-9]{6}$/.test(ifscValue || "");

  const handleFormSubmit = async (data) => {
    // The Dashboard's createBankApi.callApi is passed here
    const success = await onSubmit(data);
    if (success) {
      reset();
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[440px] p-0 overflow-hidden border-none shadow-2xl">
        <div className="h-1.5 w-full bg-primary" />

        <div className="p-6">
          <DialogHeader className="space-y-3">
            <div className="mx-auto bg-primary/10 w-12 h-12 rounded-full flex items-center justify-center mb-2">
              <Landmark className="w-6 h-6 text-primary" />
            </div>
            <DialogTitle className="text-2xl font-bold text-center tracking-tight text-foreground">
              {labels.title}
            </DialogTitle>
            <DialogDescription className="text-center text-muted-foreground px-4">
              {labels.description}
            </DialogDescription>
          </DialogHeader>

        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4 pt-4">
          <div className="space-y-4">
            <div className="bg-secondary/30 p-4 rounded-xl space-y-4 border border-border/50">
              <InputField
                icon={Landmark}
                label={labels.bankName}
                placeholder={labels.bankNamePlaceholder}
                register={register("bankName")}
                error={errors.bankName}
                disabled={isLoading}
              />

              <InputField
                icon={Hash}
                label={labels.accountNumber}
                placeholder={labels.accountNumberPlaceholder}
                register={register("accountNumber", {
                  onChange: (e) => {
                    e.target.value = String(e.target.value || "").replace(/\D/g, "").slice(0, 18);
                  },
                })}
                error={errors.accountNumber}
                disabled={isLoading}
              />
              <p className={`text-xs ${accountNumberLooksValid ? "text-emerald-600" : "text-muted-foreground"}`}>
                Account digits: {accountDigits.length}/9-18
              </p>

              <InputField
                icon={Banknote}
                label={labels.ifscCode}
                placeholder={labels.ifscPlaceholder}
                register={register("ifsc", {
                  onChange: (e) => {
                    e.target.value = String(e.target.value || "")
                      .toUpperCase()
                      .replace(/[^A-Z0-9]/g, "")
                      .slice(0, 11);
                  },
                })}
                error={errors.ifsc}
                disabled={isLoading}
              />
              <p className={`text-xs ${ifscLooksValid ? "text-emerald-600" : "text-muted-foreground"}`}>
                IFSC format: AAAA0XXXXXX ({(ifscValue || "").length}/11)
              </p>
              </div>
            </div>
          <div className="flex flex-col gap-3 pt-2" >
            <Button type="submit" className="w-full h-12 font-bold mt-2" disabled={isLoading || !isValid}>
              {isLoading ? (
                <>
                  <Loader2 className="animate-spin mr-2 h-4 w-4" />
                  {labels.linkingAccount}
                </>
              ) : (
                labels.linkBankAccount
              )}
            </Button>
            <div className="flex items-center justify-center gap-1.5 text-[11px] font-medium text-muted-foreground uppercase tracking-wider">
                <ShieldCheck className="w-3.5 h-3.5 text-primary" />
                {labels.encryptedConnection}
              </div>
            </div>
        </form>
        </div>
      </DialogContent>
    </Dialog>
  );
};
