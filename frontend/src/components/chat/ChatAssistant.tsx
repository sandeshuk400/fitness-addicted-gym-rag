import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { api } from '../../api/client'

type Message = { role: 'user' | 'assistant'; text: string; sources?: string[] }

export default function ChatAssistant() {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')

  const ask = useMutation({
    mutationFn: (q: string) =>
      api<{ answer: string; sources: string[] }>('/assistant/ask', {
        method: 'POST',
        body: JSON.stringify({ question: q }),
      }),
    onSuccess: (data, variables) => {
      setMessages((prev) => [
        ...prev,
        { role: 'user', text: variables },
        { role: 'assistant', text: data.answer, sources: data.sources },
      ])
      setInput('')
    },
  })

  return (
    <div className="flex h-[500px] flex-col rounded-xl border border-white/10 bg-forest p-5">
      <div className="flex-1 space-y-4 overflow-y-auto">
        {messages.map((m, i) => (
          <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[80%] rounded-lg p-3 ${m.role === 'user' ? 'bg-crimson text-white' : 'bg-white/10 text-neutral-200'}`}>
              <p>{m.text}</p>
              {m.sources && m.sources.length > 0 && (
                <p className="mt-2 text-xs text-neutral-400">Sources: {m.sources.join(', ')}</p>
              )}
            </div>
          </div>
        ))}
        {ask.isPending && <div className="text-sm text-neutral-400">Assistant is thinking...</div>}
      </div>
      <form
        onSubmit={(e) => { e.preventDefault(); if (input.trim()) ask.mutate(input) }}
        className="mt-4 flex gap-2"
      >
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask a question..."
          className="flex-1 rounded-lg border border-white/20 bg-charcoal px-3 py-2 text-sm text-white focus:border-crimson focus:outline-none"
        />
        <button
          disabled={ask.isPending || !input.trim()}
          className="rounded-lg bg-crimson px-4 py-2 text-sm font-semibold text-white hover:bg-crimson-dark disabled:opacity-60"
        >
          Send
        </button>
      </form>
    </div>
  )
}
