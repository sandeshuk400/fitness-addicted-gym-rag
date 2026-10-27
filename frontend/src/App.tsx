import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import type { ReactElement } from 'react'
import { AuthProvider, useAuth } from './auth/AuthContext'
import Login from './pages/Login'
import Notifications from './pages/Notifications'

function RequireAuth({ children }: { children: ReactElement }) {
  const { me, loading } = useAuth()
  if (loading) return <p className="p-6 text-neutral-400">Loading...</p>
  return me ? children : <Navigate to="/login" replace />
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/notifications" element={<RequireAuth><Notifications /></RequireAuth>} />
          <Route path="*" element={<Navigate to="/notifications" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App