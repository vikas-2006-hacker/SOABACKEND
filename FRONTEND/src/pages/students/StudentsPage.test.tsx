import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../context/auth-context'
import type { AuthContextValue } from '../../context/auth-context'
import { studentService } from '../../services/studentService'
import type { Student } from '../../types/student'
import { StudentsPage } from './StudentsPage'

vi.mock('../../services/studentService', () => ({
  studentService: {
    getStudents: vi.fn(),
    getStudent: vi.fn(),
    getStudentProfile: vi.fn(),
    createStudent: vi.fn(),
    updateStudent: vi.fn(),
    deleteStudent: vi.fn(),
  },
}))

const student: Student = {
  id: 17,
  name: 'Asha Rao',
  email: 'asha@example.edu',
  age: 19,
  department: 'Computer Science',
  courses: 'CS301',
  contact: '555-0117',
  role: 'STUDENT',
  semester: 3,
  academicYear: '2025-2026',
  status: 'ACTIVE',
}

const adminAuth: AuthContextValue = {
  user: { email: 'admin@example.edu', role: 'ADMIN' },
  isAuthenticated: true,
  login: vi.fn(),
  logout: vi.fn(),
}

function renderStudents(auth = adminAuth) {
  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter>
        <StudentsPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

afterEach(() => {
  cleanup()
})

describe('student directory', () => {
  it('loads actual student fields and filters the list locally', async () => {
    vi.mocked(studentService.getStudents).mockResolvedValue([
      student,
      { ...student, id: 18, name: 'Faculty Account', email: 'faculty@example.edu', role: 'FACULTY' },
    ])
    const user = userEvent.setup()
    renderStudents()

    expect(await screen.findByText('Asha Rao')).toBeInTheDocument()
    expect(screen.queryByText('Faculty Account')).not.toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getAllByText('Computer Science').length).toBeGreaterThan(0)
    expect(screen.getByText('Semester 3')).toBeInTheDocument()

    await user.type(screen.getByRole('searchbox', { name: 'Search students' }), 'physics')
    expect(screen.getByText('No matching students')).toBeInTheDocument()
    expect(screen.queryByText('Asha Rao')).not.toBeInTheDocument()
  })

  it('shows an empty state when the service returns no students', async () => {
    vi.mocked(studentService.getStudents).mockResolvedValue([])
    renderStudents()
    expect(await screen.findByText('No students yet')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /add student/i })).toHaveLength(2)
  })

  it('filters by department and status and sorts matching students by name', async () => {
    vi.mocked(studentService.getStudents).mockResolvedValue([
      student,
      { ...student, id: 18, name: 'Zara Chen', email: 'zara@example.edu', department: 'Physics', status: 'ACTIVE' },
      { ...student, id: 19, name: 'Mina Patel', email: 'mina@example.edu', department: 'Computer Science', status: 'INACTIVE' },
    ])
    const user = userEvent.setup()
    renderStudents()
    await screen.findByText('Asha Rao')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Filter by department' }), 'Computer Science')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Filter by status' }), 'INACTIVE')
    expect(screen.getByText('Mina Patel')).toBeInTheDocument()
    expect(screen.queryByText('Asha Rao')).not.toBeInTheDocument()
    await user.selectOptions(screen.getByRole('combobox', { name: 'Sort students' }), 'name-desc')
    expect(screen.getByText('Mina Patel')).toBeInTheDocument()
  })

  it('requires confirmation before deleting and removes a deleted row', async () => {
    vi.mocked(studentService.getStudents).mockResolvedValue([student])
    vi.mocked(studentService.deleteStudent).mockResolvedValue()
    const user = userEvent.setup()
    renderStudents()

    await user.click(await screen.findByRole('button', { name: 'Delete Asha Rao' }))
    expect(screen.getByRole('alertdialog')).toHaveTextContent('This action cannot be undone.')
    await user.click(screen.getByRole('button', { name: 'Delete student' }))

    await waitFor(() => expect(studentService.deleteStudent).toHaveBeenCalledWith(17))
    expect(await screen.findByRole('status')).toHaveTextContent('Student deleted successfully.')
    expect(screen.queryByText('Asha Rao')).not.toBeInTheDocument()
  })

  it('does not show the delete action to faculty', async () => {
    vi.mocked(studentService.getStudents).mockResolvedValue([student])
    const facultyAuth = { ...adminAuth, user: { email: 'faculty@example.edu', role: 'FACULTY' as const } }
    renderStudents(facultyAuth)

    expect(await screen.findByText('Asha Rao')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete Asha Rao' })).not.toBeInTheDocument()
  })
})
