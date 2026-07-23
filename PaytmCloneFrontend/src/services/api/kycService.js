import api from "./axios";

export const kycService = {
  getMyKycStatus: async () => {
    const res = await api.get("/users/kyc/me/status");
    return res.data;
  },

  uploadKyc: async (file) => {
    const formData = new FormData();
    formData.append("file", file);

    const res = await api.post("/kyc/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });

    return res.data;
  },

  viewMyKycDocument: async () => {
    const res = await api.get("/users/kyc/me/file", {
      responseType: "blob",
    });
    return res;
  },
};
