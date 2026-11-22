import { Navigate, Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";
import { useAuth } from "./context/AuthContext";
import { homePathFor } from "./utils/roles";
import Login from "./pages/Login";
import Register from "./pages/Register";
import BuyerPage from "./pages/BuyerPage";
import SellerPage from "./pages/SellerPage";
import AgentPage from "./pages/AgentPage";
import WalletHistory from "./pages/WalletHistory";
import NotFound from "./pages/NotFound";

export default function App() {
  const { user } = useAuth();

  return (
    <>
      <Navbar />
      <Routes>
        <Route
          path="/"
          element={<Navigate to={user ? homePathFor(user.role) : "/login"} replace />}
        />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route
          path="/buyer"
          element={
            <ProtectedRoute role="BUYER">
              <BuyerPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/seller"
          element={
            <ProtectedRoute role="SELLER">
              <SellerPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/agent"
          element={
            <ProtectedRoute role="AGENT">
              <AgentPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/wallet/history"
          element={
            <ProtectedRoute>
              <WalletHistory />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<NotFound />} />
      </Routes>
    </>
  );
}