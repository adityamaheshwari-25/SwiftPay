import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Loader2, Users } from "lucide-react";
import { SPLIT_TYPES } from "./createSplitModal/constants";
import { useCreateSplitForm } from "./createSplitModal/useCreateSplitForm";
import { LABELS } from "@/config/labels.config";
import {
  AmountAndNoteSection,
  ParticipantsSection,
  ParticipantLookupSection,
  SplitTypeSection,
} from "./createSplitModal/sections";

export const CreateSplitModal = ({ open, onOpenChange, onCreate, isLoading }) => {
  const labels = LABELS.splits;

  // The hook owns all form state + business rules; this component stays UI-focused.
  const {
    mobile,
    setMobile,
    lookupLoading,
    lookupUser,
    lookupNotFound,
    members,
    amount,
    setAmount,
    note,
    setNote,
    splitType,
    setSplitType,
    customShares,
    setCustomShares,
    perHeadPreview,
    totalPaise,
    customSumPaise,
    customValidation,
    addMember,
    removeMember,
    submit,
  } = useCreateSplitForm({ onCreate, onOpenChange });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[520px] h-[85vh] max-h-[85vh] p-0 flex flex-col min-h-0 overflow-hidden border border-border/60 shadow-xl bg-background">
        <DialogHeader className="px-6 py-4 shrink-0 border-b border-border/60">
          <DialogTitle className="flex items-center justify-between">
            <span className="flex items-center gap-2">
              <Users className="h-5 w-5" />
              {labels.createModal.title}
            </span>
            <Badge variant="secondary">
              {members.length}{" "}
              {members.length === 1
                ? labels.createModal.participantsLabel
                : labels.createModal.participantsLabelPlural}
            </Badge>
          </DialogTitle>
        </DialogHeader>

        {/* Scrollable body split into focused sections for readability and reuse. */}
        <div className="px-6 pb-4 flex-1 min-h-0 overflow-y-auto space-y-5">
          <SplitTypeSection splitType={splitType} onSplitTypeChange={setSplitType} />

          <ParticipantLookupSection
            mobile={mobile}
            setMobile={setMobile}
            lookupLoading={lookupLoading}
            lookupUser={lookupUser}
            lookupNotFound={lookupNotFound}
            onAddMember={addMember}
          />

          <AmountAndNoteSection
            amount={amount}
            onAmountChange={setAmount}
            note={note}
            onNoteChange={setNote}
            membersCount={members.length}
            splitType={splitType}
            perHeadPreview={perHeadPreview}
          />

          <ParticipantsSection
            splitType={splitType}
            members={members}
            totalPaise={totalPaise}
            customSumPaise={customSumPaise}
            customValidation={customValidation}
            customShares={customShares}
            setCustomShares={setCustomShares}
            onRemoveMember={removeMember}
          />
        </div>

        {/* Footer action is disabled while submitting or when CUSTOM validation fails. */}
        <div className="px-6 py-4 shrink-0 bg-background/80 backdrop-blur-sm border-t border-border/60">
          <Button
            className="w-full h-12 font-bold"
            disabled={isLoading || (splitType === SPLIT_TYPES.CUSTOM && !customValidation.ok)}
            onClick={submit}
          >
            {isLoading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
            {labels.createModal.submit}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
