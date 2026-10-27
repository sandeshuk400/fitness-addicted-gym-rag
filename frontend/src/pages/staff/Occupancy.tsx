import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'

type Occupancy = { currentlyInGym: number; asOf: string }

export default function Occupancy() {
  const q = useQuery({
    queryKey: ['occupancy'],
    queryFn: () => api<Occupancy>('/attendance/occupancy'),
    refetchInterval: 30_000,
  })

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-4 text-xl font-bold">Gym occupancy</h1>
      {q.isLoading && <p className="text-neutral-400">Loading...</p>}
      {q.isError && <p className="text-red-300">Could not load occupancy.</p>}
      {q.data && (
        <div className="rounded-xl border border-white/10 bg-forest p-6">
          <p className="text-xs uppercase text-neutral-400">Currently in the gym</p>
          <p className="mt-1 text-5xl font-bold text-white">{q.data.currentlyInGym}</p>
          <p className="mt-3 text-xs text-neutral-500">
            Updated {new Date(q.data.asOf).toLocaleTimeString()} · refreshes every 30 seconds
          </p>
        </div>
      )}
    </div>
  )
}