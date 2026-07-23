import api from "./axios"

export const bankAccountService = {
  createBankAccount: async (data) => {
    const response = await api.post("/bank-accounts", data)
    return response.data
  },

  getMyBankAccounts: async () => {
    const response = await api.get("/bank-accounts")
    return response.data
  },

  setPrimaryAccount: async (accountId) => {
    const response = await api.patch(`/bank-accounts/${accountId}/set-primary`);
    return response.data;
  }
}