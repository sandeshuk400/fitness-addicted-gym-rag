import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'

type Member = {
  userId: number
  fullName: string
  phone: string | null
  gender: string | null
  joinedOn: string
}
type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number }

export default function Members() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const q = useQuery({
    queryKey: ['members', page],
    queryFn: () => api<Page<Member>>(`/members?page=${page}&size=20`),
  })

  const rows = (q.data?.content ?? []).filter((m) =>
    m.fullName.toLowerCase().includes(search.trim().toLowerCase()),
  )

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-4 flex items-center justify-between gap-4">
        <h1 className="text-xl font-bold">Members</h1>
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Filter by name"
          className="w-48 rounded-lg border border-white/20 bg-charcoal px-3 py-1.5 text-sm text-white focus:border-crimson focus:outline-none"
        />
      </div>

      {q.isLoading && <p className="text-neutral-400">Loading...</p>}
      {q.isError && <p className="text-red-300">Could not load members.</p>}
      {q.data && rows.length === 0 && <p className="text-neutral-400">No members found.</p>}

      {rows.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-white/10 bg-forest">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-white/10 text-xs uppercase text-neutral-400">
              <tr>
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="px-4 py-3 font-medium">Phone</th>
                <th className="px-4 py-3 font-medium">Joined</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((m) => (
                <tr key={m.userId} className="border-b border-white/5 last:border-0 hover:bg-white/5">
                  <td className="px-4 py-3">
                    <Link to={`/members/${m.userId}`} className="font-semibold text-white hover:text-crimson">
                      {m.fullName}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-neutral-300">{m.phone ?? '—'}</td>
                  <td className="px-4 py-3 text-neutral-300">{m.joinedOn}</td>
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