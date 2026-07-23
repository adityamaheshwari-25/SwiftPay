import api from "./axios";

export const mpinService = {
    setMpin : async (data) => {
        const response = await api.post("/user/set-mpin", data);
        return response.data;
    }
}