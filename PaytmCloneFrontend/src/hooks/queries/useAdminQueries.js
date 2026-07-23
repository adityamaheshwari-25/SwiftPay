// these all are hooks that I have written in this folder "queries"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { queryKeys } from "./queryKeys"
import { adminKycService } from "@/services/api/adminKycService"
import { toast } from "sonner"

// fetching pending kycs.
export const usePendingKycs = () => {
    return useQuery({
        queryKey: queryKeys.admin.kycList(),
        queryFn: adminKycService.getPendingKycs,
        staleTime: 1000 * 60 * 2 // 2mins
    })
}

// admin actions(approve/reject)

export const useKycActions = () => {
    const queryClient = useQueryClient();
    
    const approve = useMutation({
        mutationFn: (userId) => adminKycService.approveKyc(userId),
        onSuccess: () => {
            toast.success("Kyc application approved");

            // global invalidation: refetch the list everywhere
            queryClient.invalidateQueries({ queryKey: queryKeys.admin.kycList() });
        },
        onError: (err) => toast.error(err.response?.data || "Approval failed")
    });

    const reject = useMutation({

        // an object holding both userId and reason.
        mutationFn: ({ userId, reason }) => adminKycService.rejectKyc(userId, reason),
        onSuccess: () => {
            toast.info("Application rejected and user notified");
            queryClient.invalidateQueries( {queryKey: queryKeys.admin.kycList() });
        },
        onError: (err) => toast.error(err.response?.data || "Rejection failed")
    })

    return { approve, reject };
}