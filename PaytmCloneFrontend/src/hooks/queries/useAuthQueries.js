import { useQuery } from "@tanstack/react-query";
import { authService } from "@/services/api/authService";
import { queryKeys } from "./queryKeys";

export const useMerchantCategories = () => {
  return useQuery({
    queryKey: queryKeys.auth.merchantCategories(),
    queryFn: authService.getMerchantCategories,
    staleTime: 1000 * 60 * 30,
    retry: 1,
  });
};
