import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'

type Summary = {
  totalMembers: number
  activeMembers: number
  checkInsToday: number
  insideNow: number
  revenueThisMonth: number | string
  expiringIn7Days: number
  generatedAt: string
}

const money = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
})

function Card({ label, value, hint, to }: { label: string; value: string; hint?: string; to?: string }) {
  const body = (
    <div className="glass h-full p-5 transition hover:border-[#8f9bff]/50">
      <p className="text-xs uppercase tracking-widest text-neutral-400">{label}</p>
      <p className="mt-2 text-3xl font-bold text-white">{value}</p>
      {hint && <p className="mt-1 text-xs text-neutral-400">{hint}</p>}
    </div>
  )
  return to ? <Link to={to} className="block">{body}</Link> : body
}

export default function Dashboard() {
  const q = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => api<Summary>('/dashboard'),
    refetchInterval: 30_000,
  })
  const d = q.data

  return (
    <div className="mx-auto max-w-4xl">
      <div className="mb-4 flex items-baseline justify-between gap-3">
        <h1 className="text-xl font-bold">Dashboard</h1>
        {d && (
          <p className="text-xs text-neutral-400">
            Updated {new Date(d.generatedAt).toLocaleTimeString()}
          </p>
        )}
      </div>

      {q.isLoading && <p className="text-neutral-400">Loading...</p>}
      {q.isError && <p className="text-red-300">Could not load the dashboard.</p>}

      {d && (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
          <Card label="Inside now" value={String(d.insideNow)} hint="Checked in, not yet out" to="/occupancy" />
          <Card label="Check-ins today" value={String(d.checkInsToday)} to="/checkin" />
          <Card label="Active members" value={String(d.activeMembers)} hint={`of ${d.totalMembers} total`} to="/members" />
          <Card label="Expiring in 7 days" value={String(d.expiringIn7Days)} hint="Memberships ending soon" to="/expiring" />
          <Card label="Revenue this month" value={money.format(Number(d.revenueThisMonth))} hint="Paid, refunds excluded" />
        </div>
      )}
    </div>
  )
}