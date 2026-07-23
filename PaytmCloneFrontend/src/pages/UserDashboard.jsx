import React, { useState, useEffect, Activity } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUserDashboard } from '@/hooks/queries/useUserQueries';
import { useWalletActions } from '@/hooks/queries/useWalletMutations';

// UI Components
import { Header } from '@/components/common/Header';
import { BalanceOverview } from '@/components/user/BalanceOverview';
import { SpendingInsightCard } from '@/components/user/SpendingInsightCard';
import { TransactionItem } from '@/components/user/TransactionItem';
import { BankAccountsList } from '@/components/common/BankAccountsList';
import { Card, CardHeader, CardTitle } from '@/components/ui/card';

// Modals
import { AddMoneyModal } from '@/components/user/AddMoneyModal';
import { SendMoneyModal } from '@/components/user/SendMoneyModal';
import { WithdrawMoneyModal } from '@/components/user/WithdrawMoneyModal';
import { SecurityModal } from '@/components/common/SecurityModal';
import { AddBankAccountModal } from '@/components/common/AddBankAccountModal';
import { ActivityIcon, ActivitySquare, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { PublicFooter } from '@/components/public/PublicFooter';
import { useAuth } from '@/context/AuthContext';
import { useSplitCreatedList, useSplitInvolvedList } from '@/hooks/queries/useSplitQueries';
import { useSplitActions } from '@/hooks/queries/useSplitActions';
import { useSplitSse } from '@/hooks/useSplitSse';
import { CreateSplitModal } from '@/components/splits/CreateSplitModal';
import { SplitsWidget } from '@/components/splits/SplitsWidget';
import { LABELS } from '@/config/labels.config';
import { APP_ROLES } from '@/config/roles.config';

export default function UserDashboard() {
  const labels = LABELS.pages.userDashboard;
  const navigate = useNavigate();
  const [activeModal, setActiveModal] = useState(null);
  const [isSecurityOpen, setIsSecurityOpen] = useState(false);

  const { hasRole } = useAuth();
  const isUser = hasRole(APP_ROLES.USER);

  // basically renaming during destructuring, so as they both don't clash with the same name, we rename the variables.
  const { data: created, isLoading: isLoadingCreated } = useSplitCreatedList();
  const { data: involved, isLoading: isLoadingInvolved } = useSplitInvolvedList();
  // console.log(created);
  // console.log(involved)

  const { createSplit, paySplit } = useSplitActions();

  useSplitSse({ enabled: isUser });


  // 1. Data Hooks (At the top!)
  const { data, isLoading, isError, refetch: refetchDashboard } = useUserDashboard();
  const { addMoney, transferMoney, withdrawMoney, setPrimaryBank, createBank } = useWalletActions();

  // 2. Auto-trigger Security
  useEffect(() => {
    if (data?.security) {
      const { mpinSet, kycStatus } = data.security;
      // eslint-disable-next-line react-hooks/set-state-in-effect
      if (!mpinSet || kycStatus !== 'APPROVED') setIsSecurityOpen(true);
    }
  }, [data?.security]);

  // 3. Loading/Error States
  if (isLoading) return <div className="p-20 text-center animate-pulse">{labels.loading}</div>;
  if (isError || !data) return <div className="p-20 text-center text-red-500">{labels.error}</div>;

  const { profile, wallet, bankAccounts, transactions, spending, security } = data;

  return (
    <div className="min-h-screen bg-slate-50/50 pb-20">
      <Header user={profile} security={security} onSecurityAction={() => setIsSecurityOpen(true)} />
      
      <main className="max-w-6xl mx-auto p-4 space-y-8">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <BalanceOverview wallet={wallet} onAction={setActiveModal} />
          <SpendingInsightCard insight={spending} />
        </div>


        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-8">
            <Card className="border-none shadow-sm hover:shadow-md transition-shadow">
              <CardHeader className="relative px-6 py-4">
              {/* Soft bottom separator */}
              <div className="absolute inset-x-6 bottom-0 h-px bg-gradient-to-r from-transparent via-border to-transparent" />

              {/* ACTUAL HEADER ROW */}
              <div className="flex items-center justify-between min-h-[48px]">
                {/* LEFT */}
                <div className="flex items-center gap-3">
                  <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10">
                    <ActivityIcon size={16} className="text-primary" />
                  </div>

                  <CardTitle className="text-base font-bold">
                    {labels.recentTransactions}
                  </CardTitle>
                </div>

                {/* RIGHT */}
                <Button
                  variant="ghost"
                  onClick={() => navigate("/user/transactions")}
                  className="h-7 text-[12px] font-bold bg-primary text-primary-foreground hover:bg-primary/90 active:scale-95 flex items-center gap-1 px-2 shadow-sm rounded-md transition-all"
                >
                  {labels.viewAll}
                  <ChevronRight size={16} />
                </Button>
              </div>
            </CardHeader>

              <div className="space-y-1 bg-white p-2">
                {transactions.content.map(tx => <TransactionItem key={tx.txId} tx={tx} />)}
              </div>
            </Card>
          </div>

          <div className="lg:col-span-4 space-y-6">
          {isUser && (
              <SplitsWidget
                created={created}
                involved={involved}
                isLoadingCreated={isLoadingCreated}
                isLoadingInvolved={isLoadingInvolved}
                onCreateClick={() => setActiveModal("SPLIT_CREATE")}
                onPay={async ({ splitId, mpin }) => {
                  try {
                    await paySplit.mutateAsync({ splitId, mpin });
                    return true;
                  } catch {
                    return false;
                  }
                }}
                payLoading={paySplit.isPending}
              />
            )}
            <BankAccountsList 
              accounts={bankAccounts} 
              onAddClick={() => setActiveModal('ADD_BANK')}
              onSetPrimary={(id) => setPrimaryBank.mutate(id)}
              isSettingPrimary={setPrimaryBank.isPending}
            />
          </div>
        </div>
        <PublicFooter/>
      </main>

      {/* Modals are controlled by mutation.mutate and mutation.isPending */}
      <AddMoneyModal 
        open={activeModal === 'ADD'} 
        onOpenChange={(open) => !open && setActiveModal(null)}
        accounts={bankAccounts}
        onSubmit={(payload) => addMoney.mutate(payload, { onSuccess: () => setActiveModal(null) })}
        isLoading={addMoney.isPending}
      />
      
      <SendMoneyModal 
        open={activeModal === 'SEND'} 
        onOpenChange={(open) => !open && setActiveModal(null)}
        onSubmit={(payload) => transferMoney.mutate(payload, { onSuccess: () => setActiveModal(null) })}
        isLoading={transferMoney.isPending}
      />

      <WithdrawMoneyModal 
        open={activeModal === 'WITHDRAW'} 
        onOpenChange={(open) => !open && setActiveModal(null)}
        accounts={bankAccounts}
        onSubmit={(payload) => withdrawMoney.mutate(payload, { onSuccess: () => setActiveModal(null) })}
        isLoading={withdrawMoney.isPending}
      />

      <AddBankAccountModal 
        open={activeModal === 'ADD_BANK'}
        onOpenChange={(open) => !open && setActiveModal(null)}
        onSubmit={(payload) => createBank.mutate(payload, { onSuccess: () => setActiveModal(null) })}
        isLoading={createBank.isPending}
      />

      <SecurityModal
        open={isSecurityOpen}
        onOpenChange={setIsSecurityOpen}
        security={security}
        refreshStatus={refetchDashboard}
      />

      <CreateSplitModal
        open={activeModal === "SPLIT_CREATE"}
        onOpenChange={(open) => !open && setActiveModal(null)}
        onCreate={async (payload) => {
          try {
            console.log(JSON.stringify(payload));
            await createSplit.mutateAsync(payload);
            return true;
          } catch {
            return false;
          }
        }}
        isLoading={createSplit.isPending}
      />

    </div>
  );
}
