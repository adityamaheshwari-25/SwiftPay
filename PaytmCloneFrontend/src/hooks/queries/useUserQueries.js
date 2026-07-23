import { userService } from "@/services/api/userService";
import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "./queryKeys";
import { useAuth } from "@/context/AuthContext";
import { APP_ROLES } from "@/config/roles.config";

/**
 * 
 * Actually here staleTime has no meaning, as it will refetch in every 15sec, and it is kept 15 seconds coz the dashboard
 * data tends to keep changing.
 */
export const useUserDashboard = () => {
    return useQuery({
        queryKey: queryKeys.user.dashboard(),
        queryFn: userService.getDashboard,
        staleTime: 1000 * 30, // 30 seconds
        refetchInterval: 1000 * 15, // Auto-refresh every 15 seconds (Polling)
        refetchOnWindowFocus: true
    })
}

/**
 * here enabled means the query will only run if the user is logged in or user is not admin.
 */
export const useSecurityStatus = () => {
    const { isAuthenticated, hasRole } = useAuth();

    return useQuery({
        queryKey: queryKeys.user.security(),
        queryFn: userService.getSecurityStatus,
        enabled: isAuthenticated && !hasRole(APP_ROLES.ADMIN), // only fetch if logged in and not an admin
        staleTime: 1000 * 60 * 5, // data stays "fresh" for 5minutes, as it doesn't changes frequently that's why made it longer as 5min.
    })
}
