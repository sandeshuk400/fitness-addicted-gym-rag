import { useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../api/client'

type Profile = {
  userId: number
  fullName: string
  phone: string | null
  dateOfBirth: string | null
  gender: string | null
  emergencyContact: string | null
  healthNotes: string | null
  joinedOn: string
}

// GUESS: only M is confirmed. Edit to match the server's allowed values.
const GENDERS = ['M', 'F', 'O']

const input =
  'w-full rounded-lg border border-white/20 bg-charcoal px-3 py-1.5 text-sm text-white focus:border-crimson focus:outline-none'

function ProfileForm({ profile }: { profile: Profile }) {
  const queryClient = useQueryClient()
  const [fullName, setFullName] = useState(profile.fullName)
  const [phone, setPhone] = useState(profile.phone ?? '')
  const [dob, setDob] = useState(profile.dateOfBirth ?? '')
  const [gender, setGender] = useState(profile.gender ?? '')
  const [emergency, setEmergency] = useState(profile.emergencyContact ?? '')
  const [notes, setNotes] = useState(profile.healthNotes ?? '')
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null)

  const save = useMutation({
    mutationFn: () =>
      api<Profile>('/members/me', {
        method: 'PUT',
        body: JSON.stringify({
          fullName: fullName.trim(),
          phone: phone.trim() || null,
          dateOfBirth: dob || null,
          gender: gender || null,
          emergencyContact: emergency.trim() || null,
          healthNotes: notes.trim() || null,
        }),
      }),
    onSuccess: () => {
      setMessage({ ok: true, text: 'Profile saved.' })
      queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
    onError: (e) =>
      setMessage({ ok: false, text: e instanceof ApiError && e.message ? e.message : 'Could not save.' }),
  })

  const label = 'flex flex-col gap-1 text-sm text-neutral-300'

  return (
    <div className="space-y-4 rounded-xl border border-white/10 bg-forest p-5">
      <label className={label}>
        Full name
        <input value={fullName} onChange={(e) => setFullName(e.target.value)} className={input} />
      </label>
      <div className="grid gap-4 sm:grid-cols-2">
        <label className={label}>
          Phone
          <input value={phone} onChange={(e) => setPhone(e.target.value)} className={input} />
        </label>
        <label className={label}>
          Date of birth
          <input type="date" value={dob} onChange={(e) => setDob(e.target.value)} className={input} />
        </label>
        <label className={label}>
          Gender
          <select value={gender} onChange={(e) => setGender(e.target.value)} className={input}>
            <option value="">Not set</option>
            {GENDERS.map((g) => <option key={g}>{g}</option>)}
          </select>
        </label>
        <label className={label}>
          Emergency contact
          <input value={emergency} onChange={(e) => setEmergency(e.target.value)} className={input} />
        </label>
      </div>
      <label className={label}>
        Health notes
        <textarea rows={3} value={notes} onChange={(e) => setNotes(e.target.value)} className={input} />
      </label>

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

      <button
        disabled={save.isPending || !fullName.trim()}
        onClick={() => { setMessage(null); save.mutate() }}
        className="rounded-lg bg-crimson px-5 py-1.5 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60"
      >
        {save.isPending ? 'Saving...' : 'Save'}
      </button>
    </div>
  )
}

type Consent = { purpose: string; granted: boolean; recordedAt: string | null }

/** One optional consent the member can switch on or off at any time. */
function ConsentSection({
  purpose, title, label, children,
}: { purpose: string; title: string; label: string; children: ReactNode }) {
  const queryClient = useQueryClient()
  const [error, setError] = useState('')
  const q = useQuery({ queryKey: ['consents'], queryFn: () => api<Consent[]>('/members/me/consents') })
  const granted = q.data?.find((c) => c.purpose === purpose)?.granted ?? false
  const change = useMutation({
    mutationFn: (g: boolean) =>
      api(`/members/me/consents/${purpose}`, { method: 'PUT', body: JSON.stringify({ granted: g }) }),
    onSuccess: () => { setError(''); queryClient.invalidateQueries({ queryKey: ['consents'] }) },
    onError: (e) => setError(e instanceof ApiError && e.message ? e.message : 'Could not save.'),
  })

  return (
    <div className="mt-6 rounded-xl border border-white/10 bg-forest p-5 text-sm text-neutral-300">
      <h2 className="mb-1 font-semibold text-white">{title}</h2>
      <p className="text-neutral-400">{children}</p>
      {q.isError && <p className="mt-3 text-red-300">Could not load this setting.</p>}
      {q.data && (
        <label className="mt-3 flex items-center gap-2">
          <input
            type="checkbox"
            checked={granted}
            disabled={change.isPending}
            onChange={(e) => change.mutate(e.target.checked)}
          />
          {label}
        </label>
      )}
      {error && <p className="mt-3 text-red-300">{error}</p>}
    </div>
  )
}

export default function ProfilePage() {
  const q = useQuery({ queryKey: ['profile'], queryFn: () => api<Profile>('/members/me') })

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-1 text-xl font-bold">My profile</h1>
      {q.data && <p className="mb-4 text-sm text-neutral-400">Member since {q.data.joinedOn}</p>}
      {q.isLoading && <p className="text-neutral-400">Loading...</p>}
      {q.isError && <p className="text-red-300">Could not load your profile.</p>}
      {q.data && <ProfileForm key={q.data.userId} profile={q.data} />}

      <ConsentSection
        purpose="BIOMETRIC_DOOR"
        title="Fingerprint door access"
        label="I agree to fingerprint door access"
      >
        You can open the gym door with your fingerprint. It is stored on the door device only, not in
        the gym database. If you switch this off, the gym removes your access and deletes your
        fingerprint from the device.
      </ConsentSection>

      <ConsentSection
        purpose="AI_PROCESSING"
        title="AI assistant"
        label="I agree to use the AI assistant"
      >
        The assistant answers questions about plans, prices and gym rules using information the gym has
        provided. Only the text of your question is used. Your questions are not saved, and your health
        notes are never sent to it. You can switch this off at any time.
      </ConsentSection>
    </div>
  )
}