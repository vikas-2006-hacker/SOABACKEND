import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { AuthContext } from '../context/auth-context'
import type { AuthContextValue } from '../context/auth-context'
import { academicService } from '../services/academicService'
import { facultyService } from '../services/facultyService'
import { AppRouteContent } from './AppRoutes'

vi.mock('../components/layout/WorkspaceTools', () => ({ WorkspaceTools: () => null }))
vi.mock('../pages/dashboard/DashboardPage', () => ({ DashboardPage: () => <h1>Test dashboard</h1> }))
vi.mock('../services/academicService', () => ({
  academicService: {
    getCourses: vi.fn(),
    getCourseFaculty: vi.fn(),
    listAllEnrollments: vi.fn(),
    getFacultyCourses: vi.fn(),
  },
}))
vi.mock('../services/facultyService', () => ({ facultyService: { list: vi.fn() } }))

function LocationLabel() {
  const location = useLocation()
  return <output data-testid="route-path">{location.pathname}</output>
}

function mount(role: 'ADMIN' | 'STUDENT' | 'FACULTY', initialPath = '/dashboard') {
  const auth: AuthContextValue = {
    user: { email: `${role.toLowerCase()}@example.edu`, role },
    currentStudent: null,
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  }
  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={[initialPath]}>
        <LocationLabel />
        <AppRouteContent />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(academicService.getCourses).mockResolvedValue([])
  vi.mocked(academicService.getCourseFaculty).mockResolvedValue([])
  vi.mocked(academicService.listAllEnrollments).mockResolvedValue([])
  vi.mocked(academicService.getFacultyCourses).mockResolvedValue([])
  vi.mocked(facultyService.list).mockResolvedValue([])
})

afterEach(cleanup)

describe('Admin Courses navigation and authorization', () => {
  it('opens Admin Course Management from the Admin sidebar without navigating to dashboard', async () => {
    const user = userEvent.setup()
    mount('ADMIN')
    expect(await screen.findByRole('heading', { name: 'Test dashboard' })).toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Courses' }))
    expect(await screen.findByRole('heading', { name: 'Course Management' })).toBeInTheDocument()
    expect(screen.getByTestId('route-path')).toHaveTextContent('/admin/courses')
    expect(screen.getByTestId('route-path')).not.toHaveTextContent('/dashboard')
  })

  it.each(['STUDENT', 'FACULTY'] as const)('shows a 403 response to %s at /admin/courses', async (role) => {
    mount(role, '/admin/courses')
    expect(await screen.findByRole('heading', { name: 'Access denied' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('403')
    expect(screen.getByTestId('route-path')).toHaveTextContent('/admin/courses')
    expect(screen.queryByRole('heading', { name: 'Course Management' })).not.toBeInTheDocument()
  })

  it('keeps faculty My Courses separate from the student course catalog', async () => {
    mount('FACULTY', '/my-courses')
    expect(await screen.findByRole('heading', { name: 'My courses' })).toBeInTheDocument()
    expect(screen.getByText('Courses assigned to you for teaching.')).toBeInTheDocument()
    expect(screen.getByTestId('route-path')).toHaveTextContent('/my-courses')
  })
})
