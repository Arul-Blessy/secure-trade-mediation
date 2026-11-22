import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { homePathFor, roleLabel } from "../utils/roles";

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <header className="navbar">
      <Link to={user ? homePathFor(user.role) : "/login"} className="brand">
        Secure Trade
      </Link>
      {user && (
        <div className="nav-right">
          <Link className="nav-link" to={homePathFor(user.role)}>
            Dashboard
          </Link>
          <Link className="nav-link" to="/wallet/history">
            Wallet history
          </Link>
          <span className="nav-user">
            {user.username} <span className="badge">{roleLabel(user.role)}</span>
          </span>
          <button className="btn btn-outline" onClick={handleLogout}>
            Logout
          </button>
        </div>
      )}
    </header>
  );
}