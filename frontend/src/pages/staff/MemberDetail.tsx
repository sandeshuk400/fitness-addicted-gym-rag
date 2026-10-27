import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { api, ApiError } from '../../api/client'

type Member = {
  userId: number
  fullName: string
  phone: string | null
  gender: string | null
  joinedOn: string
}
type Subscription = {
  id: number
  planName: string
  startDate: string
  endDate: string
  status: string
  frozenUntil: string | null
  frozenDaysUsed: number
  daysRemaining: number
}
type Payment = {
  id: number
  amount: number
  currency: string
  status: string
  method: string
  receiptNumber: string | null
  paidAt: string | null
}

export default function MemberDetail() {
  const { id } = useParams()
  const queryClient = useQueryClient()
  const [days, setDays] = useState(7)
  const [error, setError] = useState('')

  const member = useQuery({ queryKey: ['member', id], queryFn: () => api<Member>(`/members/${id}`) })
  const subs = useQuery({
    queryKey: ['member-subs', id],
    queryFn: () => api<Subscription[]>(`/members/${id}/subscriptions`),
  })
  const pays = useQuery({
    queryKey: ['member-pays', id],
    queryFn: () => api<Payment[]>(`/members/${id}/payments`),
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['member-subs', id] })
    queryClient.invalidateQueries({ queryKey: ['member-pays', id] })
  }
  const onError = (e: unknown) => setError(e instanceof ApiError && e.message ? e.message : 'Action failed.')

  const freeze = useMutation({
    mutationFn: (v: { subId: number; days: number }) =>
      api(`/subscriptions/${v.subId}/freeze`, { method: 'POST', body: JSON.stringify({ days: v.days }) }),
    onSuccess: () => { setError(''); refresh() },
    onError,
  })
  const unfreeze = useMutation({
    mutationFn: (subId: number) => api(`/subscriptions/${subId}/unfreeze`, { method: 'POST' }),
    onSuccess: () => { setError(''); refresh() },
    onError,
  })
  const busy = freeze.isPending || unfreeze.isPending

  const money = (p: Payment) =>
    new Intl.NumberFormat('en-IN', { style: 'currency', currency: p.currency }).format(p.amount)

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/members" className="text-sm text-neutral-400 hover:text-white">← All members</Link>

      {member.isError && <p className="mt-4 text-red-300">Could not load this member.</p>}
      {member.data && (
        <div className="mb-6 mt-2">
          <h1 className="text-xl font-bold">{member.data.fullName}</h1>
          <p className="text-sm text-neutral-400">
            Phone: {member.data.phone ?? '—'} · Joined {member.data.joinedOn}
          </p>
        </div>
      )}

      {error && (
        <p className="mb-4 rounded-lg border border-red-400/30 bg-red-500/10 p-3 text-sm text-red-300">{error}</p>
      )}

      <h2 className="mb-2 font-semibold">Subscriptions</h2>
      {subs.isLoading && <p className="text-neutral-400">Loading...</p>}
      {subs.data?.length === 0 && <p className="text-neutral-400">No subscriptions yet.</p>}
      <div className="mb-6 space-y-3">
        {subs.data?.map((s) => (
          <div key={s.id} className="rounded-xl border border-white/10 bg-forest p-4">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="font-semibold text-white">{s.planName}</p>
                <p className="text-sm text-neutral-300">{s.startDate} to {s.endDate}</p>
                <p className="mt-1 text-sm text-neutral-400">
                  {s.daysRemaining} days left · freeze days used {s.frozenDaysUsed}
                  {s.frozenUntil ? ` · frozen until ${s.frozenUntil}` : ''}
                </p>
              </div>
              <span className="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-semibold text-neutral-200">
                {s.status}
              </span>
            </div>

            {s.status === 'ACTIVE' && (
              <div className="mt-3 flex items-center gap-3 border-t border-white/10 pt-3 text-sm text-neutral-300">
                Freeze for
                <input
                  type="number"
                  min={1}
                  value={days}
                  onChange={(e) => setDays(Number(e.target.value))}
                  className="w-20 rounded-lg border border-white/20 bg-charcoal px-2 py-1 text-white focus:border-crimson focus:outline-none"
                />
                days
                <button
                  disabled={busy || days < 1}
                  onClick={() => freeze.mutate({ subId: s.id, days })}
                  className="rounded-lg bg-crimson px-4 py-1.5 font-semibold text-white hover:bg-crimson-dark disabled:opacity-60"
                >
                  Freeze
                </button>
              </div>
            )}
            {s.status === 'FROZEN' && (
              <div className="mt-3 border-t border-white/10 pt-3">
                <button
                  disabled={busy}
                  onClick={() => unfreeze.mutate(s.id)}
                  className="rounded-lg bg-crimson px-4 py-1.5 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60"
                >
                  Unfreeze
                </button>
              </div>
            )}
          </div>
        ))}
      </div>

      <h2 className="mb-2 font-semibold">Payments</h2>
      {pays.isLoading && <p className="text-neutral-400">Loading...</p>}
      {pays.data?.length === 0 && <p className="text-neutral-400">No payments yet.</p>}
      {!!pays.data?.length && (
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
              {pays.data.map((p) => (
                <tr key={p.id} className="border-b border-white/5 last:border-0">
                  <td className="px-4 py-3 font-mono text-xs">{p.receiptNumber ?? '—'}</td>
                  <td className="px-4 py-3 text-neutral-300">
                    {p.paidAt ? new Date(p.paidAt).toLocaleDateString() : '—'}
                  </td>
                  <td className="px-4 py-3 font-semibold text-white">{money(p)}</td>
                  <td className="px-4 py-3 text-neutral-300">{p.method}</td>
                  <td className="px-4 py-3 text-neutral-300">{p.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}