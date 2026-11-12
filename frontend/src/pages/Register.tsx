import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, api } from '../api/client'
import { useAuth } from '../auth/AuthContext'

const PHONE_OK = /^[0-9+\- ]{7,20}$/
const EMAIL_OK = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const input =
  'w-full rounded-lg border border-white/10 bg-charcoal px-3 py-2 text-sm text-white outline-none focus:border-[#8f9bff]'
const label = 'flex flex-col gap-1 text-left text-xs text-neutral-300'

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

  const missing: string[] = []
  if (!fullName.trim()) missing.push('your name')
  if (!EMAIL_OK.test(email.trim())) missing.push('a valid email')
  if (password.length < 8) missing.push('a password of at least 8 characters')
  if (phoneBad) missing.push('a valid phone number')
  if (!acceptTerms) missing.push('the terms checkbox')
  const valid = missing.length === 0

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!valid || busy) return
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
    } catch (err) {
      setError(err instanceof ApiError && err.message ? err.message : 'Could not register.')
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

  return (
    <div className="relative z-10 flex min-h-screen items-center justify-center p-4 text-neutral-100">
      <form onSubmit={submit} className="glass w-full max-w-md space-y-4 p-6 sm:p-8">
        <div className="text-center">
          <p className="text-xs uppercase tracking-widest text-neutral-400">Fitness Addicted Gym</p>
          <h1 className="mt-2 text-xl font-bold text-white">Create your account</h1>
        </div>

        <label className={label} htmlFor="fullName">
          Full name
          <input id="fullName" name="fullName" autoComplete="name" value={fullName} maxLength={150}
            onChange={(e) => setFullName(e.target.value)} className={input} />
        </label>
        <label className={label} htmlFor="email">
          Email
          <input id="email" name="email" type="email" autoComplete="username" value={email} maxLength={255}
            onChange={(e) => setEmail(e.target.value)} className={input} />
        </label>
        <label className={label} htmlFor="phone">
          Phone (optional)
          <input id="phone" name="phone" type="tel" autoComplete="tel" value={phone}
            onChange={(e) => setPhone(e.target.value)} className={input} />
        </label>
        {phoneBad && <p className="text-sm text-red-300">Phone must be 7-20 digits, spaces, + or -.</p>}
        <label className={label} htmlFor="password">
          Password (8 to 72 characters)
          <input id="password" name="password" type="password" autoComplete="new-password" maxLength={72}
            value={password} onChange={(e) => setPassword(e.target.value)} className={input} />
        </label>

        <div className="space-y-2 text-sm text-neutral-300">
          <label className="flex items-start gap-2" htmlFor="acceptTerms">
            <input id="acceptTerms" name="acceptTerms" type="checkbox" checked={acceptTerms}
              onChange={(e) => setAcceptTerms(e.target.checked)} className="mt-1" />
            I accept the terms (required)
          </label>
          <label className="flex items-start gap-2" htmlFor="marketing">
            <input id="marketing" name="marketing" type="checkbox" checked={marketing}
              onChange={(e) => setMarketing(e.target.checked)} className="mt-1" />
            Send me offers and news (optional)
          </label>
          <label className="flex items-start gap-2" htmlFor="ai">
            <input id="ai" name="ai" type="checkbox" checked={ai}
              onChange={(e) => setAi(e.target.checked)} className="mt-1" />
            Allow AI processing of my data for personalised advice (optional)
          </label>
        </div>

        {error && (
          <p role="alert" className="rounded-lg border border-red-400/30 bg-red-500/10 p-3 text-sm text-red-300">{error}</p>
        )}

        {!valid && (
          <p className="text-xs text-neutral-400">Still needed: {missing.join(', ')}.</p>
        )}

        <button type="submit" disabled={!valid || busy}
          className="btn-pill w-full px-6 py-2.5 font-semibold text-white disabled:cursor-not-allowed disabled:opacity-40">
          {busy ? 'Creating...' : 'Create account'}
        </button>
        <p className="text-center text-sm text-neutral-400">
          Already have an account?{' '}
          <Link to="/login" className="text-white underline decoration-[#8f9bff]/60 underline-offset-4 hover:text-[#c9cfff]">Sign in</Link>
        </p>
      </form>
    </div>
  )
}