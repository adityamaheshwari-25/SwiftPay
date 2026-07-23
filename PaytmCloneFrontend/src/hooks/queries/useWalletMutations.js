import { useMutation, useQueryClient } from "@tanstack/react-query";
import { walletService } from "@/services/api/walletService";
import { bankAccountService } from "@/services/api/bankAccountService";
import { queryKeys } from "./queryKeys";
import { toast } from "sonner";

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
  const createOptimisticMutation = (mutationFn, balanceModifier, successMsg) => {
    return useMutation({
      mutationFn,
      onMutate: async (variables) => { // variables is the input you pass when you call the mutation.
        /**
         * 
         * Cancel outgoing fetches so they don't overwrite our optimistic update, because if any fetching happens, it gonna
         * send outdated data(stale fetch) because till now we haven't called the mutationFn which gonna fetch the correct upto date value.
         */
        
        await queryClient.cancelQueries({ queryKey: queryKeys.user.dashboard() });

        /**
         * we are keeping previousDashboard before changing the cache so that if the update doesn't happens properly, then
         * we can roll back basically set the previousDashboard only in onError function.
         * This is the heart of optimistic updates.
         * 
         * context is the thing returned by onMutate function.
         */
        const previousDashboard = queryClient.getQueryData(queryKeys.user.dashboard());

        // Optimistically update the cache
        if (previousDashboard) {

          // directly setting the cache value
          queryClient.setQueryData(queryKeys.user.dashboard(), {
            ...previousDashboard,
            wallet: {
              ...previousDashboard.wallet,
              balance: balanceModifier(previousDashboard.wallet.balance, variables.amount),
            },
          });
        }

        /**
         * React Query stores it and passes it to onError / onSettled later as context.
         * we return previousDashboard so that React Query can store that in the context.
         * */ 
        return { previousDashboard };
      },
      onError: (err, variables, context) => {
        // Roll back to the previous state if the API fails
        queryClient.setQueryData(queryKeys.user.dashboard(), context.previousDashboard);
        toast.error(err.response?.data?.message || "Transaction failed");
      },
      /*
        This runs after success or error, 
      */
      onSettled: () => {
        // Always refetch after error or success to sync with server truth
        queryClient.invalidateQueries({ queryKey: queryKeys.user.dashboard() });
      },
      onSuccess: () => {
        toast.success(successMsg);
      },
    });
  };

  // 1. Optimistic Add Money
  const addMoney = createOptimisticMutation(
    walletService.addMoney,
    (old, amount) => old + amount,
    "Funds added to wallet!"
  );

  // 2. Optimistic Transfer Money
  const transferMoney = createOptimisticMutation(
    walletService.transferMoney,
    (old, amount) => old - amount,
    "Money sent successfully!"
  );

  // 3. Optimistic Withdraw Money
  const withdrawMoney = createOptimisticMutation(
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