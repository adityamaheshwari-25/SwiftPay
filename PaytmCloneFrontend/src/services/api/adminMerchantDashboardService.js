import api from "./axios"

export const adminMerchantDashboardService = {
    getHighValueMerchantsSummary: async ({minAmount = 50000, q = "", limit=20, offset =0} = {}) => {
        const res = await api.get("/admin/merchant/high-value/summary", {
            params: 
            { 
                minAmount, // don't send empty string; let backend normalize
                ...(q ? { q } : {}), 
                limit, 
                offset 
            },
        });
        return res.data;
    },

    getMerchantHighValueTransactions: async ({ merchantId, minAmount = 50000, limit = 50, offset = 0 }) => {
        const res = await api.get(`admin/merchant/${merchantId}/high-value/transactions`, {
            params: { minAmount, limit, offset },
        });
        return res.data;
    },

    downloadHighValueMerchantsSummaryXlsx: async ({ minAmount = 50000, q = "" } = {}) => {
        const res = await api.get("/admin/merchants/reports/high-value.xlsx", {
            params: {
                minAmount,
                ...(q ? { q } : {}),
            },
            responseType: "blob",
        });
        return {
            blob: res.data,
            contentDisposition: res.headers["content-disposition"],
        };
    },

    downloadMerchantHighValueTransactionsXlsx: async ({ merchantId, minAmount = 50000 }) => {
        const res = await api.get(`/admin/merchants/reports/merchant/${merchantId}/high-value-txns.xlsx`, {
            params: { minAmount },
            responseType: "blob",
        });
        return {
            blob: res.data,
            contentDisposition: res.headers["content-disposition"],
        };
    },
}
