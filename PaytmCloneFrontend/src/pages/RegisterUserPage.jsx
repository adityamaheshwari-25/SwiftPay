import { zodResolver } from "@hookform/resolvers/zod";
import { registerUserSchema } from "../schemas/authSchema";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Lock, Mail, Phone, User, Loader2 } from "lucide-react";
import { motion } from "framer-motion";
import { InputField } from "../components/common/InputField";
import { Link, useNavigate } from "react-router-dom";
import { useRegisterUser } from "@/hooks/queries/useAuthMutations";
import { LABELS } from "@/config/labels.config";
import { AuthShell } from "@/components/auth/AuthShell";

export const RegisterUserPage = () => {
  const labels = LABELS.pages.registerUserPage;
  const MotionButton = motion.button;
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [isExiting, setIsExiting] = useState(false);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(registerUserSchema),
    mode: "onBlur"
  });

  const { mutate, isPending } = useRegisterUser();

  const onSubmit = async (data) => {
    mutate(data)
  };

  const navigateWithTransition = (to) => {
    setIsExiting(true);
    window.setTimeout(() => navigate(to), 280);
  };

  return (
    <AuthShell
      title={labels.title}
      subtitle={labels.subtitle}
      panelTitle="Welcome Back!"
      panelDescription="Already have an account? Sign in and continue where you left off with your wallet and payments."
      panelActionLabel={labels.signIn}
      onPanelAction={() => navigateWithTransition("/login")}
      panelOnLeft
      backLabel={labels.backToHome}
      onBack={() => navigateWithTransition("/")}
      isExiting={isExiting}
      footer={(
        <div className="auth-meta">
          {labels.alreadyHaveAccount}{" "}
          <Link to="/login" onClick={(e) => { e.preventDefault(); navigateWithTransition("/login"); }} className="auth-meta-link">
            {labels.signIn}
          </Link>
        </div>
      )}
    >
      <form onSubmit={handleSubmit(onSubmit)} className="auth-form" noValidate>
        <InputField
          icon={User}
          register={register('name')}
          placeholder={labels.fullNamePlaceholder}
          error={errors.name}
        />

        <InputField
          icon={Mail}
          register={register('email')}
          placeholder={labels.emailPlaceholder}
          error={errors.email}
        />

        <InputField
          icon={Phone}
          register={register('mobile')}
          placeholder={labels.mobilePlaceholder}
          error={errors.mobile}
        />

        <InputField
          icon={Lock}
          register={register('password')}
          placeholder={labels.passwordPlaceholder}
          showPasswordToggle
          showPassword={showPassword}
          onTogglePassword={() => setShowPassword(!showPassword)}
          error={errors.password}
        />

        <MotionButton
          whileHover={{ scale: 1.01 }}
          whileTap={{ scale: 0.99 }}
          type="submit"
          disabled={isSubmitting || isPending}
          className="auth-primary-btn mt-1"
        >
          {isPending ? <Loader2 className="mx-auto animate-spin" /> : labels.createAccount}
        </MotionButton>
      </form>
    </AuthShell>
  );
};
