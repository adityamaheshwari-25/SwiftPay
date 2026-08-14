import { useMutation, useQueryClient } from "@tanstack/react-query";
import { walletService } from "@/services/api/walletService";
import { bankAccountService } from "@/services/api/bankAccountService";
import { queryKeys } from "./queryKeys";
import { toast } from "sonner";

const useOptimisticWalletMutation = (
  queryClient,
  mutationFn,
  balanceModifier,
  successMsg,
) => useMutation({
  mutationFn,
  onMutate: async (variables) => {
    await queryClient.cancelQueries({ queryKey: queryKeys.user.dashboard() });

    const previousDashboard = queryClient.getQueryData(queryKeys.user.dashboard());

    if (previousDashboard) {
      queryClient.setQueryData(queryKeys.user.dashboard(), {
        ...previousDashboard,
        wallet: {
          ...previousDashboard.wallet,
          balance: balanceModifier(previousDashboard.wallet.balance, variables.amount),
        },
      });
    }

    return { previousDashboard };
  },
  onError: (err, variables, context) => {
    queryClient.setQueryData(queryKeys.user.dashboard(), context.previousDashboard);
    toast.error(err.response?.data?.message || "Transaction failed");
  },
  onSettled: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.user.dashboard() });
  },
  onSuccess: () => {
    toast.success(successMsg);
  },
});

export const useWalletActions = () => {
  const queryClient = useQueryClient();

  // Helper to handle optimistic logic for balance changes
  /**
   * 
   * Optimistic update is when you update the UI before getting confirmation from the server, that makes the application looks fast,
   * assuming the request will succeed, basically its kinda you are optimistic.
   * Basically,
   * 1. Updating the UI instantly.
   * 2. Sending API request in the backend.
   * 3. If the request succeeds then no change in the UI, already updated.
   * 4. If request fails, rollback the UI.
   * 
   * In pessimistic Update, you are waiting for the confirmation from the server before updating the UI. You are pessimistic.
   * 
   * Just for the sake of adding it, I tried adding this, in financial or sensitive operations, its still recommended to go with
   * pessimistic update only. 
   * 
   * onMutate runs before the mutationFn
   * 
   * This optimistic update is only for the faster UX.
   * 
   * previousDashboard is the snapshot inside onMutate. We return it so React Query stores it as context. 
   * Later, if the mutation fails, React Query provides that stored snapshot as context.previousDashboard so we can roll 
   * back the optimistic cache update.
   * 
   * variables is the mutation payload; it’s the same object passed to mutate(variables) 
   * and is available in onMutate/onError/onSuccess.
   * 
   * 
   * React Query mutation lifecycle is:

      onMutate runs first (before request)

      request runs

      if success:

            onSuccess

            onSettled

      if error:

              onError

              onSettled


      onMutate runs before the mutation function and is primarily used to perform optimistic updates, snapshot previous 
      state for rollback, cancel in-flight queries to prevent race conditions, and prepare mutation context.
   * 
   */
  // 1. Optimistic Add Money
  const addMoney = useOptimisticWalletMutation(
    queryClient,
    walletService.addMoney,
    (old, amount) => old + amount,
    "Funds added to wallet!"
  );

  // 2. Optimistic Transfer Money
  const transferMoney = useOptimisticWalletMutation(
    queryClient,
    walletService.transferMoney,
    (old, amount) => old - amount,
    "Money sent successfully!"
  );

  // 3. Optimistic Withdraw Money
  const withdrawMoney = useOptimisticWalletMutation(
    queryClient,
    walletService.withdrawMoney,
    (old, amount) => old - amount,
    "Withdrawal successful!"
  );

  // Standard Mutations (No optimistic update needed for bank linking)
  const setPrimaryBank = useMutation({
    mutationFn: bankAccountService.setPrimaryAccount,
    onSuccess: () => {
      toast.success("Primary bank updated!");
      queryClient.invalidateQueries({ queryKey: queryKeys.user.dashboard() });
    }
  });

  const createBank = useMutation({
    mutationFn: bankAccountService.createBankAccount,
    onSuccess: () => {
      toast.success("Bank account linked!");
      queryClient.invalidateQueries({ queryKey: queryKeys.user.dashboard() });
    }
  });

  return { addMoney, transferMoney, withdrawMoney, setPrimaryBank, createBank };
};
