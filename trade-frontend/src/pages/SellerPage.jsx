import { useCallback, useEffect, useState } from "react";
import api, { errorMessage } from "../api/axios";
import { useAuth } from "../context/AuthContext";
import WalletCard from "../components/WalletCard";
import StatusBadge from "../components/StatusBadge";
import { paymentLabel, rupees } from "../utils/format";

const EMPTY_FORM = { name: "", price: "", imageUrl: "" };
const DEMO_MODE = import.meta.env.VITE_DEMO_MODE !== "false";

export default function SellerPage() {
  const { user } = useAuth();
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
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
      setProducts(productRes.data.filter((p) => p.seller === user.username));
      setOrders(orderRes.data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [user.username]);

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

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const addProduct = (e) => {
    e.preventDefault();
    run(async () => {
      await api.post("/api/products", {
        name: form.name.trim(),
        price: Number(form.price),
        imageUrl: form.imageUrl.trim(),
      });
      setForm(EMPTY_FORM);
    }, "Product listed.");
  };

  const agree = (order) => {
    const text =
      `Agreeing locks a ${rupees(order.sellerDeposit)} security deposit from your wallet. ` +
      "You get it back after a successful delivery. Continue?";
    if (!window.confirm(text)) return;
    run(
      () => api.post(`/api/orders/${order.id}/seller-agree`),
      "Order accepted. A delivery agent has been notified."
    );
  };

  const cancelOrder = (order) => {
    const text = order.sellerAgreed
      ? `You already agreed to this order. Cancelling forfeits your ${rupees(
          order.sellerDeposit
        )} deposit (shared with the buyer and the agent) and lowers your reputation. Continue?`
      : "Cancel this order? The buyer is refunded in full and your reputation drops by 0.1. Continue?";
    if (!window.confirm(text)) return;
    run(() => api.post(`/api/orders/${order.id}/seller-cancel`), "Order cancelled.");
  };

    const topUp = () =>
    run(
      () => api.post("/api/wallet/add-money", { amount: 1000 }),
      "Added ₹1,000 of demo money to your wallet."
    );

    const removeProduct = (product) => {
    const text =
      `Remove "${product.name}" from the store? Buyers will no longer see it. ` +
      "Orders already placed are not affected and must be cancelled separately.";
    if (!window.confirm(text)) return;
    run(() => api.delete(`/api/products/${product.id}`), "Product removed from the store.");
  };



  return (
    <div className="page">
      <h1>Seller Dashboard</h1>
      <p className="muted">List products and handle the orders you receive.</p>

      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      <WalletCard refreshKey={walletKey} />

            {DEMO_MODE && (
        <button className="btn btn-outline btn-sm" onClick={topUp} disabled={busy}>
          Add ₹1,000 demo money
        </button>
      )}

      <div className="card">
        <h2>List a new product</h2>
        <form className="form-row" onSubmit={addProduct}>
          <div className="field">
            <label htmlFor="name">Product name</label>
            <input
              id="name"
              name="name"
              value={form.name}
              onChange={handleChange}
              maxLength={150}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="price">Price (₹)</label>
            <input
              id="price"
              name="price"
              type="number"
              min="1"
              step="0.01"
              value={form.price}
              onChange={handleChange}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="imageUrl">Image URL (optional)</label>
            <input
              id="imageUrl"
              name="imageUrl"
              value={form.imageUrl}
              onChange={handleChange}
              maxLength={500}
            />
          </div>
          <div className="field">
            <button className="btn btn-primary" type="submit" disabled={busy}>
              List product
            </button>
          </div>
        </form>
        <p className="muted">
          Your reputation must stay at 5.0 or above to list new products.
        </p>
      </div>

      <div className="card">
        <h2>Your products</h2>
        {loading ? (
          <p className="muted">Loading...</p>
        ) : products.length === 0 ? (
          <p className="muted">You have not listed anything yet.</p>
        ) : (
          <div className="product-grid">
            {products.map((product) => (
              <div className="product" key={product.id}>
                {product.imageUrl && (
                  <img className="product-img" src={product.imageUrl} alt={product.name} />
                )}
                <span className="product-name">{product.name}</span>
                <span className="product-price">{rupees(product.price)}</span>

                                <button
                  className="btn btn-outline btn-sm"
                  disabled={busy}
                  onClick={() => removeProduct(product)}
                >
                  Remove (out of stock)
                </button>
                
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="card">
        <div className="card-header">
          <h2>Orders for your products</h2>
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
                  <th>Buyer</th>
                  <th>Price</th>
                  <th>Payment</th>
                  <th>Your deposit</th>
                  <th>You agreed</th>
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
                    <td>{rupees(order.price)}</td>
                    <td>{paymentLabel(order.paymentMode)}</td>
                    <td>{rupees(order.sellerDeposit)}</td>
                    <td>{order.sellerAgreed ? "Yes" : "Not yet"}</td>
                    <td>
                      <StatusBadge status={order.status} />
                    </td>
                    <td>
                      <div className="actions">
                        {order.status === "NOT_YET_DISPATCHED" && !order.sellerAgreed && (
                          <button
                            className="btn btn-primary btn-sm"
                            disabled={busy}
                            onClick={() => agree(order)}
                          >
                            Agree
                          </button>
                        )}

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
                          <span className="muted">Waiting for buyer</span>
                        )}
                        {order.status === "RETURN_REQUESTED" && (
                          <span className="muted">Return under agent review</span>
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