import { useEffect, useState } from "react";
import { api } from "../api/client";

type Payment = {
  id: number;
  subscriptionId: number;
  amount: number;
  currency: string;
  status: string;
  method: string;
  receiptNumber: string | null;
  paidAt: string | null;
  refundedAt: string | null;
  refundReason: string | null;
};

export default function Payments() {
  const [rows, setRows] = useState<Payment[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api<Payment[]>("/members/me/payments")
      .then(setRows)
      .catch(() => setError("Could not load payments."));
  }, []);

  if (error) return <p>{error}</p>;
  if (!rows) return <p>Loading…</p>;
  if (rows.length === 0) return <p>No payments yet.</p>;

  return (
    <div>
      <h2>My payments</h2>
      <table>
        <thead>
          <tr>
            <th>Receipt</th>
            <th>Date</th>
            <th>Amount</th>
            <th>Method</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.id}>
              <td>{p.receiptNumber ?? "—"}</td>
              <td>{p.paidAt ? new Date(p.paidAt).toLocaleDateString() : "—"}</td>
              <td>
                {new Intl.NumberFormat("en-IN", {
                  style: "currency",
                  currency: p.currency,
                }).format(p.amount)}
              </td>
              <td>{p.method}</td>
              <td>
                {p.status}
                {p.refundReason ? ` (${p.refundReason})` : ""}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}