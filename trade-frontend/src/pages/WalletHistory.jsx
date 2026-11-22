import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { homePathFor } from "../utils/roles";
import { formatDateTime, reasonLabel, rupees } from "../utils/format";

export default function WalletHistory() {
  const { user } = useAuth();
  const [rows, setRows] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    api
      .get("/api/wallet/transactions")
      .then((response) => {
        if (!cancelled) setRows(response.data);
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const visible = filter === "ALL" ? rows : rows.filter((row) => row.bucket === filter);

  return (
    <div className="page">
      <Link to={homePathFor(user.role)}>&larr; Back to dashboard</Link>
      <h1>Wallet History</h1>
      <p className="muted">
        Every rupee that moved in or out of your wallet. Moving money into escrow shows as
        two lines: money leaving your regular balance and arriving in your escrow balance.
      </p>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <div className="card-header">
          <h2>Transactions</h2>
          <select
            className="filter-select"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          >
            <option value="ALL">All balances</option>
            <option value="REGULAR">Regular balance only</option>
            <option value="ESCROW">Escrow balance only</option>
          </select>
        </div>

        {loading ? (
          <p className="muted">Loading...</p>
        ) : visible.length === 0 ? (
          <p className="muted">No transactions yet.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Order</th>
                  <th>Balance</th>
                  <th>What happened</th>
                  <th>Amount</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((row) => {
                  const positive = Number(row.amount) >= 0;
                  return (
                    <tr key={row.id}>
                      <td>{formatDateTime(row.createdAt)}</td>
                      <td>{row.orderId ? `#${row.orderId}` : "-"}</td>
                      <td>{row.bucket === "ESCROW" ? "Escrow" : "Regular"}</td>
                      <td>{reasonLabel(row.reason)}</td>
                      <td className={positive ? "amount-in" : "amount-out"}>
                        {positive ? "+" : "-"}
                        {rupees(Math.abs(Number(row.amount)))}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}