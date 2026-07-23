import api from "./axios";

export const authService = {
    login: async (credentials) => {
        const response = await api.post("/auth/login", credentials);
        return response.data;
    },

    registerUser: async (userData) => {
        const response = await api.post("/users/register", userData);
        return response.data;
    },

    registerMerchant: async (merchantData) => {
        const response = await api.post("/merchants/register", merchantData);
        return response.data;
    },

    getRoles: async () => {
        const response = await api.get("/roles");
        const roles = Array.isArray(response.data) ? response.data : [];
        return roles.map((role) => String(role || "").trim().toUpperCase()).filter(Boolean);
    },

    getMerchantCategories: async () => {
        const response = await api.get("/merchants/categories");
        const raw = response.data;
        const items = Array.isArray(raw)
            ? raw
            : Array.isArray(raw?.categories)
                ? raw.categories
                : [];

        return items
            .map((item) => {
                if (typeof item === "string") {
                    return { value: item, label: item };
                }
                const value = item?.value ?? item?.code ?? item?.name;
                const label = item?.label ?? item?.name ?? item?.value;
                if (!value || !label) return null;
                return { value: String(value), label: String(label) };
            })
            .filter(Boolean);
    },

    logout: () => {
        localStorage.removeItem('authToken');
    }
}
