const BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080/api/v1'

export interface Tokens {
  accessToken: string
  refreshToken: string
}

export interface Me {
  id: number
  email: string
  role: 'MEMBER' | 'TRAINER' | 'STAFF' | 'ADMIN'
  status: string
  createdAt: string
}

export interface AppNotification {
  id: number
  type: string
  title: string
  message: string
  daysBefore: number | null
  subscriptionId: number | null
  createdAt: string
  readAt: string | null
  read: boolean
}

const KEY = 'gym.tokens'

export function loadTokens(): Tokens | null {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as Tokens) : null
  } catch {
    return null
  }
}

export function saveTokens(t: Tokens | null) {
  try {
    if (t) localStorage.setItem(KEY, JSON.stringify(t))
    else localStorage.removeItem(KEY)
  } catch {
    /* storage unavailable: session only */
  }
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

let onSignedOut: () => void = () => {}
export function setSignedOutHandler(fn: () => void) {
  onSignedOut = fn
}

async function parseError(res: Response): Promise<ApiError> {
  let detail = res.statusText
  try {
    const body = await res.json()
    detail = body.detail || body.title || detail
  } catch {
    /* no body */
  }
  return new ApiError(res.status, detail)
}

let refreshing: Promise<boolean> | null = null

async function refresh(): Promise<boolean> {
  const t = loadTokens()
  if (!t) return false
  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: t.refreshToken }),
    })
    if (!res.ok) return false
    const next = (await res.json()) as Tokens
    saveTokens({ accessToken: next.accessToken, refreshToken: next.refreshToken })
    return true
  } catch {
    return false
  }
}

export async function api<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const t = loadTokens()
  const headers = new Headers(init.headers)
  if (init.body) headers.set('Content-Type', 'application/json')
  if (t) headers.set('Authorization', `Bearer ${t.accessToken}`)

  const res = await fetch(`${BASE}${path}`, { ...init, headers })

  if (res.status === 401 && retry && t) {
    refreshing = refreshing ?? refresh().finally(() => (refreshing = null))
    if (await refreshing) return api<T>(path, init, false)
    saveTokens(null)
    onSignedOut()
  }
  if (!res.ok) throw await parseError(res)
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T)
}

export async function login(email: string, password: string): Promise<Tokens> {
  const res = await fetch(`${BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!res.ok) throw await parseError(res)
  return (await res.json()) as Tokens
}

export async function logout() {
  const t = loadTokens()
  if (!t) return
  try {
    await fetch(`${BASE}/auth/logout`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: t.refreshToken }),
    })
  } catch {
    /* signing out locally anyway */
  }
}