import api from "./axios";

export const splitService = {
  create: async (payload) => {
    const res = await api.post("/splits", payload);
    return res.data; // SplitDetailsResponseDto
  },

  getById: async (splitId) => {
    const res = await api.get(`/splits/${splitId}`);
    return res.data; // SplitDetailsResponseDto
  },

  pay: async ({ splitId, mpin }) => {
    const res = await api.post(`/splits/${splitId}/pay`, { mpin });
    return res.data; // SplitPayResponseDto
  },

  listCreated: async () => {
    const res = await api.get("/splits/me/created");
    return res.data;
  },
  
  listInvolved: async () => {
    const res = await api.get("/splits/me/involved");
    return res.data;
  }
};
