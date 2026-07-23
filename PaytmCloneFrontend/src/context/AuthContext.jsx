/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useState } from "react";
import { authService } from "../services/api/authService";
import { decodeJWT, isTokenExpired, mapJwtToUser } from "../utils/jwt";
import { ROLE_HOME_PATHS } from "@/config/roles.config";

const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const [roles, setRoles] = useState([]);
  const [rolesLoading, setRolesLoading] = useState(true);

  const loadRoles = async () => {
    try {
      const backendRoles = await authService.getRoles();
      setRoles(backendRoles);
    } catch {
      setRoles([]);
    } finally {
      setRolesLoading(false);
    }
  };

  useEffect(() => {
    const storedToken = localStorage.getItem("authToken");

    if (!storedToken) {
      setRolesLoading(false);
      setLoading(false);
      return;
    }

    const decoded = decodeJWT(storedToken);

    if (!decoded || isTokenExpired(decoded)) {
      authService.logout();
      setRolesLoading(false);
      setLoading(false);
      return;
    }

    setToken(storedToken);
    setUser(mapJwtToUser(decoded));
    loadRoles();
    setLoading(false);
  }, []);

  const login = async (credentials) => {
    const { token } = await authService.login(credentials);

    const decoded = decodeJWT(token);
    if (!decoded || isTokenExpired(decoded)) {
      throw new Error("Invalid or expired token");
    }

    localStorage.setItem("authToken", token);
    setToken(token);
    setUser(mapJwtToUser(decoded));

    return decoded.role;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    setToken(null);
  };

  const isRoleKnown = (role) => roles.includes(String(role || "").toUpperCase());

  const hasRole = (role) => {
    const normalizedRole = String(role || "").toUpperCase();
    const currentRole = String(user?.role || "").toUpperCase();

    if (!normalizedRole) return false;
    if (roles.length > 0 && !isRoleKnown(normalizedRole)) return false;

    return currentRole === normalizedRole;
  };

  const hasAnyRole = (allowedRoles = []) => allowedRoles.some((role) => hasRole(role));

  const getHomeRouteForRole = (role) => {
    const normalizedRole = String(role || "").toUpperCase();
    return ROLE_HOME_PATHS[normalizedRole] || ROLE_HOME_PATHS.USER;
  };

  const value = {
    user,
    token,
    roles,
    loading,
    rolesLoading,
    isAuthenticated: !!user,
    isRoleKnown,
    hasRole,
    hasAnyRole,
    getHomeRouteForRole,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{!loading && children}</AuthContext.Provider>;
};
