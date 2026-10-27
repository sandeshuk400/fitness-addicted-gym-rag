import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../../api/client'

type Plan = {
  id: number
  name: string
  description: string | null
  durationDays: number
  price: number
  active: boolean
}

const input =
  'w-full rounded-lg border border-white/20 bg-charcoal px-3 py-1.5 text-sm text-white focus:border-crimson focus:outline-none'
const label = 'flex flex-col gap-1 text-sm text-neutral-300'

function PlanForm({ plan, onDone }: { plan: Plan | null; onDone: () => void }) {
  const queryClient = useQueryClient()
  const [name, setName] = useState(plan?.name ?? '')
  const [description, setDescription] = useState(plan?.description ?? '')
  const [days, setDays] = useState(String(plan?.durationDays ?? 30))
  const [price, setPrice] = useState(String(plan?.price ?? ''))
  const [active, setActive] = useState(plan?.active ?? true)
  const [error, setError] = useState('')

  const d = Number(days)
  const p = Number(price)
  const valid =
    !!name.trim() && Number.isInteger(d) && d >= 1 && d <= 1095 && price !== '' && p >= 0

  const save = useMutation({
    mutationFn: () =>
      api<Plan>(plan ? `/membership-plans/${plan.id}` : '/membership-plans', {
        method: plan ? 'PUT' : 'POST',
        body: JSON.stringify({
          name: name.trim(),
          description: description.trim() || null,
          durationDays: d,
          price: Math.round(p * 100) / 100,
          active,
        }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['plans-all'] })
      queryClient.invalidateQueries({ queryKey: ['plans'] })
      onDone()
    },
    onError: (e) => setError(e instanceof ApiError && e.message ? e.message : 'Could not save the plan.'),
  })

  return (
    <div className="mb-6 space-y-4 rounded-xl border border-white/10 bg-forest p-5">
      <h2 className="font-semibold">{plan ? `Edit ${plan.name}` : 'New plan'}</h2>
      <label className={label}>
        Name
        <input value={name} maxLength={100} onChange={(e) => setName(e.target.value)} className={input} />
      </label>
      <label className={label}>
        Description
        <textarea rows={2} value={description} maxLength={2000} onChange={(e) => setDescription(e.target.value)} className={input} />
      </label>
      <div className="grid gap-4 sm:grid-cols-2">
        <label className={label}>
          Duration (days, 1 to 1095)
          <input type="number" min={1} max={1095} value={days} onChange={(e) => setDays(e.target.value)} className={input} />
        </label>
        <label className={label}>
          Price (INR)
          <input type="number" min={0} step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} className={input} />
        </label>
      </div>
      <label className="flex items-center gap-2 text-sm text-neutral-300">
        <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
        Active (can be sold)
      </label>
      {error && (
        <p className="rounded-lg border border-red-400/30 bg-red-500/10 p-3 text-sm text-red-300">{error}</p>
      )}
      <div className="flex gap-3">
        <button
          disabled={!valid || save.isPending}
          onClick={() => { setError(''); save.mutate() }}
          className="rounded-lg bg-crimson px-5 py-1.5 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60"
        >
          {save.isPending ? 'Saving...' : 'Save'}
        </button>
        <button onClick={onDone} className="rounded-lg border border-white/20 px-5 py-1.5 text-sm text-neutral-200 hover:border-crimson">
          Cancel
        </button>
      </div>
    </div>
  )
}

export default function Plans() {
  // undefined = form closed, null = creating, Plan = editing
  const [editing, setEditing] = useState<Plan | null | undefined>(undefined)
  const q = useQuery({ queryKey: ['plans-all'], queryFn: () => api<Plan[]>('/membership-plans/all') })
  const money = (n: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(n)

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-bold">Membership plans</h1>
        {editing === undefined && (
          <button
            onClick={() => setEditing(null)}
            className="rounded-lg bg-crimson px-4 py-1.5 text-sm font-semibold text-white hover:bg-crimson-dark"
          >
            New plan
          </button>
        )}
      </div>

      {editing !== undefined && (
        <PlanForm key={editing?.id ?? 'new'} plan={editing} onDone={() => setEditing(undefined)} />
      )}

      {q.isLoading && <p className="text-neutral-400">Loading...</p>}
      {q.isError && <p className="text-red-300">Could not load plans.</p>}

      {!!q.data?.length && (
        <div className="overflow-x-auto rounded-xl border border-white/10 bg-forest">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-white/10 text-xs uppercase text-neutral-400">
              <tr>
                <th className="px-4 py-3 font-medium">Plan</th>
                <th className="px-4 py-3 font-medium">Days</th>
                <th className="px-4 py-3 font-medium">Price</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {q.data.map((p) => (
                <tr key={p.id} className="border-b border-white/5 last:border-0">
                  <td className="px-4 py-3">
                    <p className="font-semibold text-white">{p.name}</p>
                    {p.description && <p className="text-xs text-neutral-500">{p.description}</p>}
                  </td>
                  <td className="px-4 py-3 text-neutral-300">{p.durationDays}</td>
                  <td className="px-4 py-3 font-semibold text-white">{money(p.price)}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${p.active ? 'bg-green-500/20 text-green-300' : 'bg-white/10 text-neutral-400'}`}>
                      {p.active ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button onClick={() => setEditing(p)} className="rounded-lg border border-white/20 px-3 py-1 text-neutral-200 hover:border-crimson">
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}