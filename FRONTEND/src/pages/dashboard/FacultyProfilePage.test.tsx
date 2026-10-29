import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../context/auth-context'
import type { AuthContextValue } from '../../context/auth-context'
import { facultyService } from '../../services/facultyService'
import type { Faculty } from '../../services/facultyService'
import { studentService } from '../../services/studentService'
import { MyProfilePage } from './StudentPages'

vi.mock('../../services/facultyService', () => ({ facultyService: { getMe: vi.fn() } }))
vi.mock('../../services/studentService', () => ({ studentService: { getCurrentStudent: vi.fn(), getStudentProfile: vi.fn() } }))

const faculty: Faculty = {
  id: 4, name: 'Saketh Anand', email: 'saketh@gmail.com', employeeId: '26000001', department: 'CSE',
  designation: 'Senior Lecturer', phone: '9876543210', status: 'ACTIVE', mfaEnabled: true,
  createdAt: '2025-01-01', updatedAt: '2026-01-01',
}
const auth = (role: 'FACULTY' | 'ADMIN' | 'STUDENT'): AuthContextValue => ({
  user: { email: role === 'ADMIN' ? 'admin@example.edu' : 'saketh@gmail.com', role }, isAuthenticated: true,
  currentStudent: null, currentStudentError: null, login: vi.fn(), logout: vi.fn(),
})
function renderProfile(role: 'FACULTY' | 'ADMIN' | 'STUDENT' = 'FACULTY') {
  return render(<AuthContext.Provider value={auth(role)}><MemoryRouter><MyProfilePage /></MemoryRouter></AuthContext.Provider>)
}

beforeEach(() => vi.clearAllMocks())
afterEach(cleanup)

describe('role specific profile pages', () => {
  it('renders the Faculty API data in a profile hero, information cards, security, and real routes', async () => {
    vi.mocked(facultyService.getMe).mockResolvedValue(faculty)
    renderProfile()
    await waitFor(() => expect(facultyService.getMe).toHaveBeenCalledTimes(1))
    expect(await screen.findByRole('heading', { name: 'My Faculty Profile' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Saketh Anand' })).toBeInTheDocument()
    expect(screen.getByText('SA')).toBeInTheDocument()
    expect(screen.getByText('26000001')).toBeInTheDocument()
    expect(screen.getAllByText('CSE')).toHaveLength(2)
    expect(screen.getByText('Senior Lecturer')).toBeInTheDocument()
    expect(screen.getAllByText('MFA enabled').length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: 'Personal Information' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Professional Information' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Account Security' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Teaching Overview' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /View Students/ })).toHaveAttribute('href', '/students')
    expect(screen.getByRole('link', { name: /Mark Attendance/ })).toHaveAttribute('href', '/attendance')
    expect(screen.getByRole('link', { name: /View Results/ })).toHaveAttribute('href', '/results')
  })

  it('uses graceful fallbacks for optional faculty fields and reflects MFA being off', async () => {
    vi.mocked(facultyService.getMe).mockResolvedValue({ ...faculty, designation: null, phone: null, mfaEnabled: false })
    renderProfile()
    expect(await screen.findByText('MFA setup pending')).toBeInTheDocument()
    expect(screen.getAllByText('Not provided').length).toBeGreaterThanOrEqual(2)
    expect(screen.getByText('Setup pending')).toBeInTheDocument()
  })

  it('shows a profile loading skeleton while the Faculty API is pending', () => {
    vi.mocked(facultyService.getMe).mockReturnValue(new Promise(() => {}))
    renderProfile()
    expect(screen.getByRole('status')).toHaveAccessibleName('Loading faculty profile')
    expect(screen.getByText('Loading faculty profile information')).toBeInTheDocument()
  })

  it('shows a friendly Faculty API error and retries', async () => {
    vi.mocked(facultyService.getMe).mockRejectedValueOnce(new Error('backend secret details')).mockResolvedValueOnce(faculty)
    renderProfile()
    expect(await screen.findByText('Please try again.')).toBeInTheDocument()
    expect(screen.queryByText('backend secret details')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    await waitFor(() => expect(facultyService.getMe).toHaveBeenCalledTimes(2))
    expect(await screen.findByRole('heading', { name: 'Professional Information' })).toBeInTheDocument()
  })

  it('renders an Admin identity from AuthContext without requesting faculty or student records', () => {
    renderProfile('ADMIN')
    expect(screen.getByRole('heading', { name: 'My Administrator Profile' })).toBeInTheDocument()
    expect(screen.getAllByText('admin@example.edu')).toHaveLength(3)
    expect(screen.getByText('ADMIN')).toBeInTheDocument()
    expect(screen.getByText('Not provided by an Admin profile API')).toBeInTheDocument()
    expect(facultyService.getMe).not.toHaveBeenCalled()
    expect(studentService.getCurrentStudent).not.toHaveBeenCalled()
    expect(studentService.getStudentProfile).not.toHaveBeenCalled()
  })
})
