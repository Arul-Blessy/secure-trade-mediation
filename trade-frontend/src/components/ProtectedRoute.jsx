import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { homePathFor } from "../utils/roles";

export default function ProtectedRoute({ role, children }) {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (role && user.role !== role) {
    return <Navigate to={homePathFor(user.role)} replace />;
  }
  return children;
}