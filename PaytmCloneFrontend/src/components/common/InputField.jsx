import { Eye, EyeOff } from "lucide-react";

export const InputField = ({
  icon,
  error,
  type = 'text',
  register,
  placeholder,
  showPasswordToggle,
  onTogglePassword,
  showPassword,
  disabled = false,
}) => {
  const Icon = icon;

  return (
    <div className="relative">
      <div className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
        <Icon className="w-5 h-5" />
      </div>
      <input
        type={showPasswordToggle ? (showPassword ? 'text' : 'password') : type}
        {...register}
        placeholder={placeholder}
        disabled={disabled}
        className={`w-full pl-10 ${showPasswordToggle ? 'pr-10' : 'pr-4'} py-3 border ${
          error ? 'border-red-300 focus:border-red-500' : 'border-slate-300 focus:border-cyan-500'
        } rounded-lg focus:outline-none focus:ring-2 focus:ring-cyan-200 transition disabled:cursor-not-allowed disabled:opacity-60`}
      />
      {showPasswordToggle && (
        <button
          type="button"
          onClick={onTogglePassword}
          disabled={disabled}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
        </button>
      )}
      {error && <p className="text-red-500 text-sm mt-1">{error.message}</p>}
    </div>
  );
};
