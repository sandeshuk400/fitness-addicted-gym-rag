import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'

type UserRow = { id: number; email: string; role: string; status: string; createdAt: string }
type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number }

const ROLES = ['STAFF', 'ADMIN']

const input =
  'w-full rounded-lg border border-white/20 bg-charcoal px-3 py-1.5 text-sm text-white focus:border-crimson focus:outline-none'
const label = 'flex flex-col gap-1 text-sm text-neutral-300'
const btn =
  'rounded-lg bg-crimson px-5 py-1.5 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60'
const btnGhost =
  'rounded-lg border border-white/20 px-3 py-1 text-sm text-neutral-200 hover:border-crimson disabled:opacity-60'

const err = (e: unknown, fallback: string) => (e instanceof ApiError && e.message ? e.message : fallback)

export default function Users() {
  const { me } = useAuth()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('STAFF')
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null)

  const list = useQuery({
    queryKey: ['admin-users', page],
    queryFn: () => api<Page<UserRow>>(`/admin/users?page=${page}&size=20`),
  })

  const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())

  const create = useMutation({
    mutationFn: () =>
      api('/admin/users', { method: 'POST', body: JSON.stringify({ email: email.trim(), password, role }) }),
    onSuccess: () => {
      setMessage({ ok: true, text: `Created ${role.toLowerCase()} account for ${email.trim()}.` })
      setEmail('')
      setPassword('')
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
    onError: (e) => setMessage({ ok: false, text: err(e, 'Could not create the account.') }),
  })

  const setStatus = useMutation({
    mutationFn: (v: { id: number; status: string }) =>
      api(`/admin/users/${v.id}/status`, { method: 'PUT', body: JSON.stringify({ status: v.status }) }),
    onSuccess: (_d, v) => {
      setMessage({ ok: true, text: `User ${v.id} is now ${v.status}.` })
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
    onError: (e) => setMessage({ ok: false, text: err(e, 'Could not change the status.') }),
  })

  const toggle = (u: UserRow) => {
    const next = u.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
    if (window.confirm(`Set ${u.email} to ${next}?`)) {
      setMessage(null)
      setStatus.mutate({ id: u.id, status: next })
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-8">
      {message && (
        <p
          className={`rounded-lg border p-3 text-sm ${
            message.ok
              ? 'border-green-400/30 bg-green-500/10 text-green-300'
              : 'border-red-400/30 bg-red-500/10 text-red-300'
          }`}
        >
          {message.text}
        </p>
      )}

      <section>
        <h1 className="mb-4 text-xl font-bold">Users</h1>
        {list.isLoading && <p className="text-neutral-400">Loading...</p>}
        {list.isError && <p className="text-red-300">Could not load users.</p>}
        {!!list.data?.content.length && (
          <div className="overflow-x-auto rounded-xl border border-white/10 bg-forest">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-white/10 text-xs uppercase text-neutral-400">
                <tr>
                  <th className="px-4 py-3 font-medium">ID</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Role</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {list.data.content.map((u) => (
                  <tr key={u.id} className="border-b border-white/5 last:border-0">
                    <td className="px-4 py-3 text-neutral-400">{u.id}</td>
                    <td className="px-4 py-3 text-white">{u.email}</td>
                    <td className="px-4 py-3 text-neutral-300">{u.role}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                          u.status === 'ACTIVE' ? 'bg-green-500/20 text-green-300' : 'bg-red-500/20 text-red-300'
                        }`}
                      >
                        {u.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      {u.id !== me?.id && (
                        <button disabled={setStatus.isPending} onClick={() => toggle(u)} className={btnGhost}>
                          {u.status === 'ACTIVE' ? 'Suspend' : 'Activate'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {list.data && list.data.totalPages > 1 && (
          <div className="mt-4 flex items-center gap-3 text-sm text-neutral-300">
            <button disabled={page === 0} onClick={() => setPage(page - 1)} className="rounded-lg border border-white/20 px-3 py-1 disabled:opacity-40">
              Previous
            </button>
            <span>Page {page + 1} of {list.data.totalPages}</span>
            <button disabled={page + 1 >= list.data.totalPages} onClick={() => setPage(page + 1)} className="rounded-lg border border-white/20 px-3 py-1 disabled:opacity-40">
              Next
            </button>
          </div>
        )}
      </section>

      <section>
        <h2 className="mb-4 text-xl font-bold">Create a staff account</h2>
        <div className="space-y-4 rounded-xl border border-white/10 bg-forest p-5">
          <label className={label}>
            Email
            <input type="email" value={email} maxLength={255} onChange={(e) => setEmail(e.target.value)} className={input} />
          </label>
          <label className={label}>
            Temporary password (8 to 72 characters)
            <input type="password" value={password} maxLength={72} onChange={(e) => setPassword(e.target.value)} className={input} />
          </label>
          <label className={label}>
            Role
            <select value={role} onChange={(e) => setRole(e.target.value)} className={input}>
              {ROLES.map((r) => <option key={r}>{r}</option>)}
            </select>
          </label>
          <button
            disabled={!emailOk || password.length < 8 || create.isPending}
            onClick={() => { setMessage(null); create.mutate() }}
            className={btn}
          >
            {create.isPending ? 'Creating...' : 'Create account'}
          </button>
          <p className="text-xs text-neutral-500">Ask the new user to change this password from the Password tab after signing in.</p>
        </div>
      </section>
    </div>
  )
}