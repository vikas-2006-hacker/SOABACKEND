import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import type { UserRole } from '../types/auth'

interface RoleRouteProps {
  allowedRoles: UserRole[]
  unauthorized?: 'dashboard' | 'forbidden'
}

export function RoleRoute({ allowedRoles, unauthorized = 'dashboard' }: RoleRouteProps) {
  const { user } = useAuth()
  if (!user || !allowedRoles.includes(user.role)) {
    if (unauthorized === 'forbidden') {
      return (
        <main className="access-denied-page" role="alert">
          <span>403</span>
          <h1>Access denied</h1>
          <p>You do not have permission to view this page.</p>
        </main>
      )
    }
    return <Navigate to="/dashboard" replace />
  }
  return <Outlet />
}
