import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../context/auth-context'
import type { AuthContextValue } from '../../context/auth-context'
import { studentService } from '../../services/studentService'
import { resultService } from '../../services/resultService'
import type { StudentProfile } from '../../types/student'
import { MyProfilePage } from './StudentPages'

vi.mock('../../services/studentService', () => ({ studentService: { getCurrentStudent: vi.fn(), getStudentProfile: vi.fn() } }))
vi.mock('../../services/facultyService', () => ({ facultyService: { getMe: vi.fn() } }))
vi.mock('../../services/resultService', () => ({ resultService: { getStudentCgpa: vi.fn(), getStudentResults: vi.fn(), getStudentTranscript: vi.fn() } }))

const auth: AuthContextValue = {
  user: { email: 'vikas@example.edu', role: 'STUDENT' }, isAuthenticated: true,
  currentStudent: { id: 240001, name: 'Vikas Kumar', email: 'vikas@example.edu', department: 'CSE', courses: 'Algorithms', semester: 4, academicYear: '2025-2026', status: 'ACTIVE' },
  currentStudentError: null, login: vi.fn(), logout: vi.fn(),
}

const profile: StudentProfile = {
  studentDetails: { id: 240001, name: 'Vikas Kumar', email: 'vikas@example.edu', age: 20, department: 'CSE', courses: 'Algorithms', contact: '9876543210', role: 'STUDENT', semester: 4, academicYear: '2025-2026', status: 'ACTIVE' },
  attendanceRecords: [{ id: 1, studentId: 240001, courseCode: 'CS204', date: '2026-02-10', status: 'PRESENT', remarks: null, markedBy: 'faculty@example.edu' }],
  resultRecords: [{ id: 2, studentId: 240001, semester: 4, academicYear: '2025-2026', courseCode: 'CS204', courseName: 'Algorithms', credits: 3, internalMarks: 35, externalMarks: 50, totalMarks: 85, grade: 'A', gradePoints: 9, resultStatus: 'PASS' }],
  totalAttendanceRecords: 1, totalResultRecords: 1,
}

function renderProfile() { return render(<AuthContext.Provider value={auth}><MyProfilePage /></AuthContext.Provider>) }

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(studentService.getCurrentStudent).mockResolvedValue(auth.currentStudent!)
  vi.mocked(resultService.getStudentCgpa).mockResolvedValue({ cgpa: 9 })
})
afterEach(cleanup)

describe('student self profile', () => {
  it('loads authenticated profile identity, academics, contact, status, and real snapshot data', async () => {
    vi.mocked(studentService.getStudentProfile).mockResolvedValue(profile)
    renderProfile()
    await waitFor(() => expect(studentService.getStudentProfile).toHaveBeenCalled())
    expect(await screen.findByRole('heading', { name: 'My Student Profile' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vikas Kumar' })).toBeInTheDocument()
    expect(screen.getAllByText('vikas@example.edu')).toHaveLength(2)
    expect(screen.getByText('240001')).toBeInTheDocument()
    expect(screen.getAllByText('CSE')).toHaveLength(2)
    expect(screen.getByText('Algorithms')).toBeInTheDocument()
    expect(screen.getByText('9876543210')).toBeInTheDocument()
    expect(screen.getAllByText('ACTIVE')).toHaveLength(2)
    expect(screen.getByRole('heading', { name: 'Academic Information' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Academic Snapshot' })).toBeInTheDocument()
    expect(screen.getByText('100%')).toBeInTheDocument()
    expect(screen.getByText('9.00')).toBeInTheDocument()
    expect(screen.getByText('Latest: Algorithms')).toBeInTheDocument()
    await waitFor(() => expect(studentService.getStudentProfile).toHaveBeenCalledWith(240001))
  })

  it('shows graceful fallbacks and zero counts for the real empty academic data', async () => {
    vi.mocked(studentService.getStudentProfile).mockResolvedValue({
      ...profile,
      studentDetails: { ...profile.studentDetails, contact: null, courses: null, department: '' },
      attendanceRecords: null,
      resultRecords: null,
    })
    renderProfile()
    expect(await screen.findByText('Not provided')).toBeInTheDocument()
    expect(screen.getAllByText('Not available').length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: 'Academic Snapshot' })).toBeInTheDocument()
    expect(screen.getByText('No records yet')).toBeInTheDocument()
    expect(screen.getByText('No published results')).toBeInTheDocument()
    expect(screen.queryByText('undefined')).not.toBeInTheDocument()
    expect(screen.queryByText('null')).not.toBeInTheDocument()
  })

  it('shows a loading skeleton until the private profile is returned', () => {
    vi.mocked(studentService.getStudentProfile).mockReturnValue(new Promise(() => {}))
    renderProfile()
    expect(screen.getByRole('status')).toHaveAccessibleName('Loading your profile')
    expect(screen.getByText('Loading profile information')).toBeInTheDocument()
  })

  it('shows a friendly error and retries the same authenticated student profile', async () => {
    vi.mocked(studentService.getStudentProfile).mockRejectedValueOnce(new Error('private backend details')).mockResolvedValueOnce(profile)
    renderProfile()
    expect(await screen.findByText('Please try again.')).toBeInTheDocument()
    expect(screen.queryByText('private backend details')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    await waitFor(() => expect(studentService.getStudentProfile).toHaveBeenCalledTimes(2))
    expect(await screen.findByRole('heading', { name: 'Academic Information' })).toBeInTheDocument()
    expect(studentService.getStudentProfile).toHaveBeenNthCalledWith(1, 240001)
    expect(studentService.getStudentProfile).toHaveBeenNthCalledWith(2, 240001)
  })
})
