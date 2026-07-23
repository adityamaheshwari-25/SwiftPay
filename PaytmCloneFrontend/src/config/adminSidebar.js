import { LayoutDashboard, UserCheck, ShieldCheck, Users, Settings, TrendingUp } from "lucide-react";

export const ADMIN_SIDEBAR_LINKS = [
  {
    group: "Core",
    items: [{ label: "Overview", icon: LayoutDashboard, path: "/admin/dashboard" }],
  },
  {
    group: "Compliance",
    items: [
      { label: "KYC Verifications", icon: UserCheck, path: "/admin/kyc" },
      { label: "Audit Logs", icon: ShieldCheck, path: "/admin/audit" },
    ],
  },
  {
    group: "Management",
    items: [
      { label: "High Value Merchants", icon: TrendingUp, path: "/admin/high-value-merchants" }, // ✅ new
      { label: "Merchants", icon: Users, path: "/admin/merchants" },
      { label: "Settings", icon: Settings, path: "/admin/settings" },
    ],
  },
];
