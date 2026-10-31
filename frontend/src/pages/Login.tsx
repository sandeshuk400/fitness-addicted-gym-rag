import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import LiquidClock from '../components/LiquidClock'

export default function Login() {
  const { me, signIn } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (me) return <Navigate to="/" replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await signIn(email.trim(), password)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not sign in')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="relative z-10 flex min-h-screen flex-col items-center justify-center gap-10 p-6">
      <LiquidClock />

      <form onSubmit={submit} className="glass w-full max-w-sm p-8 text-center">
        <div className="mx-auto mb-4 flex h-11 w-11 items-center justify-center rounded-full border border-white/15 bg-white/5">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#c9cfff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <rect x="5" y="11" width="14" height="9" rx="2" />
            <path d="M8 11V8a4 4 0 0 1 8 0v3" />
          </svg>
        </div>
        <p className="text-xs uppercase tracking-widest text-neutral-400">Fitness Addicted Gym</p>
        <h1 className="mt-2 mb-6 text-xl font-bold text-white">Sign in to continue</h1>

        <label className="block text-left text-xs text-neutral-300" htmlFor="email">Email</label>
        <input id="email" type="email" required autoComplete="username" value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="mt-1 mb-4 w-full rounded-lg border border-white/10 bg-charcoal px-3 py-2 text-white outline-none focus:border-[#8f9bff]" />

        <label className="block text-left text-xs text-neutral-300" htmlFor="password">Password</label>
        <input id="password" type="password" required autoComplete="current-password" value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="mt-1 mb-4 w-full rounded-lg border border-white/10 bg-charcoal px-3 py-2 text-white outline-none focus:border-[#8f9bff]" />

        {error && <p role="alert" className="mb-4 rounded-lg bg-red-500/10 px-3 py-2 text-left text-sm text-red-200">{error}</p>}

        <button type="submit" disabled={busy}
          className="btn-pill w-full px-6 py-2.5 font-semibold text-white disabled:opacity-60">
          {busy ? 'Signing in...' : 'Sign in'}
        </button>

        <p className="mt-5 text-sm text-neutral-400">
          New here?{' '}
          <Link to="/register" className="text-white underline decoration-[#8f9bff]/60 underline-offset-4 hover:text-[#c9cfff]">Create an account</Link>
        </p>
      </form>
    </div>
  )
}