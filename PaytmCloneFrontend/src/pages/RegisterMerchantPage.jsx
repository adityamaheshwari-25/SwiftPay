import { zodResolver } from "@hookform/resolvers/zod";
import { registerMerchantSchema } from "../schemas/authSchema";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Lock, Mail, Phone, User, Briefcase, Tag, AlertCircle, CheckCircle } from "lucide-react";
import { motion } from "framer-motion";
import { InputField } from "../components/common/InputField";
import { Link, useNavigate } from "react-router-dom";
import { useRegisterMerchant } from "@/hooks/queries/useAuthMutations";
import { useMerchantCategories } from "@/hooks/queries/useAuthQueries";
import { LABELS } from "@/config/labels.config";
import { AuthShell } from "@/components/auth/AuthShell";

export const RegisterMerchantPage = () => {
  const labels = LABELS.pages.registerMerchantPage;
  const MotionButton = motion.button;
  const [showPassword, setShowPassword] = useState(false);
  const [error, _setError] = useState(null);
  const [success, _setSuccess] = useState(false);
  const [isExiting, setIsExiting] = useState(false);
  const navigate = useNavigate();

  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(registerMerchantSchema),
    mode: "onBlur"
  });

  const { mutate, isPending } = useRegisterMerchant();
  const categoriesQuery = useMerchantCategories();
  const categories = categoriesQuery.data || [];


  const onSubmit = async (data) => {
    mutate(data);
  };

  const navigateWithTransition = (to) => {
    setIsExiting(true);
    window.setTimeout(() => navigate(to), 280);
  };

  return (
    <AuthShell
      title={labels.title}
      subtitle={labels.subtitle}
      panelTitle="Start Growing with SwiftPay"
      panelDescription="Get a merchant account for QR collections, settlements, and business payment insights."
      panelActionLabel={labels.signIn}
      onPanelAction={() => navigateWithTransition("/login")}
      panelOnLeft
      backLabel={labels.backToPortal}
      onBack={() => navigateWithTransition("/")}
      isExiting={isExiting}
      footer={(
        <div className="auth-meta">
          {labels.existingPartner}{" "}
          <Link to="/login" onClick={(e) => { e.preventDefault(); navigateWithTransition("/login"); }} className="auth-meta-link">
            {labels.signIn}
          </Link>
        </div>
      )}
    >
      <form onSubmit={handleSubmit(onSubmit)} className="auth-form" noValidate>
        {error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-1 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-start"
          >
            <AlertCircle className="w-5 h-5 text-red-500 mr-2 flex-shrink-0 mt-0.5" />
            <p>{error}</p>
          </motion.div>
        )}

        {success && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-1 rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-700 flex items-start"
          >
            <CheckCircle className="w-5 h-5 text-green-500 mr-2 flex-shrink-0 mt-0.5" />
            <p>{labels.successMessage}</p>
          </motion.div>
        )}

        <InputField
          icon={User}
          register={register('name')}
          placeholder={labels.fullNamePlaceholder}
          error={errors.name}
          disabled={isPending}
        />

        <InputField
          icon={Mail}
          register={register('email')}
          placeholder={labels.emailPlaceholder}
          error={errors.email}
          disabled={isPending}
        />

        <InputField
          icon={Phone}
          register={register('mobile')}
          placeholder={labels.mobilePlaceholder}
          error={errors.mobile}
          disabled={isPending}
        />

        <InputField
          icon={Lock}
          register={register('password')}
          placeholder={labels.passwordPlaceholder}
          showPasswordToggle
          showPassword={showPassword}
          onTogglePassword={() => setShowPassword(!showPassword)}
          error={errors.password}
          disabled={isPending}
        />

        <div className="relative">
          <div className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
            <Briefcase className="w-5 h-5" />
          </div>
          <input
            {...register('businessName')}
            placeholder={labels.businessNamePlaceholder}
            disabled={isPending}
            className={`w-full pl-10 pr-4 py-3 border ${
              errors.businessName ? 'border-red-300 focus:border-red-500' : 'border-slate-300 focus:border-cyan-500'
            } rounded-lg focus:outline-none focus:ring-2 focus:ring-cyan-200 transition`}
          />
          {errors.businessName && <p className="text-red-500 text-sm mt-1">{errors.businessName.message}</p>}
        </div>

        <div className="relative">
          <div className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
            <Tag className="w-5 h-5" />
          </div>
          <select
            {...register('category')}
            disabled={isPending}
            className={`w-full pl-10 pr-4 py-3 border ${
              errors.category ? 'border-red-300 focus:border-red-500' : 'border-slate-300 focus:border-cyan-500'
            } rounded-lg focus:outline-none focus:ring-2 focus:ring-cyan-200 transition appearance-none bg-white`}
          >
            <option value="">{labels.categoryPlaceholder}</option>
            {categoriesQuery.isLoading && <option value="" disabled>Loading categories...</option>}
            {categories.map((cat) => (
              <option key={cat.value} value={cat.value}>{cat.label}</option>
            ))}
          </select>
          {errors.category && <p className="text-red-500 text-sm mt-1">{errors.category.message}</p>}
          {categoriesQuery.isError && (
            <p className="text-amber-600 text-sm mt-1">Failed to load categories from server.</p>
          )}
        </div>

        <MotionButton
          whileHover={{ scale: 1.01 }}
          whileTap={{ scale: 0.99 }}
          type="submit"
          disabled={isPending}
          className="auth-primary-btn mt-1"
        >
          {isPending ? labels.onboarding : labels.startAccepting}
        </MotionButton>
      </form>
    </AuthShell>
  );
};
