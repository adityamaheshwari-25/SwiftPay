import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  User,
  ShieldAlert,
  LogOut,
  ChevronDown,
  LayoutDashboard,
  UserPlus,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { useEffect, useRef, useState } from "react";
import { SecurityModal } from "./SecurityModal";
import { useAuth } from "@/context/AuthContext";
import { useSecurityStatus } from "@/hooks/queries/useUserQueries";
import { LABELS } from "@/config/labels.config";
import { APP_ROLES } from "@/config/roles.config";

export const Header = ({ user: profileUser, onSecurityAction, hideSecurityAction = false }) => {
  const labels = LABELS.commonComponents.header;
  const [isSecurityOpen, setIsSecurityOpen] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const openTimerRef = useRef(null);
  const closeTimerRef = useRef(null);

  const { data: security, refetch: refetchSecurityStatus } = useSecurityStatus();
  const { user, logout, isAuthenticated, hasRole } = useAuth();
  const displayUser = profileUser || user;

  const location = useLocation();
  const isMerchant = hasRole(APP_ROLES.MERCHANT);
  const isAdmin = hasRole(APP_ROLES.ADMIN);
  const needsAction =
    isAuthenticated && !isAdmin && security && (!security?.mpinSet || security?.kycStatus !== "APPROVED");

  const initials = displayUser?.name?.split(" ").map((n) => n[0]).join("").toUpperCase();

  const navigate = useNavigate();

  const onLogoutClick = () => {
    logout();
    navigate("/");
  };

  const isActive = (path) => location.pathname === path;

  const clearDropdownTimers = () => {
    if (openTimerRef.current) {
      clearTimeout(openTimerRef.current);
      openTimerRef.current = null;
    }
    if (closeTimerRef.current) {
      clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
  };

  const handleProfileMouseEnter = () => {
    if (closeTimerRef.current) {
      clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
    openTimerRef.current = setTimeout(() => {
      setIsDropdownOpen(true);
    }, 150);
  };

  const handleProfileMouseLeave = () => {
    if (openTimerRef.current) {
      clearTimeout(openTimerRef.current);
      openTimerRef.current = null;
    }
    closeTimerRef.current = setTimeout(() => {
      setIsDropdownOpen(false);
    }, 220);
  };

  useEffect(() => {
    return () => clearDropdownTimers();
  }, []);


  return (
    <>
      <header className="sticky top-0 z-50 w-full border-b border-border bg-background/80 backdrop-blur-xl">
        <div className="max-w-7xl w-full flex h-16 items-center justify-between px-6 mx-auto">
          <div className="flex items-center gap-10">
            <Link to="/" className="flex items-center gap-2 group">
              <div className="bg-primary text-primary-foreground p-2 rounded-xl shadow-lg shadow-primary/20 group-hover:scale-105 transition-all duration-300">
                <span className="font-black text-xl italic tracking-tighter">SP</span>
              </div>
              <div className="hidden sm:block">
                <h1 className="font-black text-xl leading-none tracking-tighter text-foreground italic">{labels.brand}</h1>
                <p className="text-[10px] text-primary font-black uppercase tracking-[0.15em] mt-0.5">
                  {isAdmin ? labels.adminConsole : isMerchant ? labels.merchantPro : labels.digitalWallet}
                </p>
              </div>
            </Link>

            {isAuthenticated && !isAdmin && (
              <nav className="hidden md:flex items-center gap-1">
                {isMerchant ? (
                  <>
                    <NavLink to="/merchant/dashboard" label={labels.navOverview} active={isActive("/merchant/dashboard")} />
                    <NavLink to="/merchant/transactions" label={labels.navHistory} active={isActive("/merchant/transactions")} />
                    <NavLink to="/merchant/settlements" label={labels.navSettlements} active={isActive("/merchant/settlements")} />
                  </>
                ) : (
                  <>
                    <NavLink to="/user/dashboard" label={labels.navOverview} active={isActive("/user/dashboard")} />
                    <NavLink to="/user/transactions" label={labels.navMyActivity} active={isActive("/transactions")} />
                  </>
                )}
              </nav>
            )}
          </div>

          <div className="flex items-center gap-3 sm:gap-4">
            {isAuthenticated ? (
              <>
                {needsAction && !hideSecurityAction && !isAdmin && (
                  <Button
                    variant="destructive"
                    size="sm"
                    className="hidden lg:flex h-8 rounded-full text-[10px] font-bold uppercase animate-pulse shadow-lg shadow-destructive/20 hover:animate-none"
                    onClick={onSecurityAction}
                  >
                    <ShieldAlert className="mr-1.5 w-3.5 h-3.5" />
                    {labels.completeSetup}
                  </Button>
                )}

                <div
                  onMouseEnter={handleProfileMouseEnter}
                  onMouseLeave={handleProfileMouseLeave}
                  className="relative"
                >
                  <DropdownMenu open={isDropdownOpen} onOpenChange={setIsDropdownOpen}>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" className="pl-1 pr-2 h-10 rounded-full hover:bg-muted transition-all">
                        <Avatar className="h-8 w-8 border-2 border-primary/10">
                          <AvatarFallback className="bg-primary/5 text-primary text-xs font-bold">{initials}</AvatarFallback>
                        </Avatar>
                        {needsAction && !isAdmin && (
                          <span className="absolute top-5 right-8 h-2.5 w-2.5 rounded-full bg-destructive border-2 border-background" />
                        )}
                        <ChevronDown className="ml-1 w-4 h-4 text-muted-foreground" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                      align="end"
                      sideOffset={8}
                      className="w-72 p-2 shadow-2xl border-border bg-card animate-in fade-in zoom-in-95 duration-200"
                    >
                      <DropdownMenuLabel className="font-normal p-2">
                        <div className="flex flex-col space-y-1">
                          <p className="text-sm font-bold leading-none">{displayUser?.name}</p>
                          <p className="text-xs text-muted-foreground truncate">{displayUser?.email}</p>
                          <p className="text-xs text-muted-foreground truncate">
                            {displayUser?.phoneNumber || "-"}
                          </p>
                          <div className="flex gap-2 mt-2">
                            <Badge variant="outline" className="text-[10px] uppercase font-bold tracking-tighter border-primary/20 text-primary">
                              {displayUser?.role}
                            </Badge>
                            {!isAdmin && (
                              <Badge
                                className={`text-[10px] uppercase font-bold border-none ${
                                  security?.kycStatus === "APPROVED" ? "bg-green-100 text-green-700" : "bg-amber-100 text-amber-700"
                                }`}
                              >
                                {labels.kycPrefix} {security?.kycStatus || labels.kycPending}
                              </Badge>
                            )}
                          </div>
                        </div>
                      </DropdownMenuLabel>
                      <DropdownMenuSeparator className="my-2" />
                      {!isAdmin && (
                        <>
                          <DropdownMenuItem asChild>
                            <Link to={isMerchant ? "/merchant/dashboard" : "/user/dashboard"} className="cursor-pointer hover:bg-primary-foreground">
                              <LayoutDashboard className="mr-2 h-4 w-4 text-slate-400" />
                              <span>{labels.myDashboard}</span>
                            </Link>
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => setIsSecurityOpen(true)} className="cursor-pointer">
                            <ShieldAlert className="mr-2 h-4 w-4 text-destructive" />
                            <span>{labels.securityCenter}</span>
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                        </>
                      )}
                      <DropdownMenuSeparator />
                      <DropdownMenuItem className="text-destructive focus:bg-destructive cursor-pointer" onClick={onLogoutClick}>
                        <LogOut className="mr-2 h-4 w-4" />
                        <span>{labels.signOut}</span>
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </>
            ) : (
              <div className="flex items-center gap-2">
                <Button variant="ghost" className="font-bold text-muted-foreground hover:text-primary" onClick={() => navigate("/login")}>
                  {labels.login}
                </Button>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button className="bg-primary hover:bg-primary/90 font-bold shadow-lg shadow-primary/20">
                      {labels.getStarted} <ChevronDown className="ml-1 w-4 h-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-56 p-2 mt-2">
                    <DropdownMenuItem onClick={() => navigate("/register-user")} className="cursor-pointer py-2">
                      <User className="mr-2 h-4 w-4 text-blue-500" />
                      <div className="flex flex-col">
                        <span className="font-bold text-sm">{labels.customerAccount}</span>
                        <span className="text-[10px] text-muted-foreground">{labels.customerAccountSub}</span>
                      </div>
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => navigate("/register-merchant")} className="cursor-pointer py-2 mt-1">
                      <UserPlus className="mr-2 h-4 w-4 text-green-500" />
                      <div className="flex flex-col">
                        <span className="font-bold text-sm">{labels.merchantAccount}</span>
                        <span className="text-[10px] text-muted-foreground">{labels.merchantAccountSub}</span>
                      </div>
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            )}
          </div>
        </div>
      </header>

      {!isAdmin && isAuthenticated && (
        <SecurityModal
          open={isSecurityOpen}
          onOpenChange={setIsSecurityOpen}
          security={security}
          refreshStatus={refetchSecurityStatus}
        />
      )}
    </>
  );
};

const NavLink = ({ to, label, active }) => (
  <Link
    to={to}
    className={`px-4 py-2 text-sm font-bold transition-all rounded-md tracking-tight ${
      active ? "text-primary bg-primary/5" : "text-muted-foreground hover:text-primary hover:bg-muted/50"
    }`}
  >
    {label}
  </Link>
);
