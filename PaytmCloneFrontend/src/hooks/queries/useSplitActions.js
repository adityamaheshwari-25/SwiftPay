import { useMutation, useQueryClient } from "@tanstack/react-query";
import { splitService } from "@/services/api/splitService";
import { queryKeys } from "./queryKeys";
import { toast } from "sonner";

export const useSplitActions = () => {
  const qc = useQueryClient();

  // useMutation is used for POST/PUT/DELETE operations(write operations)
  const createSplit = useMutation({
    mutationFn: splitService.create,
    onSuccess: (data) => {
      toast.success("Split created");
      // details cache warm, this is manual cache update
      /**
       * Basically what "setQueryData" does is, it updates the cache manually, actually you are getting the whole split object "data" 
       * on success, so you are setting the data basically kinda map, setting it with the key only. No refetch triggered at any point
       * you are basically directly modifying the cache value using the key, cache already has the data -> instant rendering will happen. 
       * Depending on staleTime as well, it may or may not fetch. If you don't do that and when you go to that page, then you have to refetch 
       * that thing for sure, because cache has no data so it has to fetch from the server.
       * 
       * React Query logic:

            If cache has data AND it is NOT stale → show immediately, no refetch.

            If cache has data AND it IS stale → show immediately, then refetch in background.

            If cache has no data → show loading and fetch.
       * 
       */
      qc.setQueryData(queryKeys.splits.details(data.splitId), data);
      // lists should be invalidated if you add list endpoints
      qc.invalidateQueries({ queryKey: queryKeys.splits.created() }); // invalidate marks data as stale whereas setQueryData updates it.
      qc.invalidateQueries({ queryKey: queryKeys.splits.involved() });
    },
    onError: (e) => toast.error(e?.response?.data?.message || "Failed to create split"),
  });

  const paySplit = useMutation({
    mutationFn: splitService.pay,
    onSuccess: (data) => {
      toast.success("Payment successful");
      // refresh the split details
      qc.invalidateQueries({ queryKey: queryKeys.splits.details(data.splitId) });

      qc.invalidateQueries({ queryKey: queryKeys.splits.created() });
      qc.invalidateQueries({ queryKey: queryKeys.splits.involved() });
    },
    onError: (e) => toast.error(e?.response?.data?.message || "Payment failed"),
  });

  return { createSplit, paySplit };
};
