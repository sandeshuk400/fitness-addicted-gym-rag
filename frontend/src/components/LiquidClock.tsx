import { useEffect, useState } from 'react'

// Rounded digit outlines drawn in a 60 x 100 box.
const PATHS: Record<string, string> = {
  '0': 'M30 10 C14 10 10 28 10 50 C10 72 14 90 30 90 C46 90 50 72 50 50 C50 28 46 10 30 10 Z',
  '1': 'M14 30 L34 10 L34 90',
  '2': 'M12 30 C12 18 20 10 30 10 C41 10 48 18 48 30 C48 44 34 54 12 90 L50 90',
  '3': 'M12 16 C18 11 24 10 30 10 C42 10 48 18 48 28 C48 38 40 46 28 47 C42 48 50 56 50 68 C50 82 40 90 28 90 C20 90 14 88 10 84',
  '4': 'M40 90 L40 10 L10 64 L52 64',
  '5': 'M48 10 L16 10 L14 46 C20 42 26 41 30 41 C42 41 50 50 50 66 C50 80 42 90 30 90 C22 90 16 88 10 83',
  '6': 'M44 14 C38 10 34 10 30 10 C16 10 10 30 10 58 C10 78 18 90 30 90 C42 90 50 80 50 66 C50 52 42 44 30 44 C22 44 14 48 10 58',
  '7': 'M10 12 L50 12 L22 90',
  '8': 'M30 10 C18 10 12 18 12 28 C12 38 20 46 30 48 C40 46 48 38 48 28 C48 18 42 10 30 10 Z M30 48 C18 50 10 58 10 70 C10 82 18 90 30 90 C42 90 50 82 50 70 C50 58 42 50 30 48 Z',
  '9': 'M16 86 C22 90 26 90 30 90 C44 90 50 70 50 42 C50 22 42 10 30 10 C18 10 10 20 10 34 C10 48 18 56 30 56 C38 56 46 52 50 42',
}

function Digit({ value }: { value: string }) {
  const d = PATHS[value] ?? PATHS['0']
  return (
    <svg className="tube-digit" viewBox="0 0 60 100" aria-hidden="true">
      <path className="t-glow" d={d} />
      <path className="t-body" d={d} />
      <path className="t-mid" d={d} />
      <path className="t-shine" d={d} />
    </svg>
  )
}

function Colon() {
  return (
    <svg className="tube-colon" viewBox="0 0 20 100" aria-hidden="true">
      <circle className="t-dot" cx="10" cy="34" r="7" />
      <circle className="t-dot" cx="10" cy="66" r="7" />
    </svg>
  )
}

export default function LiquidClock({ className = '' }: { className?: string }) {
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), 1000)
    return () => window.clearInterval(id)
  }, [])

  const pad = (n: number) => String(n).padStart(2, '0')
  const h = pad(now.getHours())
  const m = pad(now.getMinutes())
  const s = pad(now.getSeconds())

  // The key restarts the "settle" animation whenever a digit changes.
  return (
    <div className={`liquid-clock ${className}`} role="img" aria-label="Live clock">
      <Digit key={`h0-${h[0]}`} value={h[0]} />
      <Digit key={`h1-${h[1]}`} value={h[1]} />
      <Colon />
      <Digit key={`m0-${m[0]}`} value={m[0]} />
      <Digit key={`m1-${m[1]}`} value={m[1]} />
      <Colon />
      <Digit key={`s0-${s[0]}`} value={s[0]} />
      <Digit key={`s1-${s[1]}`} value={s[1]} />
    </div>
  )
}
