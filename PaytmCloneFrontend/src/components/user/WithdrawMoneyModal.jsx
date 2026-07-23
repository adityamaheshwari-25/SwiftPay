import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { LABELS } from "@/config/labels.config";
import { withdrawMoneySchema } from "@/schemas/withdrawMoneySchema";
import { BankSelector } from "../common/BankSelector";

export const WithdrawMoneyModal = ({ open, onOpenChange, accounts, onSubmit, isLoading }) => {
  const labels = LABELS.userComponents.withdrawMoneyModal;
  const rupee = LABELS.splits.common.rupee;

  const {
    control,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm({
    resolver: zodResolver(withdrawMoneySchema),
    defaultValues: {
      amount: 0,
      mpin: "",
    },
  });

  const handleFormSubmit = async (data) => {
    const success = await onSubmit(data);
    if (success) {
      reset();
      onOpenChange(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[440px] overflow-hidden border-border/70 bg-card p-0 shadow-2xl">
        <DialogHeader className="border-b border-border/70 bg-linear-to-r from-primary/12 via-accent/10 to-secondary px-6 py-5">
          <DialogTitle className="text-xl font-extrabold tracking-tight text-foreground">{labels.title}</DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">{labels.description}</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6 px-6 py-6">
          <Controller
            name="bankAccountId"
            control={control}
            render={({ field }) => (
              <BankSelector label={labels.destinationAccount} accounts={accounts} value={field.value} onChange={field.onChange} error={errors.bankAccountId} />
            )}
          />

          <div className="space-y-2 rounded-lg border border-border/70 bg-secondary/35 p-4">
            <Label className="text-xs font-bold uppercase tracking-wider text-muted-foreground">{labels.amountToWithdraw}</Label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-base font-bold text-muted-foreground">{rupee}</span>
              <Controller
                name="amount"
                control={control}
                render={({ field }) => (
                  <Input
                    type="number"
                    className="h-12 rounded-md border-border bg-background/95 pl-8 text-lg font-bold"
                    placeholder={labels.amountPlaceholder}
                    onChange={(e) => field.onChange(parseFloat(e.target.value))}
                  />
                )}
              />
            </div>
            {errors.amount && <p className="text-[10px] text-destructive font-bold">{errors.amount.message}</p>}
          </div>

          <div className="space-y-2">
            <Label className="text-xs font-bold uppercase tracking-wider text-muted-foreground">{labels.enterMpin}</Label>
            <Controller
              name="mpin"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  type="password"
                  maxLength={4}
                  placeholder={labels.mpinPlaceholder}
                  className="h-12 rounded-md border-border bg-background/95 text-center text-xl font-bold tracking-[1em]"
                />
              )}
            />
            {errors.mpin && <p className="text-[10px] text-destructive font-bold">{errors.mpin.message}</p>}
          </div>

          <Button type="submit" className="h-12 w-full rounded-md bg-linear-to-r from-primary to-accent text-primary-foreground font-bold shadow-md hover:brightness-95" disabled={isLoading}>
            {isLoading ? labels.processingWithdrawal : labels.confirmWithdraw}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
};
