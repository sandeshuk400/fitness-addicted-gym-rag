import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../../api/client'

type Member = { userId: number; fullName: string }
type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
type Attendance = { id: number; memberId: number; checkIn: string; checkOut: string | null; source: string }
type Occupancy = { currentlyInGym: number; asOf: string }

const btn =
  'rounded-lg bg-crimson px-4 py-1.5 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60'
const btnGhost =
  'rounded-lg border border-white/20 px-4 py-1.5 text-sm text-neutral-200 hover:border-crimson disabled:opacity-60'

export default function CheckIn() {
  const queryClient = useQueryClient()
  const [memberId, setMemberId] = useState<number | ''>('')
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null)

  const members = useQuery({
    queryKey: ['members', 'all'],
    queryFn: () => api<Page<Member>>('/members?page=0&size=100'),
  })
  const occupancy = useQuery({
    queryKey: ['occupancy'],
    queryFn: () => api<Occupancy>('/attendance/occupancy'),
    refetchInterval: 30_000,
  })
  const history = useQuery({
    queryKey: ['attendance', memberId],
    queryFn: () => api<Page<Attendance>>(`/attendance/members/${memberId}?page=0&size=10`),
    enabled: memberId !== '',
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['occupancy'] })
    queryClient.invalidateQueries({ queryKey: ['attendance', memberId] })
  }
  const name = members.data?.content.find((m) => m.userId === memberId)?.fullName ?? 'Member'

  const act = (path: string, done: string) =>
    api(path, { method: 'POST', body: JSON.stringify({ memberId }) })
      .then(() => { setMessage({ ok: true, text: `${name} ${done}.` }); refresh() })
      .catch((e) =>
        setMessage({ ok: false, text: e instanceof ApiError && e.message ? e.message : 'Action failed.' }),
      )

  const checkIn = useMutation({ mutationFn: () => act('/attendance/check-in', 'checked in') })
  const checkOut = useMutation({ mutationFn: () => act('/attendance/check-out', 'checked out') })
  const busy = checkIn.isPending || checkOut.isPending

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-4 text-xl font-bold">Check-in</h1>

      <div className="mb-6 rounded-xl border border-white/10 bg-forest p-6">
        <p className="text-xs uppercase text-neutral-400">Currently in the gym</p>
        <p className="mt-1 text-4xl font-bold text-white">{occupancy.data?.currentlyInGym ?? '—'}</p>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-3 rounded-xl border border-white/10 bg-forest p-4">
        <select
          value={memberId}
          onChange={(e) => { setMemberId(e.target.value ? Number(e.target.value) : ''); setMessage(null) }}
          className="rounded-lg border border-white/20 bg-charcoal px-2 py-1.5 text-sm text-white focus:border-crimson focus:outline-none"
        >
          <option value="">Choose a member</option>
          {members.data?.content.map((m) => (
            <option key={m.userId} value={m.userId}>{m.fullName}</option>
          ))}
        </select>
        <button disabled={busy || memberId === ''} onClick={() => checkIn.mutate()} className={btn}>
          Check in
        </button>
        <button disabled={busy || memberId === ''} onClick={() => checkOut.mutate()} className={btnGhost}>
          Check out
        </button>
      </div>

      {message && (
        <p
          className={`mb-4 rounded-lg border p-3 text-sm ${
            message.ok
              ? 'border-green-400/30 bg-green-500/10 text-green-300'
              : 'border-red-400/30 bg-red-500/10 text-red-300'
          }`}
        >
          {message.text}
        </p>
      )}

      {memberId !== '' && (
        <>
          <h2 className="mb-2 font-semibold">Recent visits</h2>
          {history.isLoading && <p className="text-neutral-400">Loading...</p>}
          {history.data?.content.length === 0 && <p className="text-neutral-400">No visits yet.</p>}
          {!!history.data?.content.length && (
            <div className="overflow-x-auto rounded-xl border border-white/10 bg-forest">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-white/10 text-xs uppercase text-neutral-400">
                  <tr>
                    <th className="px-4 py-3 font-medium">In</th>
                    <th className="px-4 py-3 font-medium">Out</th>
                    <th className="px-4 py-3 font-medium">Source</th>
                  </tr>
                </thead>
                <tbody>
                  {history.data.content.map((a) => (
                    <tr key={a.id} className="border-b border-white/5 last:border-0">
                      <td className="px-4 py-3 text-neutral-200">{new Date(a.checkIn).toLocaleString()}</td>
                      <td className="px-4 py-3 text-neutral-300">
                        {a.checkOut ? new Date(a.checkOut).toLocaleString() : 'Still inside'}
                      </td>
                      <td className="px-4 py-3 text-neutral-400">{a.source}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  )
}