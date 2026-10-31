import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom'
import type { ReactElement } from 'react'
import { AuthProvider, useAuth } from './auth/AuthContext'
import Login from './pages/Login'
import Notifications from './pages/Notifications'
import Subscriptions from './pages/Subscriptions'
import Payments from './pages/Payments'
import Dashboard from './pages/staff/Dashboard'
import Members from './pages/staff/Members'
import MemberDetail from './pages/staff/MemberDetail'
import Occupancy from './pages/staff/Occupancy'
import Expiring from './pages/staff/Expiring'
import CheckIn from './pages/staff/CheckIn'
import Attendance from './pages/Attendance'
import ProfilePage from './pages/Profile'
import ChangePassword from './pages/ChangePassword'
import Register from './pages/Register'
import Plans from './pages/admin/Plans'
import Users from './pages/admin/Users'
import Starfield from './components/Starfield'

function RequireAuth({ children }: { children: ReactElement }) {
  const { me, loading } = useAuth()
  if (loading) return <p className="relative z-10 p-6 text-neutral-400">Loading...</p>
  return me ? children : <Navigate to="/login" replace />
}

function RequireRole({ staff, children }: { staff: boolean; children: ReactElement }) {
  const { me } = useAuth()
  const isStaff = me?.role === 'ADMIN' || me?.role === 'STAFF'
  return isStaff === staff ? children : <Navigate to="/" replace />
}

function RequireAdmin({ children }: { children: ReactElement }) {
  const { me } = useAuth()
  return me?.role === 'ADMIN' ? children : <Navigate to="/" replace />
}

function Home() {
  const { me } = useAuth()
  const isStaff = me?.role === 'ADMIN' || me?.role === 'STAFF'
  return <Navigate to={isStaff ? '/dashboard' : '/notifications'} replace />
}

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-full px-3.5 py-1.5 text-sm transition ${
    isActive
      ? 'bg-[#8f9bff]/15 text-white ring-1 ring-[#8f9bff]/50'
      : 'text-neutral-400 hover:bg-white/5 hover:text-white'
  }`

function Layout() {
  const { me, signOut } = useAuth()
  const isStaff = me?.role === 'ADMIN' || me?.role === 'STAFF'
  const isAdmin = me?.role === 'ADMIN'
  return (
    <div className="relative z-10 min-h-screen text-neutral-100">
      <nav className="flex items-center gap-2 border-b border-white/10 bg-[#07080d]/80 p-3 backdrop-blur">
        <span className="mr-3 hidden shrink-0 items-center gap-2 text-xs font-bold uppercase tracking-widest sm:flex">
          <span className="h-2.5 w-2.5 rounded-full bg-[#8f9bff] shadow-[0_0_12px_#8f9bff]" />
          Fitness Addicted Gym
        </span>
        <div className="flex min-w-0 flex-1 items-center gap-2 overflow-x-auto whitespace-nowrap [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {isStaff ? (
            <>
              <NavLink to="/dashboard" className={linkClass}>Dashboard</NavLink>
              <NavLink to="/checkin" className={linkClass}>Check-in</NavLink>
              <NavLink to="/members" className={linkClass}>Members</NavLink>
              <NavLink to="/expiring" className={linkClass}>Expiring</NavLink>
              <NavLink to="/occupancy" className={linkClass}>Occupancy</NavLink>
              {isAdmin && (
                <>
                  <NavLink to="/plans" className={linkClass}>Plans</NavLink>
                  <NavLink to="/users" className={linkClass}>Staff</NavLink>
                </>
              )}
            </>
          ) : (
            <>
              <NavLink to="/notifications" className={linkClass}>Notifications</NavLink>
              <NavLink to="/subscriptions" className={linkClass}>Subscriptions</NavLink>
              <NavLink to="/payments" className={linkClass}>Payments</NavLink>
              <NavLink to="/attendance" className={linkClass}>Visits</NavLink>
              <NavLink to="/profile" className={linkClass}>Profile</NavLink>
            </>
          )}
          <NavLink to="/change-password" className={linkClass}>Password</NavLink>
        </div>
        <span className="hidden shrink-0 text-xs text-neutral-400 lg:block">
          {me?.email} ({me?.role.toLowerCase()})
        </span>
        <button onClick={() => void signOut()} className="btn-pill shrink-0 px-4 py-1.5 text-sm text-white">
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
      <Starfield />
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route element={<RequireAuth><Layout /></RequireAuth>}>
            <Route path="/notifications" element={<RequireRole staff={false}><Notifications /></RequireRole>} />
            <Route path="/subscriptions" element={<RequireRole staff={false}><Subscriptions /></RequireRole>} />
            <Route path="/payments" element={<RequireRole staff={false}><Payments /></RequireRole>} />
            <Route path="/attendance" element={<RequireRole staff={false}><Attendance /></RequireRole>} />
            <Route path="/profile" element={<RequireRole staff={false}><ProfilePage /></RequireRole>} />
            <Route path="/dashboard" element={<RequireRole staff><Dashboard /></RequireRole>} />
            <Route path="/checkin" element={<RequireRole staff><CheckIn /></RequireRole>} />
            <Route path="/members" element={<RequireRole staff><Members /></RequireRole>} />
            <Route path="/members/:id" element={<RequireRole staff><MemberDetail /></RequireRole>} />
            <Route path="/expiring" element={<RequireRole staff><Expiring /></RequireRole>} />
            <Route path="/occupancy" element={<RequireRole staff><Occupancy /></RequireRole>} />
            <Route path="/plans" element={<RequireAdmin><Plans /></RequireAdmin>} />
            <Route path="/users" element={<RequireAdmin><Users /></RequireAdmin>} />
            <Route path="/change-password" element={<ChangePassword />} />
          </Route>
          <Route path="*" element={<Home />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App