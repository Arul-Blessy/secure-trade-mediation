export const rupees = (value) =>
  "₹" +
  Number(value).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

const STATUS_LABELS = {
  NOT_YET_DISPATCHED: "Waiting for dispatch",
  IN_TRANSIT: "In transit",
  DELIVERED: "Delivered",
  COMPLETED: "Completed",
  CANCELLED_BY_BUYER: "Cancelled by buyer",
  CANCELLED_BY_SELLER: "Cancelled by seller",
  RETURN_REQUESTED: "Return requested",
  RETURN_APPROVED: "Return approved",
  RETURN_REJECTED: "Return rejected",
};

export const statusLabel = (status) => STATUS_LABELS[status] || status;

export const paymentLabel = (mode) =>
  mode === "COD" ? "Cash on delivery" : "Prepaid";

const REASON_LABELS = {
  TOP_UP: "Demo top-up",
  ORDER_LOCK: "Money locked for your order",
  SELLER_DEPOSIT_LOCK: "Security deposit locked",
  SELLER_DEPOSIT_RELEASE: "Security deposit returned",
  SALE_PAYMENT: "Payment for a sale",
  COD_LOCK_REFUND: "Cash-on-delivery lock returned",
  SELLER_CANCEL_BUYER_REFUND: "Refund: seller cancelled",
  SELLER_CANCEL_PENALTY_TO_BUYER: "Compensation: seller cancelled",
  SELLER_CANCEL_PENALTY_TO_AGENT: "Agent fee: seller cancelled",
  BUYER_CANCEL_REFUND: "Refund: order cancelled",
  BUYER_CANCEL_BUYER_REFUND: "Refund after cancellation penalty",
  BUYER_CANCEL_PENALTY_TO_SELLER: "Compensation: buyer cancelled",
  BUYER_CANCEL_PENALTY_TO_AGENT: "Agent fee: buyer cancelled",
  RETURN_BUYER_REFUND: "Refund for returned order",
  RETURN_SELLER_PENALTY_TO_BUYER: "Compensation: return approved",
  RETURN_SELLER_PENALTY_TO_AGENT: "Agent fee: return approved",
  RETURN_PENALTY_TO_SELLER: "Compensation: return rejected",
  RETURN_PENALTY_TO_AGENT: "Agent fee: return rejected",
};

export const reasonLabel = (reason) =>
  REASON_LABELS[reason] || reason.toLowerCase().replaceAll("_", " ");

export const formatDateTime = (value) =>
  new Date(value).toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });