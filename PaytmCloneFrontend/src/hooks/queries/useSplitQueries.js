import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "./queryKeys";
import { splitService } from "@/services/api/splitService";
import { useAuth } from "@/context/AuthContext";
import { APP_ROLES } from "@/config/roles.config";

/**
 * Details query (works with your backend as-is)
 */
export const useSplitDetails = (splitId, enabled = true) => {
  const { isAuthenticated, hasRole } = useAuth();

  return useQuery({
    queryKey: queryKeys.splits.details(splitId),
    queryFn: () => splitService.getById(splitId),
    enabled: enabled && isAuthenticated && hasRole(APP_ROLES.USER) && !!splitId,
    staleTime: 1000 * 15,
    refetchOnWindowFocus: true,
  });
};


export const useSplitCreatedList = () => {
  const { isAuthenticated, hasRole } = useAuth();
  const enabled = isAuthenticated && hasRole(APP_ROLES.USER);

  return useQuery({
    queryKey: queryKeys.splits.created(),
    queryFn: splitService.listCreated,
    enabled,
    staleTime: 1000 * 20, 
    refetchOnWindowFocus: true, // if the user goes to another tab and returns back, then first checks the stale data, and if the data is stale, it refetches it.
  });
};

export const useSplitInvolvedList = () => {
  const { isAuthenticated, hasRole } = useAuth();
  const enabled = isAuthenticated && hasRole(APP_ROLES.USER);

  return useQuery({
    queryKey: queryKeys.splits.involved(),
    queryFn: splitService.listInvolved,
    enabled, // if the enabled is true only then the query will run otherwise not.
    staleTime: 1000 * 10, // taken 10sec coz the involved part can be changed frequently.
    refetchOnWindowFocus: true,
  });
};
