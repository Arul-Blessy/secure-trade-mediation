import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import api, { errorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { homePathFor } from "../utils/roles";

export default function Register() {
  const { user, saveSession } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    username: "",
    password: "",
    confirm: "",
    role: "BUYER",
  });
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

    if (form.password.length < 8) {
      setError("Password must be at least 8 characters");
      return;
    }
    if (form.password !== form.confirm) {
      setError("Passwords do not match");
      return;
    }

    setLoading(true);
    try {
      const { data } = await api.post("/api/auth/register", {
        username: form.username,
        password: form.password,
        role: form.role,
      });
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
        <h1>Create Account</h1>
        <p className="muted">Join as a buyer or a seller.</p>

        {error && <div className="alert alert-error">{error}</div>}

        <div className="field">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            name="username"
            value={form.username}
            onChange={handleChange}
            minLength={3}
            maxLength={50}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="role">I want to</label>
          <select id="role" name="role" value={form.role} onChange={handleChange}>
            <option value="BUYER">Buy products (Buyer)</option>
            <option value="SELLER">Sell products (Seller)</option>
          </select>
        </div>

        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            autoComplete="new-password"
            required
          />
        </div>

        <div className="field">
          <label htmlFor="confirm">Confirm password</label>
          <input
            id="confirm"
            name="confirm"
            type="password"
            value={form.confirm}
            onChange={handleChange}
            autoComplete="new-password"
            required
          />
        </div>

        <button className="btn btn-primary btn-block" type="submit" disabled={loading}>
          {loading ? "Creating account..." : "Register"}
        </button>

        <p className="auth-switch">
          Already registered? <Link to="/login">Login</Link>
        </p>
      </form>
    </div>
  );
}