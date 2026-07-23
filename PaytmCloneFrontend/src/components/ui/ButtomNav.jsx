import { motion } from "framer-motion";
import { Home, History, QrCode, User, Store } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";

const userNavItems = [
  { icon: <Home className="w-5 h-5" />, label: "Home", path: "/" },
  { icon: <History className="w-5 h-5" />, label: "History", path: "/history" },
  { icon: <QrCode className="w-5 h-5" />, label: "Scan", path: "/scan" },
  { icon: <User className="w-5 h-5" />, label: "Profile", path: "/profile" },
];

const merchantNavItems = [
  { icon: <Store className="w-5 h-5" />, label: "Dashboard", path: "/merchant" },
  { icon: <QrCode className="w-5 h-5" />, label: "Receive", path: "/merchant/receive" },
  { icon: <History className="w-5 h-5" />, label: "History", path: "/merchant/history" },
  { icon: <User className="w-5 h-5" />, label: "Profile", path: "/merchant/profile" },
];

export function BottomNav({ variant = "user" }) {
  const navigate = useNavigate();
  const location = useLocation();
  const navItems = variant === "user" ? userNavItems : merchantNavItems;

  return (
    <motion.nav
      initial={{ y: 100 }}
      animate={{ y: 0 }}
      transition={{ type: "spring", damping: 20 }}
      className="fixed bottom-0 left-0 right-0 bg-card/95 backdrop-blur-xl border-t border-border safe-area-bottom z-50"
    >
      <div className="flex items-center justify-around py-3 px-4 max-w-lg mx-auto">
        {navItems.map((item) => {
          const isActive = location.pathname === item.path;
          return (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              className="flex flex-col items-center gap-1 px-4 py-1 relative"
            >
              {isActive && (
                <motion.div
                  layoutId="navIndicator"
                  className="absolute -top-3 w-8 h-1 rounded-full bg-primary"
                  transition={{ type: "spring", damping: 20 }}
                />
              )}
              <span className={isActive ? "text-primary" : "text-muted-foreground"}>
                {item.icon}
              </span>
              <span className={`text-xs font-medium ${isActive ? "text-primary" : "text-muted-foreground"}`}>
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </motion.nav>
  );
}
