import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../context/auth-context'
import type { AuthContextValue } from '../../context/auth-context'
import { resultService } from '../../services/resultService'
import type { ResultRecord } from '../../types/academic'
import { MyResultsPage } from './MyResultsPage'

vi.mock('../../services/resultService', () => ({ resultService: { getStudentResults: vi.fn(), getStudentCgpa: vi.fn(), getStudentTranscript: vi.fn() } }))
const recordsRequest = vi.mocked(resultService.getStudentResults)
const cgpaRequest = vi.mocked(resultService.getStudentCgpa)
const transcriptRequest = vi.mocked(resultService.getStudentTranscript)
const student = { id: 17, name: 'Asha Rao', email: 'asha@example.edu', department: 'CS', courses: 'CS301', semester: 3, academicYear: '2025-2026', status: 'ACTIVE' as const }
const result = (id: number, semester: number, name: string, studentId = 17, status: ResultRecord['resultStatus'] = 'PASS'): ResultRecord => ({ id, studentId, semester, academicYear: semester === 2 ? '2024-2025' : '2025-2026', courseCode: `CS${id}`, courseName: name, credits: 3, internalMarks: 35, externalMarks: 55, totalMarks: status === 'FAIL' ? 42 : 90, grade: status === 'FAIL' ? 'F' : 'A', gradePoints: status === 'FAIL' ? 0 : 9, resultStatus: status })
const transcript = { totalCreditsRegistered: 6, totalCreditsEarned: 6, cumulativeGPA: 9, overallAcademicStanding: 'First Class with Distinction', semesterBreakdowns: [{ semester: 3, academicYear: '2025-2026', totalCredits: 3, creditsEarned: 3, semesterGPA: 9, semesterStanding: 'Excellent' }, { semester: 2, academicYear: '2024-2025', totalCredits: 3, creditsEarned: 3, semesterGPA: 8.5, semesterStanding: 'Excellent' }] }
function mount() { const auth: AuthContextValue = { user: { email: student.email, role: 'STUDENT' }, currentStudent: student, currentStudentError: null, isAuthenticated: true, login: vi.fn(), logout: vi.fn() }; return render(<AuthContext.Provider value={auth}><MyResultsPage/></AuthContext.Provider>) }
beforeEach(() => { recordsRequest.mockResolvedValue([result(1, 3, 'Data Structures')]); cgpaRequest.mockResolvedValue({ cgpa: 9 }); transcriptRequest.mockResolvedValue(transcript) })
afterEach(() => { cleanup(); vi.clearAllMocks() })

describe('student results dashboard', () => {
  it('shows the authenticated student CGPA, credits, standing, and published subject results', async () => {
    mount()
    expect(await screen.findByRole('heading', { name: 'Subject Performance' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'My Academic Results' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Subject Performance' })).toBeInTheDocument()
    expect(screen.getByRole('article', { name: 'Result for Data Structures' })).toBeInTheDocument()
    expect(screen.getAllByText('9.00').length).toBeGreaterThan(0)
    expect(screen.getAllByText('6 / 6').length).toBeGreaterThan(0)
    expect(screen.getAllByText('First Class with Distinction').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Data Structures').length).toBeGreaterThan(0)
    expect(screen.getAllByText('90').length).toBeGreaterThan(0)
    expect(screen.getAllByText('3').length).toBeGreaterThan(0)
    expect(screen.getAllByText('PASS').length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: 'Semester Performance' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Semester History' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Your Academic Insights' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(recordsRequest).toHaveBeenCalledWith(17)
    expect(cgpaRequest).toHaveBeenCalledWith(17)
  })

  it('renders every subject as a wide result card with its own grade, marks, points, credits, and status', async () => {
    recordsRequest.mockResolvedValue([result(1, 3, 'Data Structures'), result(2, 3, 'Database Management'), result(3, 3, 'Operating Systems', 17, 'FAIL')])
    mount()
    expect(await screen.findByRole('heading', { name: 'Subject Performance' })).toBeInTheDocument()
    expect(screen.getByRole('article', { name: 'Result for Data Structures' })).toHaveTextContent('90 / 100')
    expect(screen.getByRole('article', { name: 'Result for Data Structures' })).toHaveTextContent('9.0')
    expect(screen.getByRole('article', { name: 'Result for Data Structures' })).toHaveTextContent('A')
    expect(screen.getByRole('article', { name: 'Result for Data Structures' })).toHaveTextContent('3')
    expect(screen.getByRole('article', { name: 'Result for Data Structures' })).toHaveTextContent('PASS')
    expect(screen.getByRole('article', { name: 'Result for Database Management' })).toBeInTheDocument()
    expect(screen.getByRole('article', { name: 'Result for Operating Systems' })).toHaveTextContent('FAIL')
    expect(screen.getByRole('article', { name: 'Result for Operating Systems' })).toHaveTextContent('42 / 100')
    expect(screen.getByRole('article', { name: 'Result for Operating Systems' })).toHaveTextContent('F')
  })

  it('switches transcript semesters and filters rows to the selected term', async () => {
    recordsRequest.mockResolvedValue([result(1, 3, 'Data Structures'), result(2, 2, 'Discrete Mathematics')])
    const { container } = mount()
    expect(await screen.findByRole('button', { name: /Semester 2/ })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Semester 2/ }))
    expect(screen.getAllByText('Discrete Mathematics').length).toBeGreaterThan(0)
    expect(container.querySelectorAll('.student-subject-card h3')).toHaveLength(1)
    expect(within(container.querySelector('.student-subject-grid') as HTMLElement).queryByRole('heading', { name: 'Data Structures' })).not.toBeInTheDocument()
  })

  it('does not display result rows returned for a different student ID', async () => {
    recordsRequest.mockResolvedValue([result(1, 3, 'My Subject'), result(2, 3, 'Private Subject', 88)])
    mount()
    expect((await screen.findAllByText('My Subject')).length).toBeGreaterThan(0)
    expect(screen.queryByText('Private Subject')).not.toBeInTheDocument()
  })

  it('shows a loading skeleton while result services are pending', () => {
    recordsRequest.mockReturnValue(new Promise(() => {}))
    mount()
    expect(screen.getByRole('status', { name: 'Loading your results' })).toBeInTheDocument()
  })

  it('shows a retryable error when result loading fails', async () => {
    recordsRequest.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce([result(1, 3, 'Data Structures')])
    mount()
    expect(await screen.findByRole('heading', { name: 'Unable to load your academic results' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('heading', { name: 'Subject Performance' })).toBeInTheDocument()
    await waitFor(() => expect(recordsRequest).toHaveBeenCalledTimes(2))
  })

  it('shows a clean empty state when the authenticated student has no published results', async () => {
    recordsRequest.mockResolvedValue([])
    mount()
    expect(await screen.findByRole('heading', { name: 'No academic results yet' })).toBeInTheDocument()
    expect(screen.queryByText('0.00')).not.toBeInTheDocument()
  })
})
