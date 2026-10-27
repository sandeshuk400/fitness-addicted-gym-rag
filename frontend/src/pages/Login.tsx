import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Login() {
  const { me, signIn } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (me) return <Navigate to="/notifications" replace />

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
    <div className="min-h-screen bg-charcoal flex items-center justify-center p-6">
      <form onSubmit={submit} className="w-full max-w-md rounded-2xl bg-forest border border-white/10 p-8 shadow-xl">
        <h1 className="text-3xl font-bold text-white text-center">Fitness Addicted Gym</h1>
        <p className="mt-2 mb-6 text-center text-neutral-400">Sign in to your account</p>

        <label className="block text-sm text-neutral-300" htmlFor="email">Email</label>
        <input id="email" type="email" required autoComplete="username" value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="mt-1 mb-4 w-full rounded-lg bg-charcoal border border-white/10 px-3 py-2 text-white outline-none focus:border-crimson" />

        <label className="block text-sm text-neutral-300" htmlFor="password">Password</label>
        <input id="password" type="password" required autoComplete="current-password" value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="mt-1 mb-4 w-full rounded-lg bg-charcoal border border-white/10 px-3 py-2 text-white outline-none focus:border-crimson" />

        {error && <p role="alert" className="mb-4 rounded-lg bg-crimson/20 px-3 py-2 text-sm text-red-200">{error}</p>}

        <button type="submit" disabled={busy}
          className="w-full rounded-lg bg-crimson px-6 py-2 font-semibold text-white transition hover:bg-crimson-dark disabled:opacity-60">
          {busy ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </div>
  )
}