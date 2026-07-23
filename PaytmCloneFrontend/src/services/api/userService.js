import api from "./axios";

export const userService = {
    lookupByMobile: async (mobile) => {
        const response = await api.get(`/users/lookup`, {
            params: {mobile}
        });
        return response.data;
    },
    getSecurityStatus: async () => {
        const res = await api.get("/users/security-status");
        return res.data;
    },
    
    getProfile: async () => {
        const response = await api.get("/users/profile");
        return response.data; // Matches UserResponseDto
    },

    getDashboard: async () => {
        const response = await api.get("/users/dashboard");
        console.log(response)
        return response.data;
    }
};