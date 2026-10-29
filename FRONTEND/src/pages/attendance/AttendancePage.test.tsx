import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../context/auth-context'
import type { AuthContextValue } from '../../context/auth-context'
import { academicService } from '../../services/academicService'
import { attendanceService } from '../../services/attendanceService'
import type { AcademicCourse, AttendanceRecord } from '../../types/academic'
import type { Student } from '../../types/student'
import { AttendancePage } from './AttendancePage'

vi.mock('../../services/academicService', () => ({
  academicService: { getFacultyCourses: vi.fn(), getFacultyStudents: vi.fn() },
}))
vi.mock('../../services/attendanceService', () => ({
  attendanceService: {
    getAttendance: vi.fn(), getAttendanceByCourse: vi.fn(), markBulkAttendance: vi.fn(),
    markAttendance: vi.fn(), updateAttendance: vi.fn(), deleteAttendance: vi.fn(),
  },
}))

const coursesRequest = vi.mocked(academicService.getFacultyCourses)
const studentsRequest = vi.mocked(academicService.getFacultyStudents)
const attendanceRequest = vi.mocked(attendanceService.getAttendanceByCourse)
const saveAttendance = vi.mocked(attendanceService.markBulkAttendance)

const course: AcademicCourse = {
  id: 4, courseCode: 'BIO101', courseName: 'Biology', department: 'Science', semester: 1,
  academicYear: '2026-2027', credits: 3, active: true,
}

function student(id: number, name: string): Student {
  return { id, name, email: `${name.toLowerCase().replaceAll(' ', '.')}@school.edu`, age: 18, department: 'Science', courses: null, contact: null, role: 'STUDENT', semester: 1, academicYear: '2026-2027', status: 'ACTIVE' }
}

function renderAttendance() {
  const auth: AuthContextValue = {
    user: { email: 'faculty@school.edu', role: 'FACULTY' }, isAuthenticated: true,
    login: vi.fn(), logout: vi.fn(),
  }
  return render(<AuthContext.Provider value={auth}><MemoryRouter><AttendancePage /></MemoryRouter></AuthContext.Provider>)
}

beforeEach(() => {
  coursesRequest.mockResolvedValue([course])
  studentsRequest.mockResolvedValue([])
  attendanceRequest.mockResolvedValue([])
  saveAttendance.mockResolvedValue([] as AttendanceRecord[])
})
afterEach(() => { cleanup(); vi.clearAllMocks() })

describe('faculty course attendance sheet', () => {
  it('loads the active course roster, including students without previous attendance', async () => {
    studentsRequest.mockResolvedValue([student(1, 'Student One'), student(2, 'Student Two')])
    const user = userEvent.setup()
    renderAttendance()
    await user.selectOptions(await screen.findByLabelText('Select Course'), '4')
    expect(await screen.findByText('Student One')).toBeInTheDocument()
    expect(screen.getByText('Student Two')).toBeInTheDocument()
    expect(studentsRequest).toHaveBeenCalledWith(4)
    expect(screen.getByText(/2 Students/)).toBeInTheDocument()
  })

  it('submits the selected course roster through the bulk attendance API', async () => {
    studentsRequest.mockResolvedValue([student(41, 'Student Forty One'), student(53, 'Student Fifty Three')])
    const user = userEvent.setup()
    renderAttendance()
    await user.selectOptions(await screen.findByLabelText('Select Course'), '4')
    const studentGroup = await screen.findByRole('group', { name: 'Attendance for Student Fifty Three' })
    await user.click(within(studentGroup).getByRole('button', { name: 'Present' }))
    await user.click(screen.getByRole('button', { name: 'Mark All Present' }))
    await user.click(screen.getByRole('button', { name: 'Save Attendance — 2 Students' }))
    await waitFor(() => expect(saveAttendance).toHaveBeenCalledWith(expect.objectContaining({
      courseCode: 'BIO101',
      attendanceRecords: [
        { studentId: 41, status: 'PRESENT', remarks: '' },
        { studentId: 53, status: 'PRESENT', remarks: '' },
      ],
    })))
  })

  it('shows the empty roster state when the course has no active students', async () => {
    const user = userEvent.setup()
    renderAttendance()
    await user.selectOptions(await screen.findByLabelText('Select Course'), '4')
    expect(await screen.findByText('No active students are enrolled in this course.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Save Attendance — 0 Students' })).toBeDisabled()
  })

  it('shows an authorization error when the attendance history cannot be loaded', async () => {
    studentsRequest.mockResolvedValue([student(5, 'Available Student')])
    attendanceRequest.mockRejectedValue(Object.assign(new Error('Forbidden'), { isAxiosError: true, response: { status: 403 } }))
    const user = userEvent.setup()
    renderAttendance()
    await user.selectOptions(await screen.findByLabelText('Select Course'), '4')
    expect(await screen.findByText('You do not have permission to perform this action.')).toBeInTheDocument()
    expect(attendanceRequest).toHaveBeenCalledWith('BIO101')
  })

  it('shows an error when the faculty course list cannot be loaded', async () => {
    coursesRequest.mockRejectedValue(Object.assign(new Error('Forbidden'), { isAxiosError: true, response: { status: 403 } }))
    renderAttendance()
    expect(await screen.findByText('You do not have permission to perform this action.')).toBeInTheDocument()
    expect(screen.getByText('No courses assigned')).toBeInTheDocument()
  })
})
