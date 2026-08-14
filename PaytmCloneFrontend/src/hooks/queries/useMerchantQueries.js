import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { queryKeys } from "./queryKeys"
import { merchantService } from "@/services/api/merchantService"
import { toast } from "sonner";
import { bankAccountService } from "@/services/api/bankAccountService";

export const useMerchantDashboard = () => {
    return useQuery({
        queryKey: queryKeys.merchant.dashboard(),
        queryFn: merchantService.getMerchantDashboard,
        staleTime: 1000 * 5, // 5sec as merchant dashboard is often changed frequently.
    });
};

/**
 * Can we use useMutation without onMutate?
 * Yes. onMutate is optional and mainly used for optimistic updates and rollback handling. If we’re following a pessimistic 
 * approach where we wait for server confirmation and then invalidate queries, onMutate isn’t required. 
 * 
 */
export const useMerchantActions = () => {
    const queryClient = useQueryClient();

    const refresh = () => queryClient.invalidateQueries({ queryKey: queryKeys.merchant.all });

    const settleFunds = useMutation({
        mutationFn: merchantService.triggerInstantSettlement,
        onSuccess: () => {
            toast.success("Settlement initiated successfully!");
            refresh();
        },
        onError: (err) => toast.error(err.response?.data || "Settlement failed")
    });

    const addBank = useMutation({
        mutationFn: bankAccountService.createBankAccount,
        onSuccess: () => {
            toast.success("Bank account linked successfully!");
            refresh();
        }
    })

    const setPrimaryAccount = useMutation({
        mutationFn: bankAccountService.setPrimaryAccount,
        onSuccess: () => {
            toast.success("Primary bank updated");
            refresh();
        }
    });

    return { settleFunds, setPrimaryAccount, addBank };
}


export const useRecentTransactions = (size = 15) => {
    return useQuery({
        queryKey: queryKeys.merchant.transactions(0, size),
        queryFn: () => merchantService.getRecentTransactions(0, size),
        refetchInterval: 1000 * 15,
    })
}

// when the request hits, first it checks the cache, and if the stale time has been passed then it
// refetches it from the backend, and if not, it returns the cache, then what about the updated data, 
// if the data has been updated in the db, then also you are showing me the cached data, so that's why
// we are using polling right(as not implementing websockets).

export const useRecentSettlements = (size = 15) => {
    return useQuery({
        queryKey: queryKeys.merchant.settlements(0, size),
        queryFn: () => merchantService.getSettlementHistory(0, size),
        staleTime: 1000 * 60 * 2,
    })
}
/**
 * It prevents the table from flickering to a white screen/loading spinner every time the merchant clicks "Next."
 *  It keeps the old data visible until the new page is ready—making the app feel like a fast desktop software 
 * rather than a slow website.
 */
export const useMerchantTransactions = (page = 0, size = 20) => {
    return useQuery({
        queryKey: queryKeys.merchant.transactions(page, size),
        queryFn: () => merchantService.getRecentTransactions(page, size),
        placeholderData: (prev) => prev // keeps old data visible while fetching new page.
    })
}
/**
 * Normally what happens:

        Page changes

        New query runs

        data becomes undefined

        UI shows loading state

        New data arrives

    React Query does this instead:

        Page changes

        It keeps showing the old page’s data temporarily

        Fetches new page in background

        Replaces data when fetch completes

    isLoading is true only on the initial fetch when there’s no cached data yet; isFetching is true anytime a network request 
    is in flight, even if we already have data. So we use isLoading for initial skeletons and isFetching for background refresh 
    indicators.

    isLoading is about the first load (no cached data available).
    isFetching is about network activity (initial load, refetch, background refresh, pagination fetch, window focus refetch, etc.).

    isRefetching = isFetching && !isLoading
    A fetch is happening, but it’s not the first load.
 */
export const useSettlementHistory = (page =0, size = 15) => {
    return useQuery({
        queryKey: queryKeys.merchant.settlements(page, size),
        queryFn: () => merchantService.getSettlementHistory(page, size),
        /**
         * placeholderData expects actual placeholder data or a function returning it. The function pattern here mimics the old keepPreviousData: true behavior.
         */
        placeholderData: (previousData) => previousData, // React Query v4+, equivalent to old syntax => keepPreviousData: true(v3)
    })
}



