import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { homePathFor } from "../utils/roles";

export default function NotFound() {
  const { user } = useAuth();

  return (
    <div className="page not-found">
      <h1>404</h1>
      <p className="muted">That page does not exist.</p>
      <Link className="btn btn-primary" to={user ? homePathFor(user.role) : "/login"}>
        Go to {user ? "my dashboard" : "login"}
      </Link>
    </div>
  );
}