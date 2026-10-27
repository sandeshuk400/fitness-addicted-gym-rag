import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { api, ApiError } from '../api/client'

const input =
  'w-full rounded-lg border border-white/20 bg-charcoal px-3 py-1.5 text-sm text-white focus:border-crimson focus:outline-none'

export default function ChangePassword() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null)

  const tooShort = next.length > 0 && (next.length < 8 || next.length > 72)
  const mismatch = confirm.length > 0 && confirm !== next
  const valid = !!current && next.length >= 8 && next.length <= 72 && confirm === next

  const save = useMutation({
    mutationFn: () =>
      api('/auth/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword: current, newPassword: next }),
      }),
    onSuccess: () => {
      setMessage({ ok: true, text: 'Password changed.' })
      setCurrent(''); setNext(''); setConfirm('')
    },
    onError: (e) =>
      setMessage({ ok: false, text: e instanceof ApiError && e.message ? e.message : 'Could not change password.' }),
  })

  return (
    <div className="mx-auto max-w-md">
      <h1 className="mb-4 text-xl font-bold">Change password</h1>
      <div className="space-y-4 rounded-xl border border-white/10 bg-forest p-5 text-sm text-neutral-300">
        <label className="flex flex-col gap-1">
          Current password
          <input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} className={input} />
        </label>
        <label className="flex flex-col gap-1">
          New password
          <input type="password" maxLength={72} value={next} onChange={(e) => setNext(e.target.value)} className={input} />
        </label>
        {tooShort && <p className="text-red-300">Password must be 8 to 72 characters.</p>}
        <label className="flex flex-col gap-1">
          Confirm new password
          <input type="password" maxLength={72} value={confirm} onChange={(e) => setConfirm(e.target.value)} className={input} />
        </label>
        {mismatch && <p className="text-red-300">The two passwords do not match.</p>}

        {message && (
          <p
            className={`rounded-lg border p-3 ${
              message.ok
                ? 'border-green-400/30 bg-green-500/10 text-green-300'
                : 'border-red-400/30 bg-red-500/10 text-red-300'
            }`}
          >
            {message.text}
          </p>
        )}

        <button
          disabled={!valid || save.isPending}
          onClick={() => { setMessage(null); save.mutate() }}
          className="rounded-lg bg-crimson px-5 py-1.5 font-semibold text-white hover:bg-crimson-dark disabled:opacity-60"
        >
          {save.isPending ? 'Saving...' : 'Change password'}
        </button>
      </div>
    </div>
  )
}