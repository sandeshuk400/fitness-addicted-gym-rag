import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

type Consent = { purpose: string; granted: boolean }
type Reply = { answer: string; sources: string[] }
type Msg = { id: number; role: 'user' | 'assistant' | 'error'; text: string; sources?: string[] }

const input =
  'flex-1 rounded-lg border border-white/20 bg-charcoal px-3 py-2 text-sm text-white focus:border-crimson focus:outline-none'
const btn =
  'rounded-lg bg-crimson px-5 py-2 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60'

const UNAVAILABLE = 'The assistant is not available right now. Please try again later.'

function errorText(e: unknown) {
  // Gateway error pages come back as HTML, so only show short plain messages from the server.
  return e instanceof ApiError && e.message && !e.message.includes('<') ? e.message : UNAVAILABLE
}

export default function Assistant() {
  const { me } = useAuth()
  const isMember = me?.role === 'MEMBER'
  const [question, setQuestion] = useState('')
  const [messages, setMessages] = useState<Msg[]>([])
  const nextId = useRef(1)
  const bottom = useRef<HTMLDivElement>(null)

  // Members must have agreed to AI assistance. Staff and admins are not asked.
  const consents = useQuery({
    queryKey: ['consents'],
    queryFn: () => api<Consent[]>('/members/me/consents'),
    enabled: isMember,
  })
  const consentKnown = !isMember || consents.isSuccess
  const aiAllowed =
    !isMember || (consents.data?.find((c) => c.purpose === 'AI_PROCESSING')?.granted ?? false)

  const add = (m: Omit<Msg, 'id'>) => setMessages((all) => [...all, { id: nextId.current++, ...m }])

  const ask = useMutation({
    mutationFn: (q: string) =>
      api<Reply>('/assistant/ask', { method: 'POST', body: JSON.stringify({ question: q }) }),
    onSuccess: (r) => add({ role: 'assistant', text: r.answer, sources: r.sources }),
    onError: (e) => add({ role: 'error', text: errorText(e) }),
  })

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, ask.isPending])

  function submit(e: FormEvent) {
    e.preventDefault()
    const q = question.trim()
    if (!q || ask.isPending) return
    add({ role: 'user', text: q })
    ask.mutate(q)
    setQuestion('')
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-1 text-xl font-bold">Gym assistant</h1>
      <p className="mb-4 text-sm text-neutral-400">
        Ask about plans, prices, opening hours and gym rules. Answers come only from information the
        gym has provided, and each question is answered on its own. For anything else, ask at the
        front desk.
      </p>

      {isMember && consents.isLoading && <p className="text-neutral-400">Loading...</p>}
      {isMember && consents.isError && (
        <p className="text-red-300">Could not check your AI setting. Please try again.</p>
      )}

      {consentKnown && !aiAllowed && (
        <div className="rounded-xl border border-white/10 bg-forest p-5 text-sm text-neutral-300">
          <p>
            To use the assistant, switch on AI assistance in your profile. Only the text of your
            question is used, and your questions are not saved.
          </p>
          <Link to="/profile" className="mt-3 inline-block text-white underline decoration-[#8f9bff]/60 underline-offset-4">
            Go to my profile
          </Link>
        </div>
      )}

      {consentKnown && aiAllowed && (
        <>
          <div className="max-h-[60vh] min-h-[12rem] space-y-3 overflow-y-auto rounded-xl border border-white/10 bg-forest p-4">
            {messages.length === 0 && !ask.isPending && (
              <p className="text-sm text-neutral-500">
                For example: How long can I freeze my membership? What plans do you have?
              </p>
            )}
            {messages.map((m) => (
              <div
                key={m.id}
                className={
                  m.role === 'user'
                    ? 'ml-auto max-w-[85%] rounded-xl bg-[#8f9bff]/15 px-3 py-2 text-sm text-white'
                    : m.role === 'error'
                      ? 'max-w-[85%] rounded-xl border border-red-400/30 bg-red-500/10 px-3 py-2 text-sm text-red-300'
                      : 'max-w-[85%] rounded-xl bg-white/5 px-3 py-2 text-sm text-neutral-100'
                }
              >
                <p className="whitespace-pre-wrap">{m.text}</p>
                {m.sources && m.sources.length > 0 && (
                  <p className="mt-2 flex flex-wrap items-center gap-1.5 text-xs text-neutral-400">
                    Sources:
                    {m.sources.map((s) => (
                      <span key={s} className="rounded-full bg-white/10 px-2 py-0.5 text-neutral-300">
                        {s}
                      </span>
                    ))}
                  </p>
                )}
              </div>
            ))}
            {ask.isPending && (
              <p className="text-sm text-neutral-400">
                Thinking... the first answer can take up to a minute.
              </p>
            )}
            <div ref={bottom} />
          </div>

          <form onSubmit={submit} className="mt-3 flex gap-2">
            <input
              value={question}
              maxLength={500}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="Ask a question"
              className={input}
            />
            <button type="submit" disabled={ask.isPending || !question.trim()} className={btn}>
              Ask
            </button>
          </form>
        </>
      )}
    </div>
  )
}