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

function statusClass(status: string) {
  switch (status) {
    case "ACTIVE":
      return "bg-green-500/20 text-green-300";
    case "FROZEN":
      return "bg-sky-500/20 text-sky-300";
    case "EXPIRED":
    case "CANCELLED":
      return "bg-red-500/20 text-red-300";
    default:
      return "bg-white/10 text-neutral-300";
  }
}

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

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-4 text-xl font-bold">My subscriptions</h1>

      {error && (
        <p className="mb-4 rounded-lg border border-red-400/30 bg-red-500/10 p-3 text-sm text-red-300">
          {error}
        </p>
      )}
      {!rows && !error && <p className="text-neutral-400">Loading...</p>}
      {rows && rows.length === 0 && <p className="text-neutral-400">No subscriptions yet.</p>}

      <div className="space-y-4">
        {rows?.map((s) => {
          const allowance = Math.max(MAX_FREEZE_DAYS - s.frozenDaysUsed, 0);
          const chosen = days[s.id] ?? Math.min(7, allowance || 1);
          return (
            <div key={s.id} className="rounded-xl border border-white/10 bg-forest p-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h2 className="text-lg font-semibold text-white">{s.planName}</h2>
                  <p className="mt-1 text-sm text-neutral-300">
                    {s.startDate} to {s.endDate}
                  </p>
                </div>
                <span
                  className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${statusClass(s.status)}`}
                >
                  {s.status}
                </span>
              </div>

              <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
                <div className="rounded-lg bg-black/20 p-3">
                  <dt className="text-xs uppercase text-neutral-400">Days left</dt>
                  <dd className="mt-1 text-xl font-bold text-white">{s.daysRemaining}</dd>
                </div>
                <div className="rounded-lg bg-black/20 p-3">
                  <dt className="text-xs uppercase text-neutral-400">Freeze days used</dt>
                  <dd className="mt-1 text-xl font-bold text-white">
                    {s.frozenDaysUsed}
                    <span className="text-sm font-normal text-neutral-400"> / {MAX_FREEZE_DAYS}</span>
                  </dd>
                </div>
              </dl>

              {s.frozenUntil && (
                <p className="mt-3 text-sm text-sky-300">Frozen until {s.frozenUntil}</p>
              )}

              {s.status === "ACTIVE" && allowance > 0 && (
                <div className="mt-4 flex flex-wrap items-center gap-3 border-t border-white/10 pt-4">
                  <label className="flex items-center gap-2 text-sm text-neutral-300">
                    Freeze for
                    <input
                      type="number"
                      min={1}
                      max={allowance}
                      value={chosen}
                      onChange={(e) => setDays({ ...days, [s.id]: Number(e.target.value) })}
                      className="w-20 rounded-lg border border-white/20 bg-charcoal px-2 py-1 text-white focus:border-crimson focus:outline-none"
                    />
                    days
                  </label>
                  <button
                    disabled={busy || chosen < 1}
                    onClick={() => freeze(s.id, chosen)}
                    className="rounded-lg bg-crimson px-4 py-1.5 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60"
                  >
                    Freeze
                  </button>
                </div>
              )}

              {s.status === "FROZEN" && (
                <div className="mt-4 border-t border-white/10 pt-4">
                  <button
                    disabled={busy}
                    onClick={() => unfreeze(s.id)}
                    className="rounded-lg bg-crimson px-4 py-1.5 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60"
                  >
                    Unfreeze
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}