import { useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  BookOpenCheck,
  CalendarCheck2,
  ContactRound,
  LayoutDashboard,
  LogOut,
  Menu,
  UsersRound,
  UserRoundCog,
  X,
} from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { WorkspaceTools } from './WorkspaceTools'
import type { UserRole } from '../../types/auth'

interface NavigationItem {
  label: string
  path: string
  icon: typeof LayoutDashboard
  roles: UserRole[]
}

const navigation: NavigationItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard, roles: ['ADMIN', 'FACULTY', 'STUDENT'] },
  { label: 'Students', path: '/students', icon: UsersRound, roles: ['ADMIN'] },
  { label: 'Faculty', path: '/faculty', icon: UserRoundCog, roles: ['ADMIN'] },
  { label: 'Courses', path: '/admin/courses', icon: BookOpenCheck, roles: ['ADMIN'] },
  { label: 'Courses', path: '/courses', icon: BookOpenCheck, roles: ['STUDENT'] },
  { label: 'My Courses', path: '/my-courses', icon: BookOpenCheck, roles: ['FACULTY'] },
  { label: 'My Students', path: '/my-students', icon: UsersRound, roles: ['FACULTY'] },
  { label: 'Attendance', path: '/attendance', icon: CalendarCheck2, roles: ['ADMIN', 'FACULTY'] },
  { label: 'Results', path: '/results', icon: BookOpenCheck, roles: ['ADMIN', 'FACULTY'] },
  { label: 'Profile', path: '/profile', icon: ContactRound, roles: ['ADMIN', 'FACULTY'] },
  { label: 'My Courses', path: '/my-courses', icon: BookOpenCheck, roles: ['STUDENT'] },
  { label: 'My Profile', path: '/my-profile', icon: ContactRound, roles: ['STUDENT'] },
  { label: 'My Attendance', path: '/my-attendance', icon: CalendarCheck2, roles: ['STUDENT'] },
  { label: 'My Results', path: '/my-results', icon: BookOpenCheck, roles: ['STUDENT'] },
]

function Brand() {
  return (
    <div className="brand-lockup">
      <div className="brand-mark"><BookOpenCheck size={21} strokeWidth={2.2} /></div>
      <div className="brand-name">
        <strong>CampusCore</strong>
        <span>Smart Academic Management Platform</span>
      </div>
    </div>
  )
}

export function AppShell() {
  const { user, currentStudent, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const activeTitle = navigation.find(
    (item) => location.pathname === item.path || location.pathname.startsWith(`${item.path}/`),
  )?.label ?? 'Dashboard'
  const visibleNavigation = navigation.filter((item) => user && item.roles.includes(user.role))
  const isStudent = user?.role === 'STUDENT'
  const studentName = currentStudent?.name?.trim()
  const displayName = isStudent ? studentName || user.email.split('@')[0] : user?.email.split('@')[0] ?? 'User'
  const studentInitials = studentName?.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
  const initials = isStudent ? studentInitials || 'ST' : displayName.trim().slice(0, 2).toUpperCase() || 'US'

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className={`app-shell${isStudent ? ' student-shell' : ''}`}>
      <button
        className={`sidebar-backdrop${menuOpen ? ' open' : ''}`}
        aria-label="Close navigation"
        onClick={() => setMenuOpen(false)}
      />
      <aside className={`sidebar glass-navigation${menuOpen ? ' open' : ''}`} aria-label="Main navigation">
        <Brand />
        <p className="nav-label">Workspace</p>
        <nav className="nav-list">
          {visibleNavigation.map(({ label, path, icon: Icon }) => (
            <NavLink
              key={path}
              to={path}
              end={path === '/dashboard'}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
              onClick={() => setMenuOpen(false)}
            >
              <Icon size={17} strokeWidth={1.9} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className={`account-card${isStudent ? ' student-account-card' : ''}`}>
            <div className="avatar" role="img" aria-label={`${isStudent ? 'Student' : user?.role.toLowerCase() ?? 'User'} avatar, initials ${initials}`}>{initials}</div>
            <div className="account-meta">
              <strong className={isStudent ? 'student-account-name' : undefined}>{displayName}</strong>
              <span>{isStudent ? 'Role: Student' : user?.role.toLowerCase()}</span>
            </div>
          </div>
          <button className="logout-button" type="button" aria-label="Sign out" onClick={handleLogout}>
            <LogOut size={17} />
            <span>Sign out</span>
          </button>
        </div>
      </aside>
      <main className="main-area">
        <header className="topbar glass-panel">
          <button
            className="mobile-menu-button"
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            {menuOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
          <div>
            <p className="topbar-title">{activeTitle}</p>
            <p className="topbar-subtitle">Smart Academic Management Platform</p>
          </div>
          <div className="topbar-actions">
            {user && <WorkspaceTools role={user.role}/>}
            <div className="topbar-user">
              <span className={isStudent ? 'topbar-student-name' : undefined}>{isStudent ? studentName || 'Student' : user?.email}</span>
              <div className="avatar" role="img" aria-label={`${isStudent ? 'Student' : user?.role.toLowerCase() ?? 'User'} avatar, initials ${initials}`}>{initials}</div>
            </div>
          </div>
        </header>
        <section className="page-content">
          <Outlet />
        </section>
      </main>
    </div>
  )
}
