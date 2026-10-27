import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom'
import type { ReactElement } from 'react'
import { AuthProvider, useAuth } from './auth/AuthContext'
import Login from './pages/Login'
import Notifications from './pages/Notifications'
import Subscriptions from './pages/Subscriptions'
import Payments from './pages/Payments'
import Members from './pages/staff/Members'
import MemberDetail from './pages/staff/MemberDetail'
import Occupancy from './pages/staff/Occupancy'
import Expiring from './pages/staff/Expiring'
import CheckIn from './pages/staff/CheckIn'

function RequireAuth({ children }: { children: ReactElement }) {
  const { me, loading } = useAuth()
  if (loading) return <p className="p-6 text-neutral-400">Loading...</p>
  return me ? children : <Navigate to="/login" replace />
}

function RequireRole({ staff, children }: { staff: boolean; children: ReactElement }) {
  const { me } = useAuth()
  const isStaff = me?.role === 'ADMIN' || me?.role === 'STAFF'
  return isStaff === staff ? children : <Navigate to="/" replace />
}

function Home() {
  const { me } = useAuth()
  const isStaff = me?.role === 'ADMIN' || me?.role === 'STAFF'
  return <Navigate to={isStaff ? '/members' : '/notifications'} replace />
}

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded px-3 py-1.5 text-sm ${
    isActive ? 'bg-neutral-800 text-white' : 'text-neutral-400 hover:text-white'
  }`

function Layout() {
  const { me, signOut } = useAuth()
  const isStaff = me?.role === 'ADMIN' || me?.role === 'STAFF'
  return (
    <div className="min-h-screen bg-charcoal text-neutral-100">
      <nav className="flex items-center gap-2 border-b border-white/10 bg-forest p-3">
        {isStaff ? (
          <>
            <NavLink to="/checkin" className={linkClass}>Check-in</NavLink>
            <NavLink to="/members" className={linkClass}>Members</NavLink>
            <NavLink to="/expiring" className={linkClass}>Expiring</NavLink>
            <NavLink to="/occupancy" className={linkClass}>Occupancy</NavLink>
          </>
        ) : (
          <>
            <NavLink to="/notifications" className={linkClass}>Notifications</NavLink>
            <NavLink to="/subscriptions" className={linkClass}>Subscriptions</NavLink>
            <NavLink to="/payments" className={linkClass}>Payments</NavLink>
          </>
        )}
        <span className="ml-auto text-sm text-neutral-400">
          {me?.email} ({me?.role.toLowerCase()})
        </span>
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
            <Route path="/notifications" element={<RequireRole staff={false}><Notifications /></RequireRole>} />
            <Route path="/subscriptions" element={<RequireRole staff={false}><Subscriptions /></RequireRole>} />
            <Route path="/payments" element={<RequireRole staff={false}><Payments /></RequireRole>} />
            <Route path="/members" element={<RequireRole staff><Members /></RequireRole>} />
            <Route path="/members/:id" element={<RequireRole staff><MemberDetail /></RequireRole>} />
            <Route path="/expiring" element={<RequireRole staff><Expiring /></RequireRole>} />
            <Route path="/occupancy" element={<RequireRole staff><Occupancy /></RequireRole>} />
            <Route path="/checkin" element={<RequireRole staff><CheckIn /></RequireRole>} />
          </Route>
          <Route path="*" element={<Home />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App