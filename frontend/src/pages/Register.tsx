import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, api } from '../api/client'
import { useAuth } from '../auth/AuthContext'

const PHONE_OK = /^[0-9+\- ]{7,20}$/
const EMAIL_OK = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const input =
  'w-full rounded-lg border border-white/20 bg-charcoal px-3 py-1.5 text-sm text-white focus:border-crimson focus:outline-none'

export default function Register() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [acceptTerms, setAcceptTerms] = useState(false)
  const [marketing, setMarketing] = useState(false)
  const [ai, setAi] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const phoneBad = !!phone.trim() && !PHONE_OK.test(phone.trim())
  const valid =
    !!fullName.trim() && EMAIL_OK.test(email.trim()) && password.length >= 8 && password.length <= 72 &&
    !phoneBad && acceptTerms

  const submit = async () => {
    setBusy(true)
    setError('')
    try {
      await api('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          email: email.trim(),
          password,
          fullName: fullName.trim(),
          phone: phone.trim() || null,
          acceptTerms,
          marketingConsent: marketing,
          aiProcessingConsent: ai,
        }),
      })
    } catch (e) {
      setError(e instanceof ApiError && e.message ? e.message : 'Could not register.')
      setBusy(false)
      return
    }
    try {
      await signIn(email.trim(), password)
      navigate('/', { replace: true })
    } catch {
      navigate('/login', { replace: true })
    }
  }

  const label = 'flex flex-col gap-1 text-sm text-neutral-300'

  return (
    <div className="flex min-h-screen items-center justify-center bg-charcoal p-4 text-neutral-100">
      <div className="w-full max-w-md space-y-4 rounded-2xl border border-white/10 bg-forest p-6">
        <h1 className="text-center text-2xl font-bold">Create your account</h1>

        <label className={label}>
          Full name
          <input value={fullName} maxLength={150} onChange={(e) => setFullName(e.target.value)} className={input} />
        </label>
        <label className={label}>
          Email
          <input type="email" value={email} maxLength={255} onChange={(e) => setEmail(e.target.value)} className={input} />
        </label>
        <label className={label}>
          Phone (optional)
          <input value={phone} onChange={(e) => setPhone(e.target.value)} className={input} />
        </label>
        {phoneBad && <p className="text-sm text-red-300">Phone must be 7-20 digits, spaces, + or -.</p>}
        <label className={label}>
          Password (8 to 72 characters)
          <input type="password" maxLength={72} value={password} onChange={(e) => setPassword(e.target.value)} className={input} />
        </label>

        <div className="space-y-2 text-sm text-neutral-300">
          <label className="flex items-start gap-2">
            <input type="checkbox" checked={acceptTerms} onChange={(e) => setAcceptTerms(e.target.checked)} className="mt-1" />
            I accept the terms (required)
          </label>
          <label className="flex items-start gap-2">
            <input type="checkbox" checked={marketing} onChange={(e) => setMarketing(e.target.checked)} className="mt-1" />
            Send me offers and news (optional)
          </label>
          <label className="flex items-start gap-2">
            <input type="checkbox" checked={ai} onChange={(e) => setAi(e.target.checked)} className="mt-1" />
            Allow AI processing of my data for personalised advice (optional)
          </label>
        </div>

        {error && (
          <p className="rounded-lg border border-red-400/30 bg-red-500/10 p-3 text-sm text-red-300">{error}</p>
        )}

        <button
          disabled={!valid || busy}
          onClick={() => void submit()}
          className="w-full rounded-lg bg-crimson py-2 font-semibold text-white hover:bg-crimson-dark disabled:opacity-60"
        >
          {busy ? 'Creating...' : 'Create account'}
        </button>
        <p className="text-center text-sm text-neutral-400">
          Already have an account? <Link to="/login" className="text-white hover:text-crimson">Sign in</Link>
        </p>
      </div>
    </div>
  )
}