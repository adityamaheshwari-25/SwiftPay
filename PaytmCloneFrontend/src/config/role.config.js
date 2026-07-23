import { APP_ROLES } from "./roles.config";

export const ROLE_CONFIG = {
  [APP_ROLES.USER]: {
    sidebar: [
      { label: "Home", path: "/dashboard" },
      { label: "Wallet", path: "/wallet" },
      { label: "Transactions", path: "/transactions" },
    ],
    dashboard: "UserDashboard",
    headerTitle: "User Dashboard",
  },

  [APP_ROLES.MERCHANT]: {
    sidebar: [
      { label: "Overview", path: "/merchant" },
      { label: "Payments", path: "/merchant/payments" },
      { label: "Settlements", path: "/merchant/settlements" },
    ],
    dashboard: "MerchantDashboard",
    headerTitle: "Merchant Dashboard",
  },

  [APP_ROLES.ADMIN]: {
    sidebar: [
      { label: "Admin Panel", path: "/admin" },
      { label: "Users", path: "/admin/users" },
      { label: "Reports", path: "/admin/reports" },
    ],
    dashboard: "AdminDashboard",
    headerTitle: "Admin Dashboard",
  },

  [APP_ROLES.SUPER_ADMIN]: {
    sidebar: [
      { label: "System Control", path: "/root" },
      { label: "Roles Mgmt", path: "/root/roles" },
      { label: "Audit Logs", path: "/root/logs" },
    ],
    dashboard: "SuperAdminDashboard",
    headerTitle: "Super Admin Console",
  },
};
