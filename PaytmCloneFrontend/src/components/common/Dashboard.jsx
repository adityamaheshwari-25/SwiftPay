// Dashboard.jsx
import { useAuth } from "@/hooks/useAuth";
import { DASHBOARD_MAP } from "@/config/dashboard.map";
import { UserDashboard } from "../dashboard/UserDashboard";

export default function Dashboard() {
  const { user } = useAuth();
  const Component = DASHBOARD_MAP[user.role] || UserDashboard;

  return <Component />;
}
