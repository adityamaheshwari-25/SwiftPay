import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { LABELS } from "@/config/labels.config";

export const BankSelector = ({ accounts, value, onChange, label, error }) => {
  const labels = LABELS.commonComponents.bankSelector;
  const resolvedLabel = label || labels.defaultLabel;

  return (
    <div className="space-y-2">
      <Label className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
        {resolvedLabel}
      </Label>
      <Select value={value?.toString()} onValueChange={(val) => onChange(parseInt(val))}>
        <SelectTrigger className={`h-12 ${error ? "border-destructive" : ""}`}>
          <SelectValue placeholder={labels.placeholder} />
        </SelectTrigger>
        <SelectContent className="z-[60] bg-background opacity-100 shadow-lg">
          {accounts.map((acc) => (
            <SelectItem key={acc.bankAccountId} value={acc.bankAccountId.toString()}>
              <div className="flex flex-col items-start">
                <span className="font-semibold">{acc.bankName}</span>
                <span className="text-[10px] opacity-70">{acc.maskedAccountNumber}</span>
              </div>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {error && <p className="text-[10px] text-destructive font-bold">{error.message}</p>}
    </div>
  );
};
