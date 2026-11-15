import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'

type Summary = { checkInsToday: number; generatedAt: string }

export default function Occupancy() {
  const q = useQuery({
    queryKey: ['visited-today'],
    queryFn: () => api<Summary>('/dashboard'),
    refetchInterval: 30_000,
  })

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-4 text-xl font-bold">Visited today</h1>
      {q.isLoading && <p className="text-neutral-400">Loading...</p>}
      {q.isError && <p className="text-red-300">Could not load today&apos;s visits.</p>}
      {q.data && (
        <div className="rounded-xl border border-white/10 bg-forest p-6">
          <p className="text-xs uppercase text-neutral-400">Members who have visited today</p>
          <p className="mt-1 text-5xl font-bold text-white">{q.data.checkInsToday}</p>
          <p className="mt-3 text-sm text-neutral-400">
            Each member is counted once per day, however many times they go in and out. Exits are
            not recorded by the door, so the app cannot show who is inside right now.
          </p>
          <p className="mt-3 text-xs text-neutral-500">
            Updated {new Date(q.data.generatedAt).toLocaleTimeString()} · refreshes every 30 seconds
          </p>
        </div>
      )}
    </div>
  )
}