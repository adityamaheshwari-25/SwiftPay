import UserDashboard from "@/components/dashboard/UserDashboard";
import MerchantDashboard from "@/components/dashboard/MerchantDashboard";
import AdminDashboard from "@/components/dashboard/AdminDashboard";
import SuperAdminDashboard from "@/components/dashboard/SuperAdminDashboard";
import { APP_ROLES } from "./roles.config";

const DASHBOARD_MAP = {
  [APP_ROLES.USER]: UserDashboard,
  [APP_ROLES.MERCHANT]: MerchantDashboard,
  [APP_ROLES.ADMIN]: AdminDashboard,
  [APP_ROLES.SUPER_ADMIN]: SuperAdminDashboard,
};
