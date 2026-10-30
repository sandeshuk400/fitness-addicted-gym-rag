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
  kind: string;
};

function statusClass(status: string) {
  switch (status) {
    case "PAID":
      return "bg-green-500/20 text-green-300";
    case "REFUNDED":
      return "bg-amber-500/20 text-amber-300";
    default:
      return "bg-white/10 text-neutral-300";
  }
}

export default function Payments() {
  const [rows, setRows] = useState<Payment[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api<Payment[]>("/members/me/payments")
      .then(setRows)
      .catch(() => setError("Could not load payments."));
  }, []);

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-4 text-xl font-bold">My payments</h1>

      {error && <p className="text-red-300">{error}</p>}
      {!error && !rows && <p className="text-neutral-400">Loading...</p>}
      {rows && rows.length === 0 && <p className="text-neutral-400">No payments yet.</p>}

      {rows && rows.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-white/10 bg-forest">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-white/10 text-xs uppercase text-neutral-400">
              <tr>
                <th className="px-4 py-3 font-medium">Receipt</th>
                <th className="px-4 py-3 font-medium">Date</th>
                <th className="px-4 py-3 font-medium">Amount</th>
                <th className="px-4 py-3 font-medium">Method</th>
                <th className="px-4 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((p) => (
                <tr key={p.id} className="border-b border-white/5 last:border-0">
                  <td className="px-4 py-3 font-mono text-xs text-neutral-200">
                    {p.receiptNumber ?? "—"}
                    {p.kind === "JOINING_FEE" && (
                      <p className="font-sans text-xs text-amber-300">Joining fee</p>
                    )}
                  </td>
                  <td className="px-4 py-3 text-neutral-300">
                    {p.paidAt ? new Date(p.paidAt).toLocaleDateString() : "—"}
                  </td>
                  <td className="px-4 py-3 font-semibold text-white">
                    {new Intl.NumberFormat("en-IN", {
                      style: "currency",
                      currency: p.currency,
                    }).format(p.amount)}
                  </td>
                  <td className="px-4 py-3 text-neutral-300">{p.method}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-semibold ${statusClass(p.status)}`}
                    >
                      {p.status}
                    </span>
                    {p.refundReason && (
                      <p className="mt-1 text-xs text-neutral-500">{p.refundReason}</p>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}