import { motion as Motion } from "framer-motion";

export function MetricCard({ title, value, subtitle, icon, trend, variant = "default", index = 0 }) {
  const Icon = icon;
  const variants = { default: "bg-card border-border/50", primary: "gradient-primary text-primary-foreground border-transparent", success: "gradient-success text-success-foreground border-transparent" };
  const iconBg = { default: "bg-muted", primary: "bg-primary-foreground/20", success: "bg-success-foreground/20" };

  return (
    <Motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * 0.1, duration: 0.4 }} className={`p-5 rounded-2xl border ${variants[variant]} shadow-card`}>
      <div className="flex items-start justify-between mb-4">
        <div className={`p-3 rounded-xl ${iconBg[variant]}`}><Icon className="w-5 h-5" /></div>
        {trend && <span className={`text-xs font-medium px-2 py-1 rounded-full ${trend.isPositive ? variant === "default" ? "bg-success/10 text-success" : "bg-primary-foreground/20" : variant === "default" ? "bg-destructive/10 text-destructive" : "bg-primary-foreground/20"}`}>{trend.isPositive ? "+" : ""}{trend.value}%</span>}
      </div>
      <p className={`text-sm mb-1 ${variant === "default" ? "text-muted-foreground" : "opacity-80"}`}>{title}</p>
      <p className="text-2xl font-display font-bold">{value}</p>
      {subtitle && <p className={`text-xs mt-1 ${variant === "default" ? "text-muted-foreground" : "opacity-70"}`}>{subtitle}</p>}
    </Motion.div>
  );
}
