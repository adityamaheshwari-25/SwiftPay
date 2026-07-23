// import { AddBankAccountModal } from '@/components/common/AddBankAccountModal';
// import { BankAccountsList } from '@/components/common/BankAccountsList';
// import { Header } from '@/components/common/Header';
// import { SecurityModal } from '@/components/common/SecurityModal';
// import { RecentActivity } from '@/components/merchant/RecentActivity';
// import { SettlementControl } from '@/components/merchant/SettlementControl';
// import { SettlementStatusCard } from '@/components/merchant/SettlementStatusCard';
// import { StatsCard } from '@/components/merchant/StatsCards';
// import { useApi } from '@/hooks/useApi';
// import { bankAccountService } from '@/services/api/bankAccountService';
// import { merchantService } from '@/services/api/merchantService';
// import React, { useEffect, useState } from 'react'
// import { toast } from 'sonner';

// export const MerchantDashboard = () => {
//   const [isSecurityOpen, setIsSecurityOpen] = useState(false);
//   const [isAddBankOpen, setIsAddBankOpen] = useState(false);

//   // merged 4 apis into 1, API composition pattern.
//   const dashboardApi = useApi(merchantService.getMerchantDashboard);

//   const settleApi = useApi(merchantService.triggerInstantSettlement, {
//     onSuccess: () => {
//       toast.success("Funds transferred successfully!");
//       dashboardApi.callApi(); // refresh balance
//     },
//     onError: (err) => toast.error(err.response?.data || "Settlement failed")
//   });

//   const addBankApi = useApi(merchantService.addBankAccount, {
//     onSuccess: () => {
//       toast.success("Bank account linked!")
//       setIsAddBankOpen(false);
//       dashboardApi.callApi();
//     }
//   });

//   const primaryBankApi = useApi(bankAccountService.setPrimaryAccount, {
//     onSuccess: () => {
//       toast.success("Primary account updated");
//       dashboardApi.callApi();
//     }, 
//     onError: (err) => {
//       toast.error("Failed to update primary bank.")
//     }
//   });

//   const refreshAll = () => dashboardApi.callApi();

//   useEffect(() => {
//     dashboardApi.callApi();
//   }, []);

//   const { profile, stats, security, bankAccounts } = dashboardApi.data || {};

//   return (
//     <div className="min-h-screen bg-slate-50/50">
//       <Header
//         user={profile}
//         security={security}
//         onSecurityAction={() => setIsSecurityOpen(true)}
//         refreshSecurity={refreshAll}
//         // onLogout{() => }
//       />
//       <main className="container mx-auto p-4 md:p-8 max-w-7xl space-y-6">
//         {/* ROW 1: Metrics & Settlement Action */}
//         <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
//           <div className="lg:col-span-2 space-y-6">
//             <StatsCard stats={stats} isLoading={dashboardApi.isLoading} />
//             <RecentActivity /> 
//           </div>
          
//           <div className="lg:col-span-1 space-y-6">
//             <SettlementControl 
//               pendingAmount={stats?.pending} 
//               kycStatus={security?.kycStatus}
//               isLoading={settleApi.isLoading}
//               onSettle={() => settleApi.callApi()}
//             />

//             {/* ADDED NEW STATUS CARD HERE */}
//             <SettlementStatusCard kycStatus={security?.kycStatus} /> 
            
//             <BankAccountsList 
//               accounts={bankAccounts || []} 
//               isLoading={dashboardApi.isLoading}
//               onAddClick={() => setIsAddBankOpen(true)}
//               onSetPrimary={(id) => primaryBankApi.callApi(id)}
//             />
//           </div>
//         </div>
//       </main>

//       {/* Modals */}
//       <SecurityModal
//         open = {isSecurityOpen}
//         onOpenChange={setIsSecurityOpen}
//         security={security}
//         refreshStatus={refreshAll}
//       />

//       <AddBankAccountModal
//         open={isAddBankOpen}
//         onOpenChange={setIsAddBankOpen}
//         onSubmit={(data) => addBankApi.callApi(data)}
//         isLoading={addBankApi.isLoading}
//       />


//     </div>
//   )

// }


import { AddBankAccountModal } from '@/components/common/AddBankAccountModal';
import { BankAccountsList } from '@/components/common/BankAccountsList';
import { Header } from '@/components/common/Header';
import { SecurityModal } from '@/components/common/SecurityModal';
import { RecentActivity } from '@/components/merchant/RecentActivity';
import { SettlementControl } from '@/components/merchant/SettlementControl';
import { SettlementStatusCard } from '@/components/merchant/SettlementStatusCard';
import { StatsCard } from '@/components/merchant/StatsCards';
import { PublicFooter } from '@/components/public/PublicFooter';
import { useMerchantActions, useMerchantDashboard, useRecentSettlements, useRecentTransactions } from '@/hooks/queries/useMerchantQueries';
import React, { useEffect, useState } from 'react'
import { LABELS } from '@/config/labels.config';

export const MerchantDashboard = () => {
  const labels = LABELS.pages.merchantDashboard;
  const [isSecurityOpen, setIsSecurityOpen] = useState(false);
  const [isAddBankOpen, setIsAddBankOpen] = useState(false);

  const dashQuery = useMerchantDashboard();
  const txQuery = useRecentTransactions(15);
  const setQuery = useRecentSettlements(15);

  const { settleFunds, addBank, setPrimaryAccount } = useMerchantActions();

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (dashQuery.data?.security?.kycStatus !== 'APPROVED') setIsSecurityOpen(true);
  }, [dashQuery.data?.security]);

  if (dashQuery.isLoading) return <div className="p-20 text-center font-bold text-slate-400">{labels.loading}</div>;

  const {stats, security, bankAccounts, profile} = dashQuery.data;

  // console.log(dashQuery.data?.security?.kycStatus)
  // console.log(dashQuery.data?.security?.kycStatus !== "APPROVED")

  return (
    <div className="min-h-screen bg-slate-50/50">
      <Header
        user={profile}
        security={security}
        onSecurityAction={() => setIsSecurityOpen(true)}
      />
      <main className="container mx-auto p-4 md:p-8 max-w-7xl space-y-6">
        {/* ROW 1: Metrics & Settlement Action */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <StatsCard stats={stats} />
            <RecentActivity 
              transactions={txQuery.data}
              settlements={setQuery.data}
              isTxLoading={txQuery.isLoading}
              isSetLoading={setQuery.isLoading}
            /> 
          </div>
          
          <div className="lg:col-span-1 space-y-6">
            <SettlementControl 
              pendingAmount={stats?.pending} 
              kycStatus={security?.kycStatus}
              isLoading={settleFunds.isPending}
              onSettle={() => settleFunds.mutate()}
            />

            {/* ADDED NEW STATUS CARD HERE */}
            <SettlementStatusCard kycStatus={security?.kycStatus} /> 
            
            <BankAccountsList 
              accounts={bankAccounts || []} 
              isLoading={setPrimaryAccount.isPending}
              onAddClick={() => setIsAddBankOpen(true)}
              onSetPrimary={(id) => setPrimaryAccount.mutate(id)}
            />
          </div>
        </div>
        <PublicFooter/>
      </main>

      {/* Modals */}
      <SecurityModal
        open = {isSecurityOpen}
        onOpenChange={setIsSecurityOpen}
        security={security}
        refreshStatus={dashQuery.refetch}
      />

      <AddBankAccountModal
        open={isAddBankOpen}
        onOpenChange={setIsAddBankOpen}
        onSubmit={(payload) => addBank.mutate(payload, {
          onSuccess: () => setIsAddBankOpen(false)
        })}
        isLoading={addBank.isPending}
      />


    </div>
  )

}
