import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../context/auth-context'
import type { AuthContextValue } from '../../context/auth-context'
import { attendanceService } from '../../services/attendanceService'
import { resultService } from '../../services/resultService'
import { studentService } from '../../services/studentService'
import { academicInsightsService } from '../../services/academicInsightsService'
import { academicService } from '../../services/academicService'
import { DashboardPage } from './DashboardPage'

vi.mock('../../services/attendanceService', () => ({ attendanceService: { getStudentSummary: vi.fn(), getStudentAttendance: vi.fn(), getAttendanceByDate: vi.fn() } }))
vi.mock('../../services/resultService', () => ({ resultService: { getStudentCgpa: vi.fn(), getStudentResults: vi.fn(), getStudentTranscript: vi.fn(), getResults: vi.fn() } }))
vi.mock('../../services/studentService', () => ({ studentService: { getCurrentStudent: vi.fn(), getStudents: vi.fn() } }))
vi.mock('../../services/academicInsightsService', () => ({ academicInsightsService: { getMyInsights: vi.fn() } }))
vi.mock('../../services/academicService', () => ({ academicService: { getMyEnrollments: vi.fn() } }))

afterEach(cleanup)

const auth: AuthContextValue = {
  user: { email: 'asha@example.edu', role: 'STUDENT' }, isAuthenticated: true,
  currentStudent: { id: 17, name: 'Asha Rao', email: 'asha@example.edu', department: 'CS', courses: 'CS301', semester: 3, academicYear: '2025-2026', status: 'ACTIVE' },
  currentStudentError: null, login: vi.fn(), logout: vi.fn(),
}

describe('student dashboard', () => {
  it('shows current profile, attendance and academic data from the current student ID', async () => {
    vi.mocked(academicService.getMyEnrollments).mockResolvedValue([])
    vi.mocked(attendanceService.getStudentAttendance).mockResolvedValue([])
    vi.mocked(resultService.getStudentResults).mockResolvedValue([{ id: 3, studentId: 17, semester: 3, academicYear: '2025-2026', courseCode: 'CS301', courseName: 'Data Structures', credits: 3, internalMarks: 35, externalMarks: 50, totalMarks: 85, grade: 'A', gradePoints: 9, resultStatus: 'PASS' }])
    vi.mocked(academicInsightsService.getMyInsights).mockResolvedValue({ studentId: 17, studentName: 'Asha Rao', department: 'CS', semester: 3, studentStatus: 'ACTIVE', attendance: { totalClasses: 10, classesAttended: 8, classesAbsent: 2, classesExcused: 0, percentage: 80, examEligible: true, status: 'GOOD', risk: 'LOW', requiredThreshold: 75 }, performance: { cumulativeGpa: 8.75, standing: 'FIRST CLASS', resultsCount: 1, averageMarks: 85, creditsRegistered: 3, creditsEarned: 3, strongestSubject: { courseName: 'Data Structures', courseCode: 'CS301', semester: 3, academicYear: '2025-2026', marks: 85, grade: 'A', gradePoints: 9, credits: 3, resultStatus: 'PASS' }, focusSubject: null, latestResult: null }, insights: [{ category: 'RESULTS', title: 'Strongest recorded subject', detail: 'Data Structures has the highest recorded grade points (9.0) across 1 published results.', severity: 'POSITIVE' }], unavailableSources: [] })
    render(<AuthContext.Provider value={auth}><MemoryRouter><DashboardPage /></MemoryRouter></AuthContext.Provider>)
    expect(await screen.findByRole('heading', { name: /Good .* Asha/ })).toBeInTheDocument()
    expect((await screen.findAllByText('80%')).length).toBeGreaterThan(0)
    expect(await screen.findByText('8.75')).toBeInTheDocument()
    expect(await screen.findByText('Data Structures')).toBeInTheDocument()
    expect(await screen.findByText('Strongest recorded subject')).toBeInTheDocument()
    expect(screen.getByText(/highest recorded grade points \(9.0\)/)).toBeInTheDocument()
    await waitFor(() => expect(academicInsightsService.getMyInsights).toHaveBeenCalled())
    expect(resultService.getStudentResults).toHaveBeenCalledWith(17)
    expect(studentService.getStudents).not.toHaveBeenCalled()
  })

  it('shows a retryable academic overview error without exposing another student record', async () => {
    vi.mocked(academicService.getMyEnrollments).mockResolvedValue([])
    vi.mocked(academicInsightsService.getMyInsights).mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce({ studentId: 17, studentName: 'Asha Rao', department: 'CS', semester: 3, studentStatus: 'ACTIVE', attendance: null, performance: null, insights: [], unavailableSources: ['attendance', 'results'] })
    vi.mocked(attendanceService.getStudentAttendance).mockResolvedValue([])
    vi.mocked(resultService.getStudentResults).mockResolvedValue([])
    const { findByRole } = render(<AuthContext.Provider value={auth}><MemoryRouter><DashboardPage /></MemoryRouter></AuthContext.Provider>)
    expect(await findByRole('heading', { name: 'Unable to load your academic overview' })).toBeInTheDocument()
    const retry = await screen.findByRole('button', { name: 'Try again' })
    retry.click()
    expect(await findByRole('heading', { name: 'Your academic journey starts here' })).toBeInTheDocument()
    expect(await findByRole('heading', { name: 'Your academic insights' })).toBeInTheDocument()
    expect(screen.getByText(/Not enough academic data yet/)).toBeInTheDocument()
    expect(academicInsightsService.getMyInsights).toHaveBeenCalledTimes(2)
  })
})
