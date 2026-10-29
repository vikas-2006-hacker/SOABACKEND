import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { academicService } from '../../services/academicService'
import { facultyService } from '../../services/facultyService'
import type { AcademicCourse, FacultyCourseAssignment, StudentCourseEnrollment } from '../../types/academic'
import type { Faculty } from '../../services/facultyService'
import { AdminCoursesPage } from './AdminCoursesPage'

vi.mock('../../services/academicService', () => ({
  academicService: {
    getCourses: vi.fn(),
    getCourseFaculty: vi.fn(),
    listAllEnrollments: vi.fn(),
    createCourse: vi.fn(),
    updateCourse: vi.fn(),
    assignFacultyToCourse: vi.fn(),
  },
}))

vi.mock('../../services/facultyService', () => ({ facultyService: { list: vi.fn() } }))

const course: AcademicCourse = {
  id: 5,
  courseCode: 'DS101',
  courseName: 'Data Structures',
  department: 'CSE',
  credits: 4,
  semester: 1,
  academicYear: '2026-2027',
  description: 'Foundations of data structures.',
  active: true,
}
const assignment: FacultyCourseAssignment = {
  id: 3,
  facultyEmail: 'vinay@example.edu',
  courseId: course.id,
  academicYear: course.academicYear,
  semester: course.semester,
  active: true,
}
const activeFaculty: Faculty = {
  id: 9,
  name: 'Dr. Vinay Kumar',
  email: 'vinay@example.edu',
  employeeId: 'FAC-9',
  department: 'CSE',
  status: 'ACTIVE',
  mfaEnabled: true,
  createdAt: '',
  updatedAt: '',
}
const inactiveFaculty: Faculty = {
  ...activeFaculty,
  id: 10,
  name: 'Inactive Faculty',
  email: 'inactive@example.edu',
  status: 'INACTIVE',
}
const enrollment: StudentCourseEnrollment = {
  id: 15,
  studentId: 21,
  courseId: course.id,
  facultyEmail: activeFaculty.email,
  academicYear: course.academicYear,
  semester: course.semester,
  status: 'ACTIVE',
}

function mockLoadedCourses(items: AcademicCourse[] = [course]) {
  vi.mocked(academicService.getCourses).mockResolvedValue(items)
  vi.mocked(academicService.getCourseFaculty).mockImplementation(async (id) => id === course.id ? [assignment] : [])
  vi.mocked(academicService.listAllEnrollments).mockResolvedValue([enrollment])
  vi.mocked(facultyService.list).mockResolvedValue([activeFaculty, inactiveFaculty])
}

beforeEach(() => {
  vi.clearAllMocks()
  mockLoadedCourses()
  vi.mocked(academicService.createCourse).mockResolvedValue(course)
  vi.mocked(academicService.updateCourse).mockResolvedValue(course)
  vi.mocked(academicService.assignFacultyToCourse).mockResolvedValue(assignment)
})

afterEach(cleanup)

describe('Admin course management', () => {
  it('renders course details, faculty, enrollment count, status, and statistics', async () => {
    render(<AdminCoursesPage />)
    expect(await screen.findByRole('heading', { name: 'Course Management' })).toBeInTheDocument()
    expect(await screen.findByText('Dr. Vinay Kumar')).toBeInTheDocument()
    expect(screen.getByText('1 Student')).toBeInTheDocument()
    expect(screen.getByText('DS101')).toBeInTheDocument()
    expect(screen.getByText('Active', { selector: '.admin-course-status' })).toBeInTheDocument()
    expect(within(screen.getByLabelText('Course statistics')).getAllByText('1', { selector: 'strong' })).toHaveLength(3)
  })

  it('creates a course with required fields and refreshes the catalog', async () => {
    const user = userEvent.setup()
    render(<AdminCoursesPage />)
    await user.click(await screen.findByRole('button', { name: 'Create Course' }))
    const dialog = screen.getByRole('dialog', { name: 'Create course' })
    await user.type(within(dialog).getByLabelText('Course code'), 'AI201')
    await user.type(within(dialog).getByLabelText('Course name'), 'Artificial Intelligence')
    await user.type(within(dialog).getByLabelText('Department'), 'CSE')
    await user.type(within(dialog).getByLabelText('Credits'), '3')
    await user.type(within(dialog).getByLabelText('Semester'), '2')
    await user.type(within(dialog).getByLabelText('Academic year'), '2026-2027')
    await user.click(within(dialog).getByRole('button', { name: 'Create Course' }))

    await waitFor(() => expect(academicService.createCourse).toHaveBeenCalledWith({
      courseCode: 'AI201',
      courseName: 'Artificial Intelligence',
      department: 'CSE',
      credits: 3,
      semester: 2,
      academicYear: '2026-2027',
      description: '',
      active: true,
    }))
    expect(await screen.findByRole('status')).toHaveTextContent('Course created successfully')
    expect(academicService.getCourses).toHaveBeenCalledTimes(2)
  })

  it('edits an existing course through Manage', async () => {
    const user = userEvent.setup()
    render(<AdminCoursesPage />)
    await user.click(await screen.findByRole('button', { name: /manage/i }))
    const dialog = screen.getByRole('dialog', { name: 'Manage course' })
    const name = within(dialog).getByLabelText('Course name')
    await user.clear(name)
    await user.type(name, 'Advanced Data Structures')
    await user.click(within(dialog).getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(academicService.updateCourse).toHaveBeenCalledWith(course.id, expect.objectContaining({
      courseName: 'Advanced Data Structures',
      courseCode: course.courseCode,
    })))
    expect(await screen.findByRole('status')).toHaveTextContent('Course updated successfully')
  })

  it('assigns only active faculty to a course', async () => {
    const user = userEvent.setup()
    render(<AdminCoursesPage />)
    await user.click(await screen.findByRole('button', { name: 'Assign Faculty' }))
    const dialog = screen.getByRole('dialog', { name: 'Assign faculty' })
    const select = within(dialog).getByLabelText('Available faculty')
    expect(within(select).queryByRole('option', { name: /Inactive Faculty/ })).not.toBeInTheDocument()
    await user.selectOptions(select, activeFaculty.email)
    await user.click(within(dialog).getByRole('button', { name: 'Confirm assignment' }))
    await waitFor(() => expect(academicService.assignFacultyToCourse).toHaveBeenCalledWith({
      courseId: course.id,
      facultyEmail: activeFaculty.email,
      academicYear: course.academicYear,
      semester: course.semester,
    }))
    expect(await screen.findByRole('status')).toHaveTextContent('Faculty assigned successfully')
  })

  it('treats an unassigned course as a valid state', async () => {
    vi.mocked(academicService.getCourseFaculty).mockResolvedValue([])
    render(<AdminCoursesPage />)
    expect(await screen.findByText('No faculty assigned yet')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows a route-specific message when course creation receives a Gateway 404', async () => {
    const user = userEvent.setup()
    vi.mocked(academicService.createCourse).mockRejectedValueOnce(Object.assign(new Error('Not found'), {
      isAxiosError: true,
      response: {
        status: 404,
        data: { path: '/api/admin/courses', status: 404, error: 'Not Found', requestId: 'gateway-id' },
      },
    }))
    render(<AdminCoursesPage />)
    await user.click(await screen.findByRole('button', { name: 'Create Course' }))
    const dialog = screen.getByRole('dialog', { name: 'Create course' })
    await user.type(within(dialog).getByLabelText('Course code'), 'DS1010')
    await user.type(within(dialog).getByLabelText('Course name'), 'Data Science')
    await user.type(within(dialog).getByLabelText('Department'), 'CSE')
    await user.type(within(dialog).getByLabelText('Credits'), '4')
    await user.type(within(dialog).getByLabelText('Semester'), '2')
    await user.type(within(dialog).getByLabelText('Academic year'), '2026-2027')
    await user.type(within(dialog).getByLabelText('Description'), 'data science')
    await user.click(within(dialog).getByRole('button', { name: 'Create Course' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Course management is unavailable through the API Gateway')
    expect(within(dialog).getByRole('alert')).toHaveTextContent('academic course route is enabled')
  })

  it('shows loading, empty, API-error, and retry states without redirecting', async () => {
    let resolveCourses: (items: AcademicCourse[]) => void = () => undefined
    vi.mocked(academicService.getCourses).mockImplementationOnce(() => new Promise((resolve) => { resolveCourses = resolve }))
    const { rerender } = render(<AdminCoursesPage />)
    expect(await screen.findByRole('status')).toHaveTextContent('Loading courses')
    resolveCourses([])
    await waitFor(() => expect(screen.getByText('No courses yet')).toBeInTheDocument())

    vi.mocked(academicService.getCourses).mockRejectedValueOnce(new Error('unavailable'))
    rerender(<AdminCoursesPage key="api-error" />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load courses')
    expect(screen.getByText('Please try again.')).toBeInTheDocument()

    mockLoadedCourses()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByText('DS101')).toBeInTheDocument()
  })
})
