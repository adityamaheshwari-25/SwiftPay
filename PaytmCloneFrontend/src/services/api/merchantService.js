// src/services/api/merchantService.js
import api from "./axios";

export const merchantService = {
  // Individual Data Fetches
  getStats: async () => {
   const res = await api.get("/merchant/stats")
   return res.data;
  },

  getProfile: async () => {
    const res = await api.get("/users/profile");
    return res.data;
  },
  getSecurityStatus: async () => {
    const res = await api.get("/users/security-status");
    return res.data;
  },
  getBankAccounts: async () => {
    const res = await api.get("/bank-accounts")
    return res.data;
  },

  // Actions
    triggerInstantSettlement: async () => {
        const res = await api.post("/merchant/settlements/instant");
        return res.data;
    },

    addBankAccount: async  (data) => {
        const res = await api.post("/bank-accounts", data)
        return res.data;
    },

    getRecentTransactions: async (page = 0, size=15) => {
        const res = await api.get(`/merchant/dashboard/transactions?page=${page}&size=${size}`);
        return res.data;
    },

    getSettlementHistory: async (page =0, size = 15) => {
        const res = await api.get(`/merchant/settlements?page=${page}&size=${size}`);
        return res.data;
    },

    getMerchantDashboard: async () => {
      const res = await api.get("/merchant/dashboard");
      return res.data;
    }

};