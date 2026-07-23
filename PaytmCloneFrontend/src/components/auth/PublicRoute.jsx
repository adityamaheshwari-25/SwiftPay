import { useAuth } from "@/context/AuthContext";
import { Navigate } from "react-router-dom";

export const PublicRoute = ({ children }) => {
  const { user, isAuthenticated, loading, getHomeRouteForRole } = useAuth();

  if (loading) return null;

  if (isAuthenticated && user) {
    return <Navigate to={getHomeRouteForRole(user.role)} replace />;
  }

  return children;
};
