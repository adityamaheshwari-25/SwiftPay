import { useAuth } from "@/context/AuthContext";
import { ROLE_CONFIG } from "@/config/role.config";
import { useNavigate } from "react-router-dom";
import { Button } from "../ui/button";

export default function Sidebar() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const menu = ROLE_CONFIG[user.role]?.sidebar || [];

  return (
    <aside className="w-64 border-r bg-secondary p-4 hidden md:block">
      <h1 className="text-2xl font-bold text-primary mb-6">Paytm Clone</h1>
      <div className="space-y-2">
        {menu.map(item => (
          <Button
            key={item.path}
            variant="ghost"
            className="w-full justify-start"
            onClick={() => navigate(item.path)}
          >
            {item.label}
          </Button>
        ))}
      </div>
    </aside>
  );
}
