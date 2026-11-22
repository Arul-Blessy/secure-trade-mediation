import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import api, { errorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { homePathFor } from "../utils/roles";

const DEMO_MODE = import.meta.env.VITE_DEMO_MODE !== "false";

export default function Login() {
  const { user, saveSession } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (user) {
    return <Navigate to={homePathFor(user.role)} replace />;
  }

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const { data } = await api.post("/api/auth/login", form);
      saveSession(data);
      navigate(homePathFor(data.role), { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrap">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <h1>Login</h1>
        <p className="muted">Welcome back to Secure Trade.</p>

        {DEMO_MODE && (
          <div className="hint">
            <strong>Demo accounts</strong> (password <code>12345678</code>):
            <br />
            Blessy_buyer, Blessy_seller, Blessy_agent
          </div>
        )}

        {error && <div className="alert alert-error">{error}</div>}

        <div className="field">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            name="username"
            value={form.username}
            onChange={handleChange}
            autoComplete="username"
            required
          />
        </div>

        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            autoComplete="current-password"
            required
          />
        </div>

        <button className="btn btn-primary btn-block" type="submit" disabled={loading}>
          {loading ? "Logging in..." : "Login"}
        </button>

        <p className="auth-switch">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </form>
    </div>
  );
}