import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AuthContext } from '../../context/auth-context'
import type { AuthContextValue } from '../../context/auth-context'
import { academicService } from '../../services/academicService'
import type { CourseOffering, StudentCourseEnrollmentView } from '../../types/academic'
import { CoursesPage, FacultyStudentsPage, MyCoursesPage } from './AcademicPages'

vi.mock('../../services/academicService', () => ({
  academicService: {
    getAvailableCourses: vi.fn(),
    getStudentCourseDetails: vi.fn(),
    enrollStudent: vi.fn(),
    getMyEnrollments: vi.fn(),
    getFacultyStudents: vi.fn(),
  },
}))

const course: CourseOffering = {
  id: 5,
  courseCode: 'DS101',
  courseName: 'Data Structures',
  department: 'CSE',
  credits: 4,
  semester: 1,
  academicYear: '2026-2027',
  active: true,
  enrollmentStatus: null,
  faculty: [{ assignmentId: 7, name: 'Vinay Kumar', email: 'vinay@example.edu', department: 'CSE', semester: 1, academicYear: '2026-2027' }],
}
const currentStudent = {
  id: 21,
  name: 'Alex Student',
  email: 'alex@example.edu',
  department: 'CSE',
  courses: null,
  semester: 1,
  academicYear: '2026-2027',
  status: 'ACTIVE' as const,
}
const studentContext: AuthContextValue = {
  user: { email: currentStudent.email, role: 'STUDENT' },
  currentStudent,
  isAuthenticated: true,
  login: vi.fn(),
  logout: vi.fn(),
}
const facultyContext: AuthContextValue = {
  user: { email: 'vinay@example.edu', role: 'FACULTY' },
  isAuthenticated: true,
  login: vi.fn(),
  logout: vi.fn(),
}

function mount(ui: React.ReactNode, auth: AuthContextValue) {
  return render(<AuthContext.Provider value={auth}><MemoryRouter>{ui}</MemoryRouter></AuthContext.Provider>)
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(academicService.getAvailableCourses).mockResolvedValue([course])
  vi.mocked(academicService.getStudentCourseDetails).mockResolvedValue(course)
  vi.mocked(academicService.getMyEnrollments).mockResolvedValue([{
    id: 22, courseId: course.id, courseName: course.courseName, courseCode: course.courseCode,
    department: course.department, credits: course.credits, courseSemester: course.semester,
    courseAcademicYear: course.academicYear, facultyName: 'Vinay Kumar', facultyEmail: 'vinay@example.edu',
    academicYear: course.academicYear, semester: course.semester, status: 'ACTIVE', enrolledAt: '2026-08-12T10:00:00',
  }])
  vi.mocked(academicService.enrollStudent).mockResolvedValue({
    id: 22,
    courseId: course.id,
    courseName: course.courseName,
    courseCode: course.courseCode,
    department: course.department,
    credits: course.credits,
    courseSemester: course.semester,
    courseAcademicYear: course.academicYear,
    facultyName: 'Vinay Kumar',
    facultyEmail: 'vinay@example.edu',
    academicYear: course.academicYear,
    semester: course.semester,
    status: 'ACTIVE',
  } satisfies StudentCourseEnrollmentView)
  vi.mocked(academicService.getFacultyStudents).mockResolvedValue([
    {
      id: currentStudent.id,
      name: currentStudent.name,
      email: currentStudent.email,
      department: currentStudent.department,
      semester: currentStudent.semester,
      status: currentStudent.status,
      age: 19,
      courses: null,
      contact: null,
      role: 'STUDENT',
      academicYear: currentStudent.academicYear,
    },
  ])
})

afterEach(cleanup)

describe('course to faculty enrollment flow', () => {
  it('lets a student select the assigned faculty before enrolling', async () => {
    const user = userEvent.setup()
    mount(<CoursesPage />, studentContext)
    expect(await screen.findByRole('heading', { name: 'Course catalog' })).toBeInTheDocument()
    await user.click(await screen.findByRole('button', { name: /Data Structures/ }))
    expect(await screen.findByText('Vinay Kumar · CSE')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Confirm enrollment' }))

    await waitFor(() => expect(academicService.enrollStudent).toHaveBeenCalledWith({
      courseId: course.id,
      facultyAssignmentId: 7,
    }))
    expect(await screen.findByRole('heading', { name: 'Enrollment confirmed' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'No courses available' })).toBeInTheDocument()
  })

  it('keeps My Courses cards minimal and opens full course details on demand', async () => {
    const user = userEvent.setup()
    mount(<MyCoursesPage />, studentContext)
    expect(await screen.findByRole('heading', { name: 'Data Structures' })).toBeInTheDocument()
    expect(screen.getByText('DS101')).toBeInTheDocument()
    expect(screen.getByText('ACTIVE')).toBeInTheDocument()
    expect(screen.queryByText('Vinay Kumar')).not.toBeInTheDocument()
    expect(screen.queryByText('vinay@example.edu')).not.toBeInTheDocument()
    expect(screen.queryByText(/Semester 1/)).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'View Course Details' }))
    expect(screen.getByRole('heading', { name: 'Course Overview' })).toBeInTheDocument()
    expect(screen.getByText('Vinay Kumar')).toBeInTheDocument()
    expect(screen.getByText('vinay@example.edu')).toBeInTheDocument()
    expect(screen.getByText(/Semester 1 · 2026-2027/)).toBeInTheDocument()
  })

  it('shows a browse-courses empty state when the student has no enrollments', async () => {
    vi.mocked(academicService.getMyEnrollments).mockResolvedValueOnce([])
    mount(<MyCoursesPage />, studentContext)
    expect(await screen.findByRole('heading', { name: 'No courses enrolled yet' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Browse Courses/ })).toHaveAttribute('href', '/courses')
  })

  it('handles a stale duplicate enrollment by removing the offering and showing the backend reason', async () => {
    const user = userEvent.setup()
    vi.mocked(academicService.enrollStudent).mockRejectedValueOnce(Object.assign(new Error('Conflict'), {
      isAxiosError: true,
      response: { status: 409, data: { message: "You're already enrolled in this course." } },
    }))
    mount(<CoursesPage />, studentContext)
    await user.click(await screen.findByRole('button', { name: /Data Structures/ }))
    await user.click(await screen.findByRole('button', { name: 'Confirm enrollment' }))
    expect(await screen.findByText("You're already enrolled in this course.")).toBeInTheDocument()
    expect(await screen.findByText(/No courses available/)).toBeInTheDocument()
  })

  it('shows an enrolled state instead of an enrollment action for an already-enrolled course', async () => {
    const user = userEvent.setup()
    vi.mocked(academicService.getStudentCourseDetails).mockResolvedValueOnce({ ...course, enrollmentStatus: 'ACTIVE' })
    mount(<CoursesPage />, studentContext)
    await user.click(await screen.findByRole('button', { name: /Data Structures/ }))
    expect(await screen.findByRole('status')).toHaveTextContent('Enrolled')
    expect(screen.queryByRole('button', { name: 'Confirm enrollment' })).not.toBeInTheDocument()
  })

  it('shows students returned for the authenticated faculty member', async () => {
    mount(<FacultyStudentsPage />, facultyContext)
    expect(await screen.findByRole('heading', { name: 'My students' })).toBeInTheDocument()
    expect(await screen.findByText('Alex Student')).toBeInTheDocument()
    expect(academicService.getFacultyStudents).toHaveBeenCalledOnce()
  })
})
