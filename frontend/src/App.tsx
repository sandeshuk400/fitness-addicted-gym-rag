import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom'
import type { ReactElement } from 'react'
import { AuthProvider, useAuth } from './auth/AuthContext'
import Login from './pages/Login'
import Notifications from './pages/Notifications'
import Subscriptions from './pages/Subscriptions'
import Payments from './pages/Payments'

function RequireAuth({ children }: { children: ReactElement }) {
  const { me, loading } = useAuth()
  if (loading) return <p className="p-6 text-neutral-400">Loading...</p>
  return me ? children : <Navigate to="/login" replace />
}

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded px-3 py-1.5 text-sm ${
    isActive ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
  }`

function Layout() {
  const { me, signOut } = useAuth()
  return (
    <div className="min-h-screen bg-charcoal text-neutral-100">
      <nav className="flex items-center gap-2 border-b border-white/10 bg-forest p-3">
        <NavLink to="/notifications" className={linkClass}>Notifications</NavLink>
        <NavLink to="/subscriptions" className={linkClass}>Subscriptions</NavLink>
        <NavLink to="/payments" className={linkClass}>Payments</NavLink>
        <span className="ml-auto text-sm text-neutral-400">{me?.email}</span>
        <button
          onClick={() => void signOut()}
          className="rounded px-3 py-1.5 text-sm text-neutral-300 hover:text-white"
        >
          Sign out
        </button>
      </nav>
      <main className="p-4">
        <Outlet />
      </main>
    </div>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<RequireAuth><Layout /></RequireAuth>}>
            <Route path="/notifications" element={<Notifications />} />
            <Route path="/subscriptions" element={<Subscriptions />} />
            <Route path="/payments" element={<Payments />} />
          </Route>
          <Route path="*" element={<Navigate to="/notifications" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App