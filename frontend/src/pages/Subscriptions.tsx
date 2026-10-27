import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../api/client";

type Subscription = {
  id: number;
  planName: string;
  startDate: string;
  endDate: string;
  status: string;
  frozenUntil: string | null;
  frozenDaysUsed: number;
  daysRemaining: number;
};

// Matches app.membership.max-freeze-days (server default). The server enforces the real limit.
const MAX_FREEZE_DAYS = 30;

export default function Subscriptions() {
  const [rows, setRows] = useState<Subscription[] | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [days, setDays] = useState<Record<number, number>>({});

  const load = useCallback(() => {
    api<Subscription[]>("/members/me/subscriptions")
      .then(setRows)
      .catch(() => setError("Could not load subscriptions."));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const run = (request: Promise<unknown>, fallback: string) => {
    setBusy(true);
    setError("");
    request
      .then(load)
      .catch((e) => setError(e instanceof ApiError && e.message ? e.message : fallback))
      .finally(() => setBusy(false));
  };

  const freeze = (id: number, n: number) =>
    run(
      api(`/subscriptions/${id}/freeze`, {
        method: "POST",
        body: JSON.stringify({ days: n }),
      }),
      "Freeze failed."
    );

  const unfreeze = (id: number) =>
    run(api(`/subscriptions/${id}/unfreeze`, { method: "POST" }), "Unfreeze failed.");

  if (!rows) return <p>{error || "Loading…"}</p>;
  if (rows.length === 0) return <p>No subscriptions yet.</p>;

  return (
    <div>
      <h2>My subscriptions</h2>
      {error && <p>{error}</p>}
      {rows.map((s) => {
        const allowance = Math.max(MAX_FREEZE_DAYS - s.frozenDaysUsed, 0);
        const chosen = days[s.id] ?? Math.min(7, allowance || 1);
        return (
          <div key={s.id}>
            <h3>
              {s.planName} · {s.status}
            </h3>
            <p>
              {s.startDate} to {s.endDate} · {s.daysRemaining} days left
            </p>
            {s.frozenUntil && <p>Frozen until {s.frozenUntil}</p>}
            <p>
              Freeze days used: {s.frozenDaysUsed} of {MAX_FREEZE_DAYS}
            </p>

            {s.status === "ACTIVE" && allowance > 0 && (
              <p>
                <label>
                  Freeze for{" "}
                  <input
                    type="number"
                    min={1}
                    max={allowance}
                    value={chosen}
                    onChange={(e) =>
                      setDays({ ...days, [s.id]: Number(e.target.value) })
                    }
                  />{" "}
                  days
                </label>{" "}
                <button disabled={busy || chosen < 1} onClick={() => freeze(s.id, chosen)}>
                  Freeze
                </button>
              </p>
            )}

            {s.status === "FROZEN" && (
              <button disabled={busy} onClick={() => unfreeze(s.id)}>
                Unfreeze
              </button>
            )}
          </div>
        );
      })}
    </div>
  );
}