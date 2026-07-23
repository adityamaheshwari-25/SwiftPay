import DashboardLayout from "@/components/layout/DashboardLayout";
import PendingKycList from "@/components/kyc/PendingKycList";

export default function SuperAdminDashboard() {
  return (
    <DashboardLayout>
      <PendingKycList />
      {/* Add RoleManagement and Audit Logs */}
    </DashboardLayout>
  );
}
