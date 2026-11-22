import { useCallback, useEffect, useState } from "react";
import api, { errorMessage } from "../api/axios";
import WalletCard from "../components/WalletCard";
import StatusBadge from "../components/StatusBadge";
import { paymentLabel, rupees } from "../utils/format";

const DEMO_MODE = import.meta.env.VITE_DEMO_MODE !== "false";

export default function BuyerPage() {
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [modes, setModes] = useState({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [walletKey, setWalletKey] = useState(0);

  const loadData = useCallback(async () => {
    try {
      const [productRes, orderRes] = await Promise.all([
        api.get("/api/products"),
        api.get("/api/orders/mine"),
      ]);
      setProducts(productRes.data);
      setOrders(orderRes.data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Runs one action, shows the result, then refreshes everything on screen.
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

  const buy = (product) => {
    const mode = modes[product.id] || "PREPAID";
    run(
      () => api.post("/api/orders", { productId: product.id, paymentMode: mode }),
      `Order placed for ${product.name}. Waiting for the seller to agree.`
    );
  };

  const cancelOrder = (order) => {
    const penalty = Number(order.price) * 0.1;
    const text = order.sellerAgreed
      ? `The seller has already agreed. Cancelling now costs you a penalty of ${rupees(
          penalty
        )} and lowers your reputation. Continue?`
      : "Cancel this order? You will get a full refund.";
    if (!window.confirm(text)) return;
    run(() => api.post(`/api/orders/${order.id}/buyer-cancel`), "Order cancelled.");
  };

  const confirmReceipt = (order) =>
    run(
      () => api.post(`/api/orders/${order.id}/confirm-receipt`),
      "Receipt confirmed. The seller has been paid."
    );

  const requestReturn = (order) => {
    const text =
      "Request a return? A delivery agent will review it. " +
      "If the return is judged not reasonable, you pay a 10% penalty.";
    if (!window.confirm(text)) return;
    run(
      () => api.post(`/api/orders/${order.id}/return-request`),
      "Return requested. A delivery agent will review it."
    );
  };

  const topUp = () =>
    run(
      () => api.post("/api/wallet/add-money", { amount: 1000 }),
      "Added ₹1,000 of demo money to your wallet."
    );

  return (
    <div className="page">
      <h1>Buyer Dashboard</h1>
      <p className="muted">Browse products, place orders and track them here.</p>

      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      <WalletCard refreshKey={walletKey} />
            {DEMO_MODE && (
        <button className="btn btn-outline btn-sm" onClick={topUp} disabled={busy}>
          Add ₹1,000 demo money
        </button>
      )}

      <div className="card">
        <h2>Products</h2>
        <p className="muted">
          Prepaid locks the full price in escrow. Cash on delivery locks only 10% now.
        </p>

        {loading ? (
          <p className="muted">Loading products...</p>
        ) : products.length === 0 ? (
          <p className="muted">No products yet. Ask a seller to list one.</p>
        ) : (
          <div className="product-grid">
            {products.map((product) => (
              <div className="product" key={product.id}>
                {product.imageUrl && (
                  <img className="product-img" src={product.imageUrl} alt={product.name} />
                )}
                <span className="product-name">{product.name}</span>
                <span className="muted">Sold by {product.seller}</span>
                <span className="product-price">{rupees(product.price)}</span>

                <select
                  value={modes[product.id] || "PREPAID"}
                  onChange={(e) => setModes({ ...modes, [product.id]: e.target.value })}
                  disabled={busy}
                >
                  <option value="PREPAID">Prepaid (lock full price)</option>
                  <option value="COD">Cash on delivery (lock 10%)</option>
                </select>

                <button
                  className="btn btn-primary"
                  onClick={() => buy(product)}
                  disabled={busy}
                >
                  Buy now
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="card">
        <div className="card-header">
          <h2>My Orders</h2>
          <button className="btn btn-outline btn-sm" onClick={loadData} disabled={busy}>
            Refresh
          </button>
        </div>

        {orders.length === 0 ? (
          <p className="muted">No orders yet.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Product</th>
                  <th>Seller</th>
                  <th>Price</th>
                  <th>Payment</th>
                  <th>Locked</th>
                  <th>Seller agreed</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id}>
                    <td>{order.id}</td>
                    <td>{order.productName}</td>
                    <td>{order.seller}</td>
                    <td>{rupees(order.price)}</td>
                    <td>{paymentLabel(order.paymentMode)}</td>
                    <td>{rupees(order.buyerLockedAmount)}</td>
                    <td>{order.sellerAgreed ? "Yes" : "Not yet"}</td>
                    <td>
                      <StatusBadge status={order.status} />
                    </td>
                    <td>
                      <div className="actions">
                        {(order.status === "NOT_YET_DISPATCHED" ||
                          order.status === "IN_TRANSIT") && (
                          <button
                            className="btn btn-danger btn-sm"
                            disabled={busy}
                            onClick={() => cancelOrder(order)}
                          >
                            Cancel
                          </button>
                        )}

                        {order.status === "DELIVERED" && (
                          <>
                            <button
                              className="btn btn-primary btn-sm"
                              disabled={busy}
                              onClick={() => confirmReceipt(order)}
                            >
                              Confirm receipt
                            </button>
                            <button
                              className="btn btn-outline btn-sm"
                              disabled={busy}
                              onClick={() => requestReturn(order)}
                            >
                              Request return
                            </button>
                          </>
                        )}

                        {order.status === "RETURN_REQUESTED" && (
                          <span className="muted">Awaiting agent review</span>
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