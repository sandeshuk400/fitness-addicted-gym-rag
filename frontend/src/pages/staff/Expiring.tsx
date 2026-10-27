import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'

type Row = {
  id: number
  memberId: number
  memberName: string
  planName: string
  endDate: string
  daysRemaining: number
  status: string
}

export default function Expiring() {
  const q = useQuery({ queryKey: ['expiring'], queryFn: () => api<Row[]>('/subscriptions/expiring') })

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-4 text-xl font-bold">Expiring soon</h1>
      {q.isLoading && <p className="text-neutral-400">Loading...</p>}
      {q.isError && <p className="text-red-300">Could not load expiring memberships.</p>}
      {q.data?.length === 0 && <p className="text-neutral-400">No memberships are expiring soon.</p>}

      {!!q.data?.length && (
        <div className="overflow-x-auto rounded-xl border border-white/10 bg-forest">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-white/10 text-xs uppercase text-neutral-400">
              <tr>
                <th className="px-4 py-3 font-medium">Member</th>
                <th className="px-4 py-3 font-medium">Plan</th>
                <th className="px-4 py-3 font-medium">Ends</th>
                <th className="px-4 py-3 font-medium">Days left</th>
              </tr>
            </thead>
            <tbody>
              {q.data.map((r) => (
                <tr key={r.id} className="border-b border-white/5 last:border-0 hover:bg-white/5">
                  <td className="px-4 py-3">
                    <Link to={`/members/${r.memberId}`} className="font-semibold text-white hover:text-crimson">
                      {r.memberName}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-neutral-300">{r.planName}</td>
                  <td className="px-4 py-3 text-neutral-300">{r.endDate}</td>
                  <td className="px-4 py-3 font-semibold text-white">{r.daysRemaining}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}