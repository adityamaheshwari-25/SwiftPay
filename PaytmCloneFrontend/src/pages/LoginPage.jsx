import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { loginSchema } from "../schemas/authSchema";
import { motion } from "framer-motion";
import { Lock, Mail } from "lucide-react";
import { InputField } from "../components/common/InputField";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { LABELS } from "@/config/labels.config";
import { AuthShell } from "@/components/auth/AuthShell";

export const LoginPage = () => {
  const labels = LABELS.pages.loginPage;
  const MotionButton = motion.button;
  const [showPassword, setShowPassword] = useState(false);
  const [isExiting, setIsExiting] = useState(false);
  const [error, setError] = useState(null);

  const { login, getHomeRouteForRole } = useAuth();
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data) => {
    setError(null);

    try {
      const role = await login({
        email: data.email,
        password: data.password,
      });

      navigate(getHomeRouteForRole(role));
    } catch {
      setError(labels.invalidCredentials);
    }
  };

  const navigateWithTransition = (to) => {
    setIsExiting(true);
    window.setTimeout(() => navigate(to), 280);
  };

  return (
    <AuthShell
      title={labels.title}
      subtitle={labels.subtitle}
      panelTitle="Hello, Friend!"
      panelDescription="New to SwiftPay? Create a secure account and start paying or accepting payments in minutes."
      panelActionLabel="Create account"
      onPanelAction={() => navigateWithTransition("/register-user")}
      backLabel={labels.backToHome}
      onBack={() => navigateWithTransition("/")}
      isExiting={isExiting}
      footer={(
        <div className="auth-meta">
          {labels.noAccount}{" "}
          <button type="button" onClick={() => navigateWithTransition("/register-user")} className="auth-meta-link">
            {labels.registerUser}
          </button>
          {" or "}
          <button type="button" onClick={() => navigateWithTransition("/register-merchant")} className="auth-meta-link">
            {labels.registerMerchant}
          </button>
        </div>
      )}
    >
      <form onSubmit={handleSubmit(onSubmit)} className="auth-form" noValidate>
        <InputField icon={Mail} register={register("email")} placeholder={labels.emailPlaceholder} error={errors.email} />

        <InputField
          icon={Lock}
          register={register("password")}
          placeholder={labels.passwordPlaceholder}
          showPasswordToggle
          showPassword={showPassword}
          onTogglePassword={() => setShowPassword(!showPassword)}
          error={errors.password}
        />


        {error && <p className="text-sm text-red-600 text-center mt-1">{error}</p>}

        <MotionButton
          whileHover={{ scale: 1.01 }}
          whileTap={{ scale: 0.99 }}
          type="submit"
          disabled={isSubmitting}
          className="auth-primary-btn mt-1"
        >
          {isSubmitting ? labels.signingIn : labels.signIn}
        </MotionButton>
      </form>
    </AuthShell>
  );
};
