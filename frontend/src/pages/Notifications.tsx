import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type AppNotification } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export default function Notifications() {
  const { me, signOut } = useAuth()
  const queryClient = useQueryClient()

  const list = useQuery({
    queryKey: ['notifications'],
    queryFn: () => api<AppNotification[]>('/notifications/mine'),
    enabled: me?.role === 'MEMBER',
  })
  const unread = useQuery({
    queryKey: ['unread'],
    queryFn: () => api<{ unread: number }>('/notifications/mine/unread-count'),
    enabled: me?.role === 'MEMBER',
  })
  const markRead = useMutation({
    mutationFn: (id: number) => api<AppNotification>(`/notifications/${id}/read`, { method: 'POST' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      queryClient.invalidateQueries({ queryKey: ['unread'] })
    },
  })

  return (
    <div className="min-h-screen bg-charcoal text-neutral-100">
      <header className="border-b border-white/10 bg-forest">
        <div className="mx-auto flex max-w-3xl items-center justify-between p-4">
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-bold">Notifications</h1>
            {!!unread.data?.unread && (
              <span className="rounded-full bg-crimson px-2 py-0.5 text-xs font-semibold text-white">
                {unread.data.unread} unread
              </span>
            )}
          </div>
          <div className="flex items-center gap-3 text-sm text-neutral-400">
            <span>{me?.email}</span>
            <button onClick={() => signOut()} className="rounded-lg border border-white/20 px-3 py-1 hover:border-crimson">
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-3xl p-4">
        {me?.role !== 'MEMBER' && (
          <p className="rounded-xl bg-forest p-6 text-neutral-300">
            Notifications are for member accounts. The {me?.role.toLowerCase()} area is coming next.
          </p>
        )}
        {list.isLoading && <p className="text-neutral-400">Loading...</p>}
        {list.isError && <p className="text-red-300">Could not load notifications.</p>}
        {list.data?.length === 0 && <p className="text-neutral-400">No notifications yet.</p>}

        <ul className="space-y-3">
          {list.data?.map((n) => (
            <li key={n.id} className={`rounded-xl border p-4 ${n.read ? 'border-white/10 bg-forest/60' : 'border-crimson bg-forest'}`}>
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h2 className="font-semibold text-white">{n.title}</h2>
                  <p className="mt-1 text-sm text-neutral-300">{n.message}</p>
                  <p className="mt-2 text-xs text-neutral-500">{new Date(n.createdAt).toLocaleString()}</p>
                </div>
                {!n.read && (
                  <button onClick={() => markRead.mutate(n.id)} disabled={markRead.isPending}
                    className="shrink-0 rounded-lg bg-crimson px-3 py-1 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60">
                    Mark read
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      </main>
    </div>
  )
}