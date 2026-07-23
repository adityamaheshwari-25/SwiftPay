import React from "react";
import { Link, Outlet, useLocation } from "react-router-dom";
import { LogOut, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Header } from "../common/Header"; 
import { ADMIN_SIDEBAR_LINKS } from "@/config/adminSidebar";
import { cn } from "@/lib/utils";
import { useAuth } from "@/context/AuthContext";

export const AdminLayout = () => {
  const location = useLocation();
  const { logout } = useAuth();

  
  const currentLabel = ADMIN_SIDEBAR_LINKS
    .flatMap(g => g.items)
    .find(i => i.path === location.pathname)?.label || "Dashboard";

  return (
    <div className="min-h-screen bg-background flex flex-col">
      
      <Header />

      <div className="flex flex-1 pt-16">
        
        
        <aside className="w-64 bg-slate-900 text-slate-300 flex flex-col fixed bottom-0 top-16 left-0 z-20 border-r border-border shadow-xl">
          <nav className="flex-1 px-4 py-8 space-y-8 overflow-y-auto">
            {ADMIN_SIDEBAR_LINKS.map((group) => (
              <div key={group.group} className="space-y-2">
                <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500 px-4">
                  {group.group}
                </h3>
                <div className="space-y-1">
                  {group.items.map((item) => {
                    const isActive = location.pathname === item.path;
                    return (
                      <Link
                        key={item.path}
                        to={item.path}
                        className={cn(
                          "group flex items-center justify-between px-4 py-2.5 rounded-xl text-xs font-bold transition-all duration-200",
                          isActive 
                            ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20" 
                            : "hover:bg-slate-800 hover:text-white text-slate-400"
                        )}
                      >
                        <div className="flex items-center gap-3">
                          <item.icon className={cn("w-4 h-4", isActive ? "text-primary-foreground" : "group-hover:text-primary")} />
                          {item.label}
                        </div>
                        {isActive && <ChevronRight className="w-3 h-3" />}
                      </Link>
                    );
                  })}
                </div>
              </div>
            ))}
          </nav>

         
          <div className="p-4 border-t border-slate-800">
            <Button 
              onClick={logout}
              variant="ghost" 
              className="w-full justify-start text-slate-400 hover:text-destructive hover:bg-destructive/10 gap-3 font-bold text-xs uppercase transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span>Sign Out</span>
            </Button>
          </div>
        </aside>

        
        <div className="flex-1 ml-64 flex flex-col min-h-screen">
          
          <div className="h-12 bg-white/50 border-b border-border flex items-center px-8 gap-2 backdrop-blur-sm sticky top-16 z-10">
             <span className="text-[10px] font-black text-muted-foreground uppercase tracking-widest">System</span>
             <ChevronRight className="w-3 h-3 text-border" />
             <span className="text-[10px] font-black text-primary uppercase tracking-widest">
                {currentLabel}
             </span>
          </div>

         
          <main className="flex-1 p-8 bg-background">
            <div className="max-w-7xl mx-auto animate-in fade-in slide-in-from-bottom-2 duration-500">
              <Outlet />
            </div>
          </main>
        </div>
      </div>
    </div>
  );
};