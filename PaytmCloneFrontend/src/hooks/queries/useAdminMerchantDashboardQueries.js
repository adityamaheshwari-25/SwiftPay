import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "./queryKeys";
import { adminMerchantDashboardService } from "@/services/api/adminMerchantDashboardService";


// backend handles missing params, and then set to the default backend values.
export const useHighValueMerchantSummary = (params = {}) => {
  return useQuery({
    queryKey: queryKeys.admin.highValueMerchantSummary(params),
    queryFn: () =>
      adminMerchantDashboardService.getHighValueMerchantsSummary(params),
    staleTime: 1000 * 60 * 2, // 2 minutes
    keepPreviousData: true,   
    retry: 1,
  });
};

/**
 * Transactions hook
 * params: merchantId (required), params optional: { minAmount, limit, offset }
 * Backend applies defaults if params are omitted.
 */
export const useHighValueMerchantTransactions = (merchantId, params = {}) => {
  const { minAmount, limit, offset } = params;

  return useQuery({
    // Use params object in key so cache differs when params differ
    queryKey: queryKeys.admin.highValueMerchantTransactions(merchantId, params),
    queryFn: () =>
      adminMerchantDashboardService.getMerchantHighValueTransactions({
        merchantId,
        ...(minAmount !== undefined ? { minAmount } : {}),
        ...(limit !== undefined ? { limit } : {}),
        ...(offset !== undefined ? { offset } : {}),
      }),
    enabled: !!merchantId,
    staleTime: 1000 * 30,
    keepPreviousData: true,
    retry: 1,
  });
};

/**
 * kept enabled as false because if not then on the component load only it will download that 
 * excel file which we don't want, and we only want when we gonna click on the button, and doing 
 * it via refetch(), which explicitly invoke the queryFn.
 */
export const useHighValueMerchantSummaryDownload = (params = {}) => {
  return useQuery({
    queryKey: [...queryKeys.admin.all, "merchant", "highValue", "summary", "xlsx", params],
    queryFn: () =>
      adminMerchantDashboardService.downloadHighValueMerchantsSummaryXlsx(params),
    enabled: false,
    retry: 0,
  });
};

export const useHighValueMerchantTransactionsDownload = (merchantId, params = {}) => {
  return useQuery({
    queryKey: [
      ...queryKeys.admin.all,
      "merchant",
      "highValue",
      "transactions",
      "xlsx",
      { merchantId, ...params },
    ],
    queryFn: () =>
      adminMerchantDashboardService.downloadMerchantHighValueTransactionsXlsx({
        merchantId,
        ...params,
      }),
    enabled: false,
    retry: 0,
  });
};
