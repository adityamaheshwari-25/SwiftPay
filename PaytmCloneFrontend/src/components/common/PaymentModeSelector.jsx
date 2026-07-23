import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { CreditCard, Landmark, Smartphone, Wallet as WalletIcon } from "lucide-react";
import { LABELS } from "@/config/labels.config";

const labels = LABELS.commonComponents.paymentModeSelector;

const MODES = [
  { id: "UPI", label: labels.upi, icon: Smartphone },
  { id: "NETBANKING", label: labels.netBanking, icon: Landmark },
  { id: "DEBIT_CARD", label: labels.debitCard, icon: CreditCard },
  { id: "WALLET", label: labels.walletBalance, icon: WalletIcon },
];

export const PaymentModeSelector = ({ value, onChange, disabledModes = [] }) => {
  return (
    <div className="space-y-3">
      <Label className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
        {labels.label}
      </Label>
      <RadioGroup
        value={value}
        onValueChange={onChange}
        className="grid grid-cols-2 gap-3"
      >
        {MODES.map((mode) => {
          const isDisabled = disabledModes.includes(mode.id);
          const Icon = mode.icon;

          return (
            <div key={mode.id}>
              <RadioGroupItem
                value={mode.id}
                id={mode.id}
                className="peer sr-only"
                disabled={isDisabled}
              />
              <Label
                htmlFor={mode.id}
                className={`flex flex-col items-center justify-between rounded-md border-2 border-muted bg-popover p-4 hover:bg-accent hover:text-accent-foreground peer-data-[state=checked]:border-primary [&:has([data-state=checked])]:border-primary transition-all cursor-pointer ${
                  isDisabled ? "opacity-50 cursor-not-allowed grayscale" : ""
                }`}
              >
                <Icon className="mb-2 h-5 w-5" />
                <span className="text-[10px] font-bold uppercase">{mode.label}</span>
              </Label>
            </div>
          );
        })}
      </RadioGroup>
    </div>
  );
};
