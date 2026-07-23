import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Toaster } from "sonner";
import { addMoneySchema } from "@/schemas/addMoneySchema";
import { Label } from "@/components/ui/label";
import { BankSelector } from "../common/BankSelector";
import { PaymentModeSelector } from "../common/PaymentModeSelector";

export const AddMoneyModal = ({ open, onOpenChange, accounts, onSubmit, isLoading }) => {
  const { control, handleSubmit, formState: { errors }, reset } = useForm({
    resolver: zodResolver(addMoneySchema),
    defaultValues: { amount: 0, paymentMode: "UPI" }
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
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader><DialogTitle>Add Money to Wallet</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6 pt-4">
          <Controller
            name="bankAccountId"
            control={control}
            render={({ field }) => (
              <BankSelector accounts={accounts} value={field.value} onChange={field.onChange} error={errors.bankAccountId} />
            )}
          />

          <Controller
            name="paymentMode"
            control={control}
            render={({ field }) => (
              <PaymentModeSelector 
                value={field.value} 
                onChange={field.onChange} 
                disabledModes={["WALLET"]} // Can't use wallet to fund wallet
              />
            )}
          />

          <div className="space-y-2">
            <Label className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Amount</Label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 font-bold text-muted-foreground">₹</span>
              <Controller
                name="amount"
                control={control}
                render={({ field }) => (
                  <Input 
                    type="number" 
                    className="pl-8 h-12 font-bold text-lg" 
                    placeholder="0.00" 
                    onChange={(e) => field.onChange(parseFloat(e.target.value))}
                  />
                )}
              />
            </div>
            {errors.amount && <p className="text-[10px] text-destructive font-bold">{errors.amount.message}</p>}
          </div>

          <Button type="submit" className="w-full h-12 font-bold" disabled={isLoading}>
            {isLoading ? "Processing..." : "Proceed to Add"}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
};