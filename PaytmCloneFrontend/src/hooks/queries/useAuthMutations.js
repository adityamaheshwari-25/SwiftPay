import { authService } from "@/services/api/authService";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom"
import { toast } from "sonner";

export const useRegisterUser = () => {
    const navigate = useNavigate();
    
    return useMutation({
        mutationFn: authService.registerUser,
        onSuccess: (data) => {
            toast.success("Account created successfully!");
            if (data.token) {
            localStorage.setItem("authToken", data.token);
            navigate("/user/dashboard");
            } else {
             navigate("/login")   
            }
        },
        onError: (error) => {
            const message = error.response?.data?.message || "Registration failed. Please try again.";
            toast.error(message);
        }
    })
}

export const useRegisterMerchant = () => {
    const navigate = useNavigate();

    return useMutation({
        mutationFn: authService.registerMerchant,
        onSuccess: (data) => {
            toast.success("Merchant account created! Welcome aboard.");

            if (data.token) {
                localStorage.setItem("authToken", data.token);
                navigate("/merchant/dashboard");
            } else {
                navigate("/login");
            }
        },
        onError: (error) => {
            const message = error.response?.data?.message || "Merchant registration failed";
            toast.error(message);
        }
    })
}
