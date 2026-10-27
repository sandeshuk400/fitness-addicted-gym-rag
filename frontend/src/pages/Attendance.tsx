import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'

type Attendance = { id: number; memberId: number; checkIn: string; checkOut: string | null; source: string }
type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number }

function duration(a: Attendance) {
  if (!a.checkOut) return 'Still inside'
  const mins = Math.max(
    Math.round((new Date(a.checkOut).getTime() - new Date(a.checkIn).getTime()) / 60000),
    0,
  )
  return mins < 60 ? `${mins} min` : `${Math.floor(mins / 60)} h ${mins % 60} min`
}

export default function Attendance() {
  const [page, setPage] = useState(0)
  const q = useQuery({
    queryKey: ['attendance-mine', page],
    queryFn: () => api<Page<Attendance>>(`/attendance/me?page=${page}&size=20`),
  })

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-4 flex items-baseline justify-between">
        <h1 className="text-xl font-bold">My visits</h1>
        {q.data && <span className="text-sm text-neutral-400">{q.data.totalElements} total</span>}
      </div>

      {q.isLoading && <p className="text-neutral-400">Loading...</p>}
      {q.isError && <p className="text-red-300">Could not load your visits.</p>}
      {q.data?.content.length === 0 && <p className="text-neutral-400">No visits yet.</p>}

      {!!q.data?.content.length && (
        <div className="overflow-x-auto rounded-xl border border-white/10 bg-forest">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-white/10 text-xs uppercase text-neutral-400">
              <tr>
                <th className="px-4 py-3 font-medium">Date</th>
                <th className="px-4 py-3 font-medium">In</th>
                <th className="px-4 py-3 font-medium">Out</th>
                <th className="px-4 py-3 font-medium">Time spent</th>
              </tr>
            </thead>
            <tbody>
              {q.data.content.map((a) => (
                <tr key={a.id} className="border-b border-white/5 last:border-0">
                  <td className="px-4 py-3 text-neutral-200">{new Date(a.checkIn).toLocaleDateString()}</td>
                  <td className="px-4 py-3 text-neutral-300">{new Date(a.checkIn).toLocaleTimeString()}</td>
                  <td className="px-4 py-3 text-neutral-300">
                    {a.checkOut ? new Date(a.checkOut).toLocaleTimeString() : '—'}
                  </td>
                  <td className="px-4 py-3 font-semibold text-white">{duration(a)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {q.data && q.data.totalPages > 1 && (
        <div className="mt-4 flex items-center gap-3 text-sm text-neutral-300">
          <button
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
            className="rounded-lg border border-white/20 px-3 py-1 disabled:opacity-40"
          >
            Previous
          </button>
          <span>Page {page + 1} of {q.data.totalPages}</span>
          <button
            disabled={page + 1 >= q.data.totalPages}
            onClick={() => setPage(page + 1)}
            className="rounded-lg border border-white/20 px-3 py-1 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}