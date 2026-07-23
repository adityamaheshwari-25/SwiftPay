import api from "./axios";

export const adminKycService = {
    getPendingKycs: async () => {
        const res = await api.get("/admin/kyc/pending");
        return res.data;
    },

    approveKyc: async (userId) => {
        const res = await api.post(`/admin/kyc/${userId}/approve`);
        return res.data
    },

    rejectKyc: async (userId, reason) => {
        const res = await api.post(`/admin/kyc/${userId}/reject`, {
            rejectionReason: reason,
        });
        return res.data
    },

    // as the backend is returning resource, it will open directly in a new tab.
    viewKycDocument: async (userId) => {
        const response = await api.get(
            `/admin/kyc/${userId}/document`,
            {
            responseType: "blob",
            // headers: {
            //     Authorization: `Bearer ${localStorage.getItem("token")}`,
            // },
            }
        );

        return {
            blob: response.data,
            contentType: response.headers["content-type"],
        };
    }
}