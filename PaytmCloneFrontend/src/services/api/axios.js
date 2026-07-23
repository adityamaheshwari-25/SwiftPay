import axios from "axios"
import { generateIdempotencyKey } from "../../utils/idempotency"

const API_URL = import.meta.env.VITE_API_BASE_URL

const api = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
})

// Attach JWT automatically with every request if it is there, for the Authentication in the Controller in the backend.
api.interceptors.request.use((config) => {
  console.log("➡️ API call:", config.url)
  const token = localStorage.getItem("authToken")
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  if (
    config.url?.includes("/wallet/add-money") ||
    config.url?.includes("/wallet/withdraw") ||
    config.url?.includes("/wallet/transfer") ||
    config.url?.includes("/splits")
  ) {
    config.headers["Idempotency-Key"] =
      config.headers["Idempotency-Key"] || generateIdempotencyKey()
  }

  
  return config
})

// response interceptor: handle session expiration, backend sends 401 error code when the session expires, so removing the token from the backend.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const requestUrl = String(error.config?.url || "");
    const isLoginRequest = requestUrl.includes("/auth/login");
    const isOnLoginPage = window.location.pathname === "/login";

    if (status === 401 && !isLoginRequest) {
      localStorage.removeItem("authToken");
      if (!isOnLoginPage) {
        window.location.replace("/login");
      }
    }
    return Promise.reject(error);
  }
)



export default api;
