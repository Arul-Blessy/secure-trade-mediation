import { statusLabel } from "../utils/format";

export default function StatusBadge({ status }) {
  return (
    <span className={`status status-${status.toLowerCase()}`}>
      {statusLabel(status)}
    </span>
  );
}