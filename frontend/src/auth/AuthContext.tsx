import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, loadTokens, login as apiLogin, logout as apiLogout, saveTokens, setSignedOutHandler, type Me } from '../api/client'

interface AuthState {
  me: Me | null
  loading: boolean
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null)
  const [loading, setLoading] = useState(!!loadTokens())
  const queryClient = useQueryClient()

  useEffect(() => {
    setSignedOutHandler(() => {
      setMe(null)
      queryClient.clear()
    })
    if (!loadTokens()) return
    api<Me>('/auth/me')
      .then(setMe)
      .catch(() => setMe(null))
      .finally(() => setLoading(false))
  }, [queryClient])

  const signIn = useCallback(async (email: string, password: string) => {
    const t = await apiLogin(email, password)
    saveTokens({ accessToken: t.accessToken, refreshToken: t.refreshToken })
    setMe(await api<Me>('/auth/me'))
  }, [])

  const signOut = useCallback(async () => {
    await apiLogout()
    saveTokens(null)
    setMe(null)
    queryClient.clear()
  }, [queryClient])

  return <AuthContext.Provider value={{ me, loading, signIn, signOut }}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}