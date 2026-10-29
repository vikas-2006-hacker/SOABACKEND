import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../context/auth-context'
import type { AuthContextValue } from '../../context/auth-context'
import { studentService } from '../../services/studentService'
import type { Student } from '../../types/student'
import { StudentFormPage } from './StudentFormPage'
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

const savedStudent: Student = {
  id: 21,
  name: 'Mira Sen',
  email: 'mira@example.edu',
  age: 20,
  department: 'Mathematics',
  courses: 'MTH204',
  contact: '555-0121',
  role: 'STUDENT',
  semester: 4,
  academicYear: '2025-2026',
  status: 'ACTIVE',
}

function renderStudentRoutes(initialPath: string) {
  const auth: AuthContextValue = {
    user: { email: 'admin@example.edu', role: 'ADMIN' },
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  }
  return render(
    <AuthContext.Provider value={auth}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/students/add" element={<StudentFormPage />} />
          <Route path="/students/:id/edit" element={<StudentFormPage />} />
          <Route path="/students" element={<StudentsPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

afterEach(() => {
  cleanup()
})

describe('student form', () => {
  it('validates the backend minimum age before submitting', async () => {
    const user = userEvent.setup()
    renderStudentRoutes('/students/add')

    await user.type(screen.getByLabelText('Full name'), 'Mira Sen')
    await user.type(screen.getByLabelText('Email address'), 'mira@example.edu')
    await user.clear(screen.getByLabelText('Age'))
    await user.type(screen.getByLabelText('Age'), '14')
    await user.type(screen.getByLabelText('Department'), 'Mathematics')
    await user.click(screen.getByRole('button', { name: 'Create student' }))

    expect(await screen.findByText('Students must be at least 15 years old')).toBeInTheDocument()
    expect(studentService.createStudent).not.toHaveBeenCalled()
  })

  it('creates a student using the backend entity fields', async () => {
    vi.mocked(studentService.createStudent).mockResolvedValue(savedStudent)
    vi.mocked(studentService.getStudents).mockResolvedValue([savedStudent])
    const user = userEvent.setup()
    renderStudentRoutes('/students/add')

    await user.type(screen.getByLabelText('Full name'), 'Mira Sen')
    await user.type(screen.getByLabelText('Email address'), 'mira@example.edu')
    await user.type(screen.getByLabelText('Department'), 'Mathematics')
    await user.click(screen.getByRole('button', { name: 'Create student' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Student created successfully.')
    expect(studentService.createStudent).toHaveBeenCalledWith(expect.objectContaining({
      name: 'Mira Sen',
      email: 'mira@example.edu',
      age: 15,
      department: 'Mathematics',
      semester: 1,
      status: 'ACTIVE',
    }))
  })

  it('loads an existing student and submits an update', async () => {
    vi.mocked(studentService.getStudent).mockResolvedValue(savedStudent)
    vi.mocked(studentService.updateStudent).mockResolvedValue(savedStudent)
    vi.mocked(studentService.getStudents).mockResolvedValue([savedStudent])
    const user = userEvent.setup()
    renderStudentRoutes('/students/21/edit')

    expect(await screen.findByDisplayValue('Mira Sen')).toBeInTheDocument()
    await user.clear(screen.getByLabelText('Department'))
    await user.type(screen.getByLabelText('Department'), 'Applied Mathematics')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(studentService.updateStudent).toHaveBeenCalledWith(
      21,
      expect.objectContaining({ department: 'Applied Mathematics' }),
    ))
    expect(await screen.findByRole('status')).toHaveTextContent('Student updated successfully.')
  })
})
