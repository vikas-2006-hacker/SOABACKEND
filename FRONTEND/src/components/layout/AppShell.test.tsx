import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../../context/auth-context'
import type { AuthContextValue } from '../../context/auth-context'
import { AppShell } from './AppShell'

vi.mock('./WorkspaceTools', () => ({ WorkspaceTools: () => null }))
afterEach(cleanup)

const makeAuth = (name: string, role: 'STUDENT' | 'FACULTY' | 'ADMIN' = 'STUDENT', logout = vi.fn()): AuthContextValue => ({
  user: { email: `${role === 'STUDENT' ? 'vikas' : name}@example.edu`, role },
  currentStudent: role === 'STUDENT' ? { id: 17, name, email: 'vikas@example.edu', department: 'CS', courses: 'CS301', semester: 3, academicYear: '2025-2026', status: 'ACTIVE' } : null,
  isAuthenticated: true,
  login: vi.fn(),
  logout,
})

function renderShell(auth: AuthContextValue) {
  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route element={<AppShell />}>
            <Route path="/dashboard" element={<h1>Student dashboard content</h1>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('student app shell profile footer', () => {
  it.each([
    'GANUGULA NAGA VENKATA VIKAS',
    'Mia Li',
    'AnExceptionallyLongUnbrokenStudentDisplayNameThatMustWrapWithoutWideningTheSidebar',
  ])('contains the name, role, fixed avatar and sign-out action for %s', (name) => {
    const logout = vi.fn()
    const { container } = renderShell(makeAuth(name, 'STUDENT', logout))
    const footer = container.querySelector('.student-account-card')
    expect(footer).not.toBeNull()
    const account = within(footer as HTMLElement)
    const initials = name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'ST'
    expect(account.getByText(name)).toHaveClass('student-account-name')
    expect(account.getByText('Role: Student')).toBeInTheDocument()
    expect(account.getByRole('img', { name: `Student avatar, initials ${initials}` })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeVisible()
    expect(screen.getByRole('link', { name: 'My Attendance' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Student dashboard content' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))
    expect(logout).toHaveBeenCalledOnce()
  })

  it('uses a useful initials fallback when a student profile name is blank', () => {
    const { container } = renderShell(makeAuth('   '))
    const footer = container.querySelector('.student-account-card')
    expect(footer).not.toBeNull()
    expect(within(footer as HTMLElement).getByRole('img', { name: 'Student avatar, initials ST' })).toBeInTheDocument()
  })

  it.each(['FACULTY', 'ADMIN'] as const)('leaves the %s profile footer and navigation structure intact', (role) => {
    const { container } = renderShell(makeAuth('Staff Account', role))
    expect(container.querySelector('.student-account-card')).toBeNull()
    expect(screen.getByText('Staff Account')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeInTheDocument()
  })
})
