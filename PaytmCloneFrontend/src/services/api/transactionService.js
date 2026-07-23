import api from "./axios";

export const transactionService = {
  /**
   * Fetches paginated transaction history for the current user.
   * @param {number} page - The page number (0-indexed)
   * @param {number} size - Number of items per page
   */
  getMyTransactions: async (page = 0, size = 10) => {
    const response = await api.get("/transactions/my", {
      params: { page, size, sort: "createdAt,desc" }
    });
    return response.data; // Returns a Page object containing 'content'
  },
};