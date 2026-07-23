import api from "./axios";

export const walletService = {
  getMyWalletService: async () => {
    const response = await api.get("wallet/me")
    return response.data
  },

  addMoney: async (data) => {
    const response = await api.post("/wallet/add-money", data);
    return response.data;
  },

  withdrawMoney: async (data) => {
    const response = await api.post("/wallet/withdraw", data);
    return response.data;
  },

  transferMoney: async (data) => {
    const response = await api.post("/wallet/transfer", data);
    return response.data;
  },

  getSpendingInsight: async () => {
    const response = await api.get("/wallet/spending-insight");
    return response.data;
  },
}