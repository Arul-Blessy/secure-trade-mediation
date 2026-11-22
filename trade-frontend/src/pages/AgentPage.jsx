import { useCallback, useEffect, useState } from "react";
import api, { errorMessage } from "../api/axios";
import WalletCard from "../components/WalletCard";
import StatusBadge from "../components/StatusBadge";
import { paymentLabel, rupees } from "../utils/format";

export default function AgentPage() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [walletKey, setWalletKey] = useState(0);

  const loadData = useCallback(async () => {
    try {
      const { data } = await api.get("/api/agent/notifications");
      setOrders(data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
    const timer = setInterval(loadData, 10000);
    return () => clearInterval(timer);
  }, [loadData]);

  const run = async (request, successText) => {
    setError("");
    setMessage("");
    setBusy(true);
    try {
      await request();
      setMessage(successText);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      await loadData();
      setWalletKey((key) => key + 1);
      setBusy(false);
    }
  };

  const markInTransit = (order) =>
    run(
      () => api.post(`/api/agent/orders/${order.id}/in-transit`),
      `Order #${order.id} marked as in transit.`
    );

  const markDelivered = (order) =>
    run(
      () => api.post(`/api/agent/orders/${order.id}/delivered`),
      `Order #${order.id} marked as delivered.`
    );

  const reviewReturn = (order, decision) => {
    const fee = rupees(Number(order.price) * 0.03);
    const text =
      decision === "REASONABLE"
        ? `Judge this return REASONABLE? The seller is at fault: the buyer is fully refunded, ` +
          `the seller's ${rupees(order.sellerDeposit)} deposit is forfeited and you earn ${fee}. ` +
          "The seller's reputation drops by 0.1."
        : `Judge this return NOT REASONABLE? The buyer is at fault and pays a 10% penalty. ` +
          `The seller gets 7%, you earn ${fee}, and the seller's deposit is released. ` +
          "The buyer's reputation drops by 0.1.";
    if (!window.confirm(text)) return;
    run(
      () => api.post(`/api/agent/orders/${order.id}/review-return`, { decision }),
      `Return for order #${order.id} judged ${decision === "REASONABLE" ? "reasonable" : "not reasonable"}.`
    );
  };

  return (
    <div className="page">
      <h1>Delivery Agent Dashboard</h1>
      <p className="muted">
        Orders waiting for your action. This list updates every 10 seconds.
      </p>

      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}
      {orders.length > 0 && (
        <div className="alert alert-warning">
          Delivery alert: {orders.length} order(s) waiting for your action.
        </div>
      )}

      <WalletCard refreshKey={walletKey} />

      <div className="card">
        <div className="card-header">
          <h2>Work queue</h2>
          <button className="btn btn-outline btn-sm" onClick={loadData} disabled={busy}>
            Refresh
          </button>
        </div>

        {loading ? (
          <p className="muted">Loading...</p>
        ) : orders.length === 0 ? (
          <p className="muted">Nothing to do right now.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Product</th>
                  <th>Buyer</th>
                  <th>Seller</th>
                  <th>Price</th>
                  <th>Payment</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id}>
                    <td>{order.id}</td>
                    <td>{order.productName}</td>
                    <td>{order.buyer}</td>
                    <td>{order.seller}</td>
                    <td>{rupees(order.price)}</td>
                    <td>{paymentLabel(order.paymentMode)}</td>
                    <td>
                      <StatusBadge status={order.status} />
                    </td>
                    <td>
                      <div className="actions">
                        {order.status === "NOT_YET_DISPATCHED" && (
                          <button
                            className="btn btn-primary btn-sm"
                            disabled={busy}
                            onClick={() => markInTransit(order)}
                          >
                            Mark in transit
                          </button>
                        )}

                        {order.status === "IN_TRANSIT" && (
                          <button
                            className="btn btn-primary btn-sm"
                            disabled={busy}
                            onClick={() => markDelivered(order)}
                          >
                            Mark delivered
                          </button>
                        )}

                        {order.status === "RETURN_REQUESTED" && (
                          <>
                            <button
                              className="btn btn-primary btn-sm"
                              disabled={busy}
                              onClick={() => reviewReturn(order, "REASONABLE")}
                            >
                              Reasonable
                            </button>
                            <button
                              className="btn btn-outline btn-sm"
                              disabled={busy}
                              onClick={() => reviewReturn(order, "NOT_REASONABLE")}
                            >
                              Not reasonable
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}