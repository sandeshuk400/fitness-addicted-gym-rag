import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { api, ApiError } from '../../api/client'

// GUESS: only ACTIVE is confirmed. Edit to match the UserStatus enum.
const STATUSES = ['ACTIVE', 'SUSPENDED', 'DISABLED']
const ROLES = ['STAFF', 'ADMIN']

const input =
  'w-full rounded-lg border border-white/20 bg-charcoal px-3 py-1.5 text-sm text-white focus:border-crimson focus:outline-none'
const label = 'flex flex-col gap-1 text-sm text-neutral-300'
const btn =
  'rounded-lg bg-crimson px-5 py-1.5 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60'

type Msg = { ok: boolean; text: string } | null
const Banner = ({ m }: { m: Msg }) =>
  m ? (
    <p className={`rounded-lg border p-3 text-sm ${m.ok ? 'border-green-400/30 bg-green-500/10 text-green-300' : 'border-red-400/30 bg-red-500/10 text-red-300'}`}>
      {m.text}
    </p>
  ) : null
const err = (e: unknown, fallback: string) => (e instanceof ApiError && e.message ? e.message : fallback)

export default function Users() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('STAFF')
  const [createMsg, setCreateMsg] = useState<Msg>(null)

  const [userId, setUserId] = useState('')
  const [status, setStatus] = useState('ACTIVE')
  const [statusMsg, setStatusMsg] = useState<Msg>(null)

  const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())
  const create = useMutation({
    mutationFn: () =>
      api('/admin/users', {
        method: 'POST',
        body: JSON.stringify({ email: email.trim(), password, role }),
      }),
    onSuccess: () => {
      setCreateMsg({ ok: true, text: `Created ${role.toLowerCase()} account for ${email.trim()}.` })
      setEmail(''); setPassword('')
    },
    onError: (e) => setCreateMsg({ ok: false, text: err(e, 'Could not create the account.') }),
  })

  const changeStatus = useMutation({
    mutationFn: () =>
      api(`/admin/users/${userId}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),
    onSuccess: () => setStatusMsg({ ok: true, text: `User ${userId} is now ${status}.` }),
    onError: (e) => setStatusMsg({ ok: false, text: err(e, 'Could not change the status.') }),
  })

  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <section>
        <h1 className="mb-4 text-xl font-bold">Create a staff account</h1>
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
          <Banner m={createMsg} />
          <button
            disabled={!emailOk || password.length < 8 || create.isPending}
            onClick={() => { setCreateMsg(null); create.mutate() }}
            className={btn}
          >
            {create.isPending ? 'Creating...' : 'Create account'}
          </button>
          <p className="text-xs text-neutral-500">Ask the new user to change this password from the Password tab after signing in.</p>
        </div>
      </section>

      <section>
        <h2 className="mb-4 text-xl font-bold">Change a user's status</h2>
        <div className="space-y-4 rounded-xl border border-white/10 bg-forest p-5">
          <div className="grid gap-4 sm:grid-cols-2">
            <label className={label}>
              User ID
              <input type="number" min={1} value={userId} onChange={(e) => setUserId(e.target.value)} className={input} />
            </label>
            <label className={label}>
              New status
              <select value={status} onChange={(e) => setStatus(e.target.value)} className={input}>
                {STATUSES.map((s) => <option key={s}>{s}</option>)}
              </select>
            </label>
          </div>
          <Banner m={statusMsg} />
          <button
            disabled={!userId || changeStatus.isPending}
            onClick={() => {
              if (window.confirm(`Set user ${userId} to ${status}?`)) { setStatusMsg(null); changeStatus.mutate() }
            }}
            className={btn}
          >
            Change status
          </button>
          <p className="text-xs text-neutral-500">There is no user list yet, so you need the user's ID. Members show their ID in the address bar on the Members page (/members/ID).</p>
        </div>
      </section>
    </div>
  )
}