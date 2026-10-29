import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../context/auth-context'
import type { AuthContextValue } from '../context/auth-context'
import { academicService } from '../services/academicService'
import { attendanceService } from '../services/attendanceService'
import { resultService } from '../services/resultService'
import { studentService } from '../services/studentService'
import type { Student } from '../types/student'
import { AttendancePage } from './attendance/AttendancePage'
import { ResultsPage } from './results/ResultsPage'

vi.mock('../services/academicService', () => ({ academicService: { getFacultyCourses: vi.fn(), getFacultyStudents: vi.fn() } }))
vi.mock('../services/attendanceService', () => ({ attendanceService: { getAttendance: vi.fn(), getAttendanceByCourse: vi.fn(), markBulkAttendance: vi.fn(), markAttendance: vi.fn(), updateAttendance: vi.fn(), deleteAttendance: vi.fn() } }))
vi.mock('../services/resultService', () => ({ resultService: { getResults: vi.fn(), recordResult: vi.fn(), updateResult: vi.fn(), deleteResult: vi.fn() } }))
vi.mock('../services/studentService', () => ({ studentService: { getStudents: vi.fn() } }))

const student: Student = { id: 17, name: 'Asha Rao', email: 'asha@example.edu', age: 19, department: 'CS', courses: 'CS301', contact: '', role: 'STUDENT', semester: 3, academicYear: '2025-2026', status: 'ACTIVE' }
const faculty: AuthContextValue = { user: { email: 'faculty@example.edu', role: 'FACULTY' }, isAuthenticated: true, login: vi.fn(), logout: vi.fn() }
const admin: AuthContextValue = { ...faculty, user: { email: 'admin@example.edu', role: 'ADMIN' } }
function mount(ui: React.ReactNode, auth = faculty) { return render(<AuthContext.Provider value={auth}><MemoryRouter>{ui}</MemoryRouter></AuthContext.Provider>) }

afterEach(() => { cleanup(); vi.clearAllMocks() })

describe('Attendance and Results integrations', () => {
  it('loads live attendance and lets faculty submit the backend request shape', async () => {
    vi.mocked(academicService.getFacultyCourses).mockResolvedValue([{ id: 17, courseCode: 'CS301', courseName: 'Data Structures', department: 'CS', semester: 3, academicYear: '2025-2026', credits: 3, active: true }])
    vi.mocked(academicService.getFacultyStudents).mockResolvedValue([student])
    vi.mocked(attendanceService.getAttendanceByCourse).mockResolvedValue([])
    vi.mocked(attendanceService.markBulkAttendance).mockResolvedValue([{ id: 4, studentId: 17, courseCode: 'CS301', date: '2026-10-22', status: 'PRESENT', remarks: null, markedBy: 'faculty@example.edu' }])
    vi.mocked(studentService.getStudents).mockResolvedValue([student])
    const user = userEvent.setup(); mount(<AttendancePage />)
    await user.selectOptions(await screen.findByLabelText('Select Course'), '17')
    expect(await screen.findByText('Data Structures')).toBeInTheDocument()
    await user.click(await screen.findByRole('button', { name: 'Mark All Present' }))
    await user.click(screen.getByRole('button', { name: 'Save Attendance — 1 Students' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Attendance saved for 1 students.')
    expect(attendanceService.markBulkAttendance).toHaveBeenCalledWith(expect.objectContaining({ courseCode: 'CS301', attendanceRecords: [{ studentId: 17, status: 'PRESENT', remarks: '' }] }))
  })

  it('shows attendance read-only controls to admins', async () => {
    vi.mocked(attendanceService.getAttendance).mockResolvedValue([])
    vi.mocked(studentService.getStudents).mockResolvedValue([student])
    mount(<AttendancePage />, admin)
    expect(await screen.findByText('No attendance records are available.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Save attendance' })).not.toBeInTheDocument()
  })

  it('loads result records and sends marks using the ResultRequestDTO fields', async () => {
    vi.mocked(resultService.getResults).mockResolvedValue([{ id: 8, studentId: 17, semester: 3, academicYear: '2025-2026', courseCode: 'CS301', courseName: 'Data Structures', credits: 3, internalMarks: 35, externalMarks: 50, totalMarks: 85, grade: 'A', gradePoints: 9, resultStatus: 'PASS' }])
    vi.mocked(studentService.getStudents).mockResolvedValue([student])
    vi.mocked(resultService.recordResult).mockResolvedValue({ id: 9, studentId: 17, semester: 3, academicYear: '2025-2026', courseCode: 'CS302', courseName: 'Algorithms', credits: 3, internalMarks: 32, externalMarks: 45, totalMarks: 77, grade: 'B', gradePoints: 8, resultStatus: 'PASS' })
    const user = userEvent.setup(); mount(<ResultsPage />)
    expect(await screen.findByText('Data Structures')).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Student'), '17')
    await user.type(screen.getByLabelText('Course code'), 'CS302')
    await user.type(screen.getByLabelText('Course name'), 'Algorithms')
    await user.clear(screen.getByLabelText('Internal marks (0–40)')); await user.type(screen.getByLabelText('Internal marks (0–40)'), '32')
    await user.clear(screen.getByLabelText('External marks (0–60)')); await user.type(screen.getByLabelText('External marks (0–60)'), '45')
    await user.click(screen.getByRole('button', { name: 'Save result' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Grade and total were calculated by the Result Service.')
    expect(resultService.recordResult).toHaveBeenCalledWith(expect.objectContaining({ studentId: 17, courseCode: 'CS302', courseName: 'Algorithms', internalMarks: 32, externalMarks: 45 }))
  })
})
