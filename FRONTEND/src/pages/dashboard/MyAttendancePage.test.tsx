import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../context/auth-context'
import type { AuthContextValue } from '../../context/auth-context'
import { attendanceService } from '../../services/attendanceService'
import type { AttendanceRecord } from '../../types/academic'
import { MyAttendancePage } from './MyAttendancePage'

vi.mock('../../services/attendanceService', () => ({ attendanceService: { getStudentAttendance: vi.fn(), getStudentSummary: vi.fn() } }))
const request = vi.mocked(attendanceService.getStudentAttendance)
const summaryRequest = vi.mocked(attendanceService.getStudentSummary)
const student = { id: 17, name: 'Avery Student', email: 'avery@school.edu', department: 'Science', courses: 'Science', semester: 3, academicYear: '2025-2026', status: 'ACTIVE' as const }
const row = (id: number, courseCode: string, date: string, status: AttendanceRecord['status'], studentId = 17, remarks: string | null = null): AttendanceRecord => ({ id, studentId, courseCode, date, status, remarks, markedBy: 'faculty@school.edu' })
function mount() {
  const auth: AuthContextValue = { user: { email: student.email, role: 'STUDENT' }, currentStudent: student, currentStudentError: null, isAuthenticated: true, login: vi.fn(), verifyMfa: vi.fn(), logout: vi.fn() }
  return render(<AuthContext.Provider value={auth}><MyAttendancePage/></AuthContext.Provider>)
}
const summary = (totalClasses: number, classesAttended: number, classesAbsent: number, attendancePercentage: number, attendanceStatus: string) => ({ totalClasses, classesAttended, classesAbsent, classesExcused: 0, attendancePercentage, preciseAttendancePercentage: attendancePercentage, examEligible: true, attendanceStatus, attendanceRisk: 'LOW', requiredAttendanceThreshold: 75 })
beforeEach(() => { request.mockResolvedValue([]); summaryRequest.mockResolvedValue(summary(0, 0, 0, 0, 'INSUFFICIENT_DATA')) })
afterEach(() => { cleanup(); vi.clearAllMocks() })

describe('student attendance center', () => {
  it('shows a loading skeleton while the student attendance request is pending', () => {
    request.mockReturnValue(new Promise(() => {}))
    mount()
    expect(screen.getByRole('status', { name: 'Loading attendance' })).toBeInTheDocument()
  })

  it('renders real records and derives attendance, course, recent activity, and recovery calculations from the authenticated student records', async () => {
    request.mockResolvedValue([row(1, 'CS301', '2026-10-20', 'PRESENT', 17, 'On time'), row(2, 'CS301', '2026-10-21', 'ABSENT'), row(3, 'BIO101', '2026-10-22', 'LATE'), row(4, 'CS301', '2026-10-22', 'PRESENT'), row(5, 'OTHER', '2026-10-23', 'PRESENT', 99)])
    summaryRequest.mockResolvedValue(summary(4, 3, 1, 75, 'WARNING'))
    mount()
    expect(await screen.findByRole('img', { name: 'Overall attendance 75%' })).toBeInTheDocument()
    expect(screen.getByText('Avery Student')).toBeInTheDocument()
    expect(screen.getByText('2025-2026 · Semester 3')).toBeInTheDocument()
    expect(screen.getByText('On time')).toBeInTheDocument()
    expect(screen.queryByText('Attendance Trend')).not.toBeInTheDocument()
    expect(screen.queryByText('Attendance Calendar')).not.toBeInTheDocument()
    expect(screen.getByText('You currently meet the 75% attendance requirement.')).toBeInTheDocument()
    expect(screen.getAllByText('CS301').length).toBeGreaterThan(0)
    expect(screen.getAllByText('BIO101').length).toBeGreaterThan(0)
    expect(screen.queryByText('OTHER')).not.toBeInTheDocument()
    const statistics = within(screen.getByRole('region', { name: 'Attendance statistics' }))
    expect(statistics.getByText('Present').parentElement).toHaveTextContent('3')
    expect(statistics.getByText('Absent').parentElement).toHaveTextContent('1')
    expect(statistics.getByText('Total Classes').parentElement).toHaveTextContent('4')
    expect(request).toHaveBeenCalledWith(17)
  })

  it('shows a professional error and retry refetches the current student records', async () => {
    request.mockRejectedValueOnce(new Error('Network unavailable')).mockResolvedValueOnce([row(10, 'MA101', '2026-10-24', 'PRESENT')])
    summaryRequest.mockResolvedValueOnce(summary(0, 0, 0, 0, 'INSUFFICIENT_DATA')).mockResolvedValueOnce(summary(1, 1, 0, 100, 'EXCELLENT'))
    mount()
    expect(await screen.findByRole('heading', { name: 'Unable to load attendance' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Try Again' }))
    expect(await screen.findByRole('img', { name: 'Overall attendance 100%' })).toBeInTheDocument()
    expect(request).toHaveBeenCalledTimes(2)
  })

  it('calculates the number of consecutive classes needed to recover to 75%', async () => {
    request.mockResolvedValue([row(1, 'MA101', '2026-10-20', 'ABSENT'), row(2, 'MA101', '2026-10-21', 'ABSENT'), row(3, 'MA101', '2026-10-22', 'PRESENT')])
    summaryRequest.mockResolvedValue(summary(3, 1, 2, 33.33, 'CRITICAL'))
    mount()
    expect(await screen.findByText('Attend the next 5 classes to reach 75%.')).toBeInTheDocument()
    expect(screen.getByText('Critical')).toBeInTheDocument()
  })

  it('shows the dedicated empty state when the API returns no student records', async () => {
    mount()
    expect(await screen.findByRole('heading', { name: 'No attendance records yet' })).toBeInTheDocument()
    expect(screen.queryByText('0%')).not.toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('does not render any records that belong to another student', async () => {
    request.mockResolvedValue([row(1, 'OWN101', '2026-10-20', 'PRESENT'), row(2, 'PRIVATE999', '2026-10-20', 'ABSENT', 88)])
    mount()
    expect((await screen.findAllByText('OWN101')).length).toBeGreaterThan(0)
    await waitFor(() => expect(screen.queryByText('PRIVATE999')).not.toBeInTheDocument())
  })
})
