import PendingKycList from "@/components/kyc/PendingKycList";
import { AdminLayout } from "@/components/layout/AdminLayout";

export default function AdminDashboard() {
  
  return (
    <AdminLayout>
      <PendingKycList />
      {/* Add more widgets like Reports, Transactions */}
    </AdminLayout>
  );
}
