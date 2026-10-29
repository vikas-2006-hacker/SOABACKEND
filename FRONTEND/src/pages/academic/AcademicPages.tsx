import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BookOpenCheck, GraduationCap, LoaderCircle, UsersRound, X, RotateCw, ArrowRight } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { academicService } from '../../services/academicService'
import { getApiErrorMessage } from '../../services/api'
import type { AcademicCourse, CourseOffering, StudentCourseEnrollmentView } from '../../types/academic'
import type { Student } from '../../types/student'

export function CoursesPage() {
  const { user } = useAuth()
  const [courses, setCourses] = useState<CourseOffering[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedCourse, setSelectedCourse] = useState<CourseOffering | null>(null)
  const [details, setDetails] = useState<CourseOffering | null>(null)
  const [selectedAssignment, setSelectedAssignment] = useState<number | null>(null)
  const [enrolling, setEnrolling] = useState(false)
  const [success, setSuccess] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setCourses(await academicService.getAvailableCourses())
    } catch (cause) {
      setError(getApiErrorMessage(cause))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const request = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(request)
  }, [load])

  const openCourse = async (course: CourseOffering) => {
    setSelectedCourse(course)
    setDetails(null)
    setSelectedAssignment(course.faculty[0]?.assignmentId ?? null)
    setError('')
    try {
      setDetails(await academicService.getStudentCourseDetails(course.id))
    } catch (cause) {
      setError(getApiErrorMessage(cause))
      setDetails(course)
    }
  }

  const handleEnroll = async (course: CourseOffering) => {
    if (!selectedAssignment) {
      setError('Choose an assigned faculty member to continue.')
      return
    }
    setEnrolling(true)
    setError('')
    setSuccess('')
    try {
      const enrollment = await academicService.enrollStudent({ courseId: course.id, facultyAssignmentId: selectedAssignment })
      setCourses((current) => current.filter((item) => item.id !== course.id))
      setSuccess(`You’re enrolled in ${enrollment.courseName}.`)
      setSelectedCourse(null)
    } catch (cause) {
      const message = getApiErrorMessage(cause)
      setError(message)
      if (message.toLowerCase().includes('already enrolled')) {
        setCourses((current) => current.filter((item) => item.id !== course.id))
        setSelectedCourse(null)
      } else void load()
    } finally {
      setEnrolling(false)
    }
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Course catalog</h1>
          <p>Browse available courses and the faculty assigned to each offering.</p>
        </div>
      </div>
      {error && !selectedCourse && <div className="form-error" role="alert">{error}<button className="student-button" type="button" onClick={() => void load()}>Try again</button></div>}
      {success && <div className="student-state-card course-success" role="status"><span><BookOpenCheck size={18}/></span><div><h2>Enrollment confirmed</h2><p>{success}</p></div><Link to="/my-courses" className="student-button">My courses <ArrowRight size={15}/></Link></div>}
      {loading ? (
        <CourseLoadingState label="Loading course catalog…" />
      ) : (
        courses.length === 0 ? <div className="student-empty-card"><span><GraduationCap size={24}/></span><h2>No courses available</h2><p>Your academic team hasn’t published any active courses yet.</p></div> :
        <section className="student-dashboard-main-grid course-offering-grid">
          {courses.map((course) => <button key={course.id} type="button" className="student-card course-offering-card" onClick={() => void openCourse(course)}>
            <div className="student-card-header"><div className="metric-icon"><BookOpenCheck size={16}/></div><div><small>{course.courseCode}</small><h2>{course.courseName}</h2></div></div>
            <p className="student-secondary">{course.department} · {course.credits} credits · Semester {course.semester}</p>
            <p>{course.description || 'Course information will be shared when the academic team publishes it.'}</p>
            <span className="course-card-footer">{course.faculty.length} assigned {course.faculty.length === 1 ? 'faculty member' : 'faculty members'} <ArrowRight size={15}/></span>
          </button>)}
        </section>
      )}
      {selectedCourse && <div className="course-dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelectedCourse(null) }}><section className="course-dialog" role="dialog" aria-modal="true" aria-labelledby="course-dialog-title">
        <button className="course-dialog-close" type="button" aria-label="Close course details" onClick={() => setSelectedCourse(null)}><X size={18}/></button>
        {!details ? <div className="page-loading" role="status"><LoaderCircle className="spin" size={18}/> Loading course details…</div> : <>
          <span className="student-eyebrow">COURSE OFFERING</span><h2 id="course-dialog-title">{details.courseName}</h2><p className="course-dialog-code">{details.courseCode} · {details.department}</p>
          <p>{details.description || 'Course information will be shared when the academic team publishes it.'}</p>
          <div className="course-dialog-facts"><span><small>Credits</small><strong>{details.credits}</strong></span><span><small>Semester</small><strong>{details.semester}</strong></span><span><small>Academic year</small><strong>{details.academicYear}</strong></span></div>
          {details.enrollmentStatus ? <><div className="course-already-enrolled" role="status"><span aria-hidden="true">✓</span><div><strong>Enrolled</strong><small>Your enrollment status is {details.enrollmentStatus.toLowerCase()}.</small></div></div><section className="course-dialog-faculty"><span className="student-eyebrow">ASSIGNED FACULTY</span>{details.faculty.map((faculty) => <div key={faculty.assignmentId}><strong>{faculty.name}</strong><small>{faculty.email}</small></div>)}</section></> : <><label htmlFor="course-faculty">Assigned faculty</label><select id="course-faculty" value={selectedAssignment ?? ''} onChange={(event) => setSelectedAssignment(Number(event.target.value))}>
            {details.faculty.map((faculty) => <option key={faculty.assignmentId} value={faculty.assignmentId}>{faculty.name} · {faculty.department || faculty.designation || faculty.email}</option>)}
          </select>
          {error && <div className="form-error" role="alert">{error}</div>}
          <button className="primary-action" type="button" disabled={enrolling || !details.faculty.length || user?.role !== 'STUDENT'} onClick={() => void handleEnroll(details)}>{enrolling ? 'Enrolling…' : 'Confirm enrollment'}</button></>}
        </>}
      </section></div>}
    </>
  )
}

export function MyCoursesPage() {
  const [enrollments, setEnrollments] = useState<StudentCourseEnrollmentView[]>([])
  const [selectedEnrollment, setSelectedEnrollment] = useState<StudentCourseEnrollmentView | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const nextEnrollments = await academicService.getMyEnrollments()
      setEnrollments(nextEnrollments)
    } catch (cause) {
      setError(getApiErrorMessage(cause))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const request = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(request)
  }, [load])

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>My courses</h1>
          <p>Your enrolled courses and their current status.</p>
        </div>
      </div>
      {error && <div className="student-state-card" role="alert"><span><RotateCw size={18}/></span><div><h2>Couldn’t load your courses</h2><p>{error}</p></div><button className="student-button" type="button" onClick={() => void load()}>Try again</button></div>}
      {loading ? (
        <CourseLoadingState label="Loading your courses…" />
      ) : enrollments.length === 0 ? (
        <div className="student-empty-card"><span><GraduationCap size={24} /></span><h2>No courses enrolled yet</h2><p>Explore the course catalog and enroll in a course to see it here.</p><Link to="/courses" className="student-button">Browse Courses <ArrowRight size={15}/></Link></div>
      ) : (
        <section className="student-dashboard-main-grid course-enrollment-grid">{enrollments.map((enrollment) => <article key={enrollment.id} className="student-card enrolled-course-card">
          <div className="enrolled-course-heading"><div><small>{enrollment.courseCode}</small><h2>{enrollment.courseName}</h2></div><span className={`enrolled-course-status status-${enrollment.status.toLowerCase()}`}>{enrollment.status}</span></div>
          <button className="course-text-action" type="button" onClick={() => setSelectedEnrollment(enrollment)}>View Course Details <ArrowRight size={15}/></button>
        </article>)}</section>
      )}
      {selectedEnrollment && <div className="course-dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelectedEnrollment(null) }}><section className="course-dialog enrolled-course-details" role="dialog" aria-modal="true" aria-labelledby="enrolled-course-title"><button className="course-dialog-close" type="button" aria-label="Close course details" onClick={() => setSelectedEnrollment(null)}><X size={18}/></button>
        <span className="student-eyebrow">COURSE DETAILS</span><h2 id="enrolled-course-title">{selectedEnrollment.courseName}</h2><p className="course-dialog-code">{selectedEnrollment.courseCode}</p>
        <section className="course-detail-section"><h3>Course Overview</h3><p>{selectedEnrollment.description || 'No course description is available.'}</p></section>
        <section className="course-detail-section"><h3>Academic Information</h3><div className="course-detail-grid"><span><small>Department</small><strong>{selectedEnrollment.department || 'Not available'}</strong></span><span><small>Credits</small><strong>{selectedEnrollment.credits}</strong></span><span><small>Semester</small><strong>{selectedEnrollment.courseSemester}</strong></span><span><small>Academic year</small><strong>{selectedEnrollment.courseAcademicYear || 'Not available'}</strong></span></div></section>
        <section className="course-detail-section"><h3>Faculty</h3><div className="course-detail-faculty"><span className="faculty-initial-avatar" aria-hidden="true">{initials(selectedEnrollment.facultyName)}</span><div><strong>{selectedEnrollment.facultyName}</strong><small>{selectedEnrollment.facultyEmail}</small></div></div></section>
        <section className="course-detail-section"><h3>Enrollment</h3><div className="course-detail-grid"><span><small>Enrollment status</small><strong>{selectedEnrollment.status}</strong></span><span><small>Current academic status</small><strong>{selectedEnrollment.status}</strong></span><span><small>Enrollment date</small><strong>{selectedEnrollment.enrolledAt ? new Date(selectedEnrollment.enrolledAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' }) : 'Not available'}</strong></span><span><small>Enrollment term</small><strong>Semester {selectedEnrollment.semester} · {selectedEnrollment.academicYear}</strong></span></div></section>
      </section></div>}
    </>
  )
}

function initials(name: string) { return name.trim().split(/\s+/).map((part) => part[0]).filter(Boolean).slice(0, 2).join('').toUpperCase() }
function CourseLoadingState({ label }: { label: string }) { return <section className="student-dashboard-main-grid course-offering-grid" role="status" aria-label={label}>{[1, 2, 3].map((item) => <article key={item} className="student-card course-skeleton-card"><i/><i/><i/><i/></article>)}<span className="sr-only">{label}</span></section> }

export function FacultyStudentsPage() {
  const [students, setStudents] = useState<Student[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const nextStudents = await academicService.getFacultyStudents()
      setStudents(nextStudents)
    } catch (cause) {
      setError(getApiErrorMessage(cause))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const request = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(request)
  }, [load])

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>My students</h1>
          <p>Students associated with your assigned courses.</p>
        </div>
      </div>
      {error && <div className="form-error" role="alert">{error}</div>}
      {loading ? (
        <div className="page-loading" role="status"><LoaderCircle className="spin" size={18} /> Loading students…</div>
      ) : students.length === 0 ? (
        <div className="student-empty-card"><span><UsersRound size={24} /></span><h2>No students are assigned to your courses yet.</h2><p>New enrollments will appear here after students register for one of your courses.</p></div>
      ) : (
        <section className="table-scroll">
          <table className="students-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Department</th>
                <th>Semester</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {students.map((student) => (
                <tr key={student.id}>
                  <td>{student.name}</td>
                  <td>{student.email}</td>
                  <td>{student.department || '—'}</td>
                  <td>{student.semester}</td>
                  <td>{student.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </>
  )
}

export function FacultyCoursesPage() {
  const [courses, setCourses] = useState<AcademicCourse[]>([])
  const [selectedCourse, setSelectedCourse] = useState<AcademicCourse | null>(null)
  const [courseStudents, setCourseStudents] = useState<Student[]>([])
  const [studentsLoading, setStudentsLoading] = useState(false)
  const [studentsError, setStudentsError] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setCourses(await academicService.getFacultyCourses())
    } catch (cause) {
      setError(getApiErrorMessage(cause))
    } finally {
      setLoading(false)
    }
  }, [])

  const openCourse = async (course: AcademicCourse) => {
    setSelectedCourse(course)
    setStudentsLoading(true)
    setStudentsError('')
    try { setCourseStudents(await academicService.getFacultyStudents(course.id)) }
    catch (cause) { setStudentsError(getApiErrorMessage(cause)) }
    finally { setStudentsLoading(false) }
  }

  useEffect(() => {
    const request = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(request)
  }, [load])

  return (
    <>
      <div className="page-heading">
        <div><h1>My courses</h1><p>Courses assigned to you for teaching.</p></div>
      </div>
      {error && <div className="form-error" role="alert">{error}</div>}
      {loading ? <div className="page-loading" role="status"><LoaderCircle className="spin" size={18} /> Loading your courses…</div>
        : courses.length === 0 ? <div className="student-empty-card"><span><BookOpenCheck size={24} /></span><h2>No courses assigned yet</h2><p>Your assigned teaching courses will appear here.</p></div>
          : <section className="student-dashboard-main-grid course-offering-grid">{courses.map((course) => <button key={course.id} type="button" className="student-card course-offering-card" onClick={() => void openCourse(course)}><span className="student-eyebrow">{course.courseCode}</span><h2>{course.courseName}</h2><p>{course.department} · Semester {course.semester} · {course.academicYear}</p><p>{course.credits} credits</p><span className="course-card-footer">View enrolled students <ArrowRight size={15}/></span></button>)}</section>}
      {selectedCourse && <div className="course-dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelectedCourse(null) }}><section className="course-dialog faculty-course-dialog" role="dialog" aria-modal="true" aria-labelledby="faculty-course-title"><button className="course-dialog-close" type="button" aria-label="Close course roster" onClick={() => setSelectedCourse(null)}><X size={18}/></button><span className="student-eyebrow">COURSE ROSTER</span><h2 id="faculty-course-title">{selectedCourse.courseName}</h2><p className="course-dialog-code">{selectedCourse.courseCode} · Semester {selectedCourse.semester} · {selectedCourse.academicYear}</p>{studentsError && <div className="form-error" role="alert">{studentsError}</div>}{studentsLoading ? <div className="page-loading" role="status"><LoaderCircle className="spin" size={18}/> Loading enrolled students…</div> : studentsError ? null : courseStudents.length === 0 ? <div className="student-empty-card"><span><UsersRound size={22}/></span><h2>No students enrolled yet</h2><p>Students who select this course will appear here.</p></div> : <div className="faculty-roster-list">{courseStudents.map((student) => <article key={student.id}><span className="faculty-initial-avatar" aria-hidden="true">{initials(student.name)}</span><div><strong>{student.name}</strong><small>{student.email}</small></div><span className="student-secondary">{student.department || 'Student'}</span></article>)}</div>}<Link className="student-button faculty-attendance-link" to={`/attendance?courseId=${selectedCourse.id}`}>Take Attendance <ArrowRight size={15}/></Link></section></div>}
    </>
  )
}

export const CourseCatalogPage = CoursesPage
export const StudentCoursesPage = MyCoursesPage
export const FacultyMyStudentsPage = FacultyStudentsPage
