import { useEffect, useState } from "react";
import api, { errorMessage } from "../api/axios";
import { rupees } from "../utils/format";

export default function WalletCard({ refreshKey = 0 }) {
  const [wallet, setWallet] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    api
      .get("/api/wallet/me")
      .then((response) => {
        if (!cancelled) {
          setWallet(response.data);
          setError("");
        }
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err));
      });
    return () => {
      cancelled = true;
    };
  }, [refreshKey]);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!wallet) return <div className="card muted">Loading wallet...</div>;

  return (
    <div className="card">
      <h2>My Wallet</h2>
      <div className="stat-grid">
        <div className="stat">
          <span className="stat-label">Regular balance</span>
          <span className="stat-value">{rupees(wallet.regularBalance)}</span>
        </div>
        <div className="stat">
          <span className="stat-label">Escrow (locked)</span>
          <span className="stat-value">{rupees(wallet.escrowBalance)}</span>
        </div>
        <div className="stat">
          <span className="stat-label">Penalties paid</span>
          <span className="stat-value">{rupees(wallet.penaltiesTotal)}</span>
        </div>
        {wallet.role !== "AGENT" && (
          <div className="stat">
            <span className="stat-label">Reputation</span>
            <span className="stat-value">{Number(wallet.reputation).toFixed(1)} / 10</span>
          </div>
        )}
      </div>
    </div>
  );
}