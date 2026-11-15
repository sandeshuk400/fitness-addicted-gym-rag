import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'

type Entry = {
  id: number
  scannedAt: string
  deviceUserId: number
  deviceSerial: string
  memberId: number | null
  memberName: string | null
  result: string
}
type Page = { content: Entry[]; totalElements: number }

const SIZE = 25
const LABELS: Record<string, { text: string; cls: string }> = {
  ATTENDANCE_MARKED: { text: 'Attendance marked', cls: 'text-emerald-300' },
  RE_ENTRY: { text: 'Re-entry', cls: 'text-neutral-300' },
  DENIED_NO_MEMBERSHIP: { text: 'Denied: no active membership', cls: 'text-red-300' },
  UNKNOWN_USER: { text: 'Unknown fingerprint', cls: 'text-amber-300' },
}

export default function DoorLog() {
  const [page, setPage] = useState(0)
  const log = useQuery({
    queryKey: ['door-log', page],
    queryFn: () => api<Page>(`/device/log?page=${page}&size=${SIZE}`),
    refetchInterval: 15000,
  })
  const total = log.data?.totalElements ?? 0
  const pages = Math.max(1, Math.ceil(total / SIZE))

  return (
    <div className="mx-auto max-w-4xl">
      <h1 className="mb-1 text-xl font-bold">Door log</h1>
      <p className="mb-4 text-sm text-neutral-400">Every fingerprint scan, newest first. Refreshes every 15 seconds.</p>
      {log.isError && <p className="text-red-300">Could not load the door log.</p>}
      {log.isLoading && <p className="text-neutral-400">Loading...</p>}
      {log.data?.content.length === 0 && <p className="text-neutral-400">No scans yet.</p>}
      {!!log.data?.content.length && (
        <div className="overflow-x-auto rounded-xl border border-white/10 bg-forest">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-white/10 text-xs uppercase text-neutral-400">
              <tr>
                <th className="px-4 py-3 font-medium">Time</th>
                <th className="px-4 py-3 font-medium">Member</th>
                <th className="px-4 py-3 font-medium">Device no.</th>
                <th className="px-4 py-3 font-medium">Result</th>
              </tr>
            </thead>
            <tbody>
              {log.data.content.map((e) => {
                const label = LABELS[e.result] ?? { text: e.result, cls: 'text-neutral-300' }
                return (
                  <tr key={e.id} className="border-b border-white/5 last:border-0">
                    <td className="px-4 py-3 text-neutral-300">{new Date(e.scannedAt).toLocaleString()}</td>
                    <td className="px-4 py-3 text-white">{e.memberName ?? '—'}</td>
                    <td className="px-4 py-3 text-neutral-300">{e.deviceUserId}</td>
                    <td className={`px-4 py-3 ${label.cls}`}>{label.text}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
      <div className="mt-4 flex items-center gap-3 text-sm text-neutral-300">
        <button disabled={page === 0} onClick={() => setPage(page - 1)}
          className="rounded-lg border border-white/20 px-4 py-1.5 disabled:opacity-40">Previous</button>
        <span>Page {page + 1} of {pages}</span>
        <button disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}
          className="rounded-lg border border-white/20 px-4 py-1.5 disabled:opacity-40">Next</button>
      </div>
    </div>
  )
}