import { useInfiniteQuery } from "@tanstack/react-query";
import { transactionService } from "@/services/api/transactionService";
import { queryKeys } from "./queryKeys";

export const useInfiniteTransactions = () => {
  return useInfiniteQuery({
    queryKey: queryKeys.transactions.infinite(),
    queryFn: ({ pageParam = 0 }) => transactionService.getMyTransactions(pageParam, 10), //
    getNextPageParam: (lastPage) => lastPage.last ? undefined : lastPage.number + 1,
    initialPageParam: 0,
  });
};