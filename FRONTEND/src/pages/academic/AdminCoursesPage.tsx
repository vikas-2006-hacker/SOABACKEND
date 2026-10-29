import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { BookOpenCheck, Check, GraduationCap, LoaderCircle, Pencil, Plus, Search, UserRoundPlus, UsersRound, X } from 'lucide-react'
import { Toast } from '../../components/common/Toast'
import { academicService } from '../../services/academicService'
import { facultyService } from '../../services/facultyService'
import { getApiErrorMessage } from '../../services/api'
import type { AcademicCourse, FacultyCourseAssignment } from '../../types/academic'
import type { Faculty } from '../../services/facultyService'

interface CourseForm {
  courseCode: string
  courseName: string
  department: string
  credits: string
  semester: string
  academicYear: string
  description: string
  active: boolean
}

interface CourseAdminData {
  course: AcademicCourse
  faculty: FacultyCourseAssignment[]
  enrollments: number
}

const blankCourse: CourseForm = {
  courseCode: '',
  courseName: '',
  department: '',
  credits: '',
  semester: '',
  academicYear: '',
  description: '',
  active: true,
}

export function AdminCoursesPage() {
  const [courses, setCourses] = useState<CourseAdminData[]>([])
  const [faculty, setFaculty] = useState<Faculty[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')
  const [editing, setEditing] = useState<AcademicCourse | null>(null)
  const [courseDialogOpen, setCourseDialogOpen] = useState(false)
  const [assignmentCourse, setAssignmentCourse] = useState<AcademicCourse | null>(null)
  const [selectedFaculty, setSelectedFaculty] = useState('')
  const [form, setForm] = useState<CourseForm>(blankCourse)
  const [dialogError, setDialogError] = useState('')
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [courseRows, facultyRows, enrollments] = await Promise.all([
        academicService.getCourses(),
        facultyService.list(),
        academicService.listAllEnrollments(),
      ])
      const facultyAssignments = await Promise.all(
        courseRows.map(async (course) => [course.id, await academicService.getCourseFaculty(course.id)] as const),
      )
      const assignmentsByCourse = new Map(facultyAssignments)
      setCourses(courseRows.map((course) => ({
        course,
        faculty: assignmentsByCourse.get(course.id) ?? [],
        enrollments: enrollments.filter((enrollment) => enrollment.courseId === course.id && enrollment.status === 'ACTIVE').length,
      })))
      setFaculty(facultyRows.filter((member) => member.status === 'ACTIVE'))
    } catch {
      setError('Unable to load courses. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const request = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(request)
  }, [load])

  const activeCount = courses.filter(({ course }) => course.active).length
  const enrollmentCount = courses.reduce((total, row) => total + row.enrollments, 0)
  const filteredCourses = useMemo(() => courses.filter(({ course, faculty: assignments }) => {
    const matchesStatus = statusFilter === 'ALL' || (statusFilter === 'ACTIVE' ? course.active : !course.active)
    const query = search.trim().toLowerCase()
    const matchesSearch = !query || [
      course.courseCode,
      course.courseName,
      course.department,
      ...assignments.map((assignment) => faculty.find((member) => member.email.toLowerCase() === assignment.facultyEmail.toLowerCase())?.name ?? assignment.facultyEmail),
    ].some((value) => value.toLowerCase().includes(query))
    return matchesStatus && matchesSearch
  }), [courses, faculty, search, statusFilter])

  const openCreate = () => {
    setEditing(null)
    setForm(blankCourse)
    setDialogError('')
    setCourseDialogOpen(true)
  }

  const openEdit = (course: AcademicCourse) => {
    setEditing(course)
    setForm({
      courseCode: course.courseCode,
      courseName: course.courseName,
      department: course.department,
      credits: String(course.credits),
      semester: String(course.semester),
      academicYear: course.academicYear ?? '',
      description: course.description ?? '',
      active: course.active,
    })
    setDialogError('')
    setCourseDialogOpen(true)
  }

  const closeCourseDialog = () => {
    if (saving) return
    setCourseDialogOpen(false)
    setEditing(null)
    setDialogError('')
  }

  const saveCourse = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setDialogError('')
    const payload = {
      courseCode: form.courseCode.trim(),
      courseName: form.courseName.trim(),
      department: form.department.trim(),
      credits: Number(form.credits),
      semester: Number(form.semester),
      academicYear: form.academicYear.trim(),
      description: form.description.trim(),
      active: form.active,
    }
    if (!payload.courseCode || !payload.courseName || !payload.department || !payload.academicYear
      || !Number.isInteger(payload.credits) || payload.credits < 1
      || !Number.isInteger(payload.semester) || payload.semester < 1) {
      setDialogError('Complete all required fields with valid credits and semester values.')
      return
    }
    setSaving(true)
    try {
      if (editing) await academicService.updateCourse(editing.id, payload)
      else await academicService.createCourse(payload)
      setCourseDialogOpen(false)
      setEditing(null)
      setNotice(editing ? 'Course updated successfully' : 'Course created successfully')
      await load()
    } catch (cause: unknown) {
      setDialogError(getApiErrorMessage(cause, 'course'))
    } finally {
      setSaving(false)
    }
  }

  const toggleStatus = async (course: AcademicCourse) => {
    try {
      await academicService.updateCourse(course.id, { active: !course.active })
      setNotice(`Course ${course.active ? 'deactivated' : 'activated'} successfully`)
      await load()
    } catch (cause: unknown) {
      setError(getApiErrorMessage(cause, 'course'))
    }
  }

  const openAssignment = (course: AcademicCourse) => {
    setAssignmentCourse(course)
    setSelectedFaculty('')
    setDialogError('')
  }

  const assignFaculty = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!assignmentCourse || !selectedFaculty) {
      setDialogError('Select an active faculty member.')
      return
    }
    setSaving(true)
    setDialogError('')
    try {
      await academicService.assignFacultyToCourse({
        courseId: assignmentCourse.id,
        facultyEmail: selectedFaculty,
        academicYear: assignmentCourse.academicYear,
        semester: assignmentCourse.semester,
      })
      setAssignmentCourse(null)
      setNotice('Faculty assigned successfully')
      await load()
    } catch (cause: unknown) {
      setDialogError(getApiErrorMessage(cause, 'course'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="admin-courses-page">
      <header className="admin-courses-hero glass-panel">
        <div>
          <span className="admin-courses-kicker"><GraduationCap size={15} /> ACADEMIC OPERATIONS</span>
          <h1>Course Management</h1>
          <p>Create courses, manage academic offerings, and assign faculty.</p>
        </div>
        <button className="primary-action" type="button" onClick={openCreate}><Plus size={17} /> Create Course</button>
      </header>

      <section className="admin-course-stats" aria-label="Course statistics">
        <article className="admin-course-stat glass-panel">
          <span className="admin-course-stat-icon"><BookOpenCheck size={18} /></span>
          <strong>{loading ? '—' : courses.length}</strong><span>Courses</span>
        </article>
        <article className="admin-course-stat glass-panel">
          <span className="admin-course-stat-icon active"><Check size={18} /></span>
          <strong>{loading ? '—' : activeCount}</strong><span>Active</span>
        </article>
        <article className="admin-course-stat glass-panel">
          <span className="admin-course-stat-icon enrolled"><UsersRound size={18} /></span>
          <strong>{loading ? '—' : enrollmentCount}</strong><span>Active enrollments</span>
        </article>
      </section>

      <section className="admin-course-library glass-panel" aria-label="Course library">
        <div className="admin-course-toolbar">
          <div>
            <h2>Academic offerings</h2>
            <p>Manage course details, availability, and faculty ownership.</p>
          </div>
          <div className="admin-course-filters">
            <label className="admin-course-search"><Search size={16} /><input aria-label="Search courses" type="search" placeholder="Search courses or faculty" value={search} onChange={(event) => setSearch(event.target.value)} /></label>
            <label className="sr-only" htmlFor="course-status-filter">Filter courses by status</label>
            <select id="course-status-filter" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as typeof statusFilter)}>
              <option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option>
            </select>
          </div>
        </div>

        {error ? (
          <div className="admin-course-error" role="alert">
            <strong>Unable to load courses</strong><p>Please try again.</p>
            <button className="secondary-action" type="button" onClick={() => void load()}>Retry</button>
          </div>
        ) : loading ? (
          <div className="admin-course-loading" role="status"><LoaderCircle className="spin" size={20} /> Loading courses…</div>
        ) : courses.length === 0 ? (
          <div className="admin-course-empty"><span><BookOpenCheck size={23} /></span><h3>No courses yet</h3><p>Create an academic offering to get started.</p><button className="secondary-action" type="button" onClick={openCreate}><Plus size={15} /> Create Course</button></div>
        ) : filteredCourses.length === 0 ? (
          <div className="admin-course-empty"><h3>No matching courses</h3><p>Try a different search or status filter.</p></div>
        ) : (
          <div className="admin-course-grid">
            {filteredCourses.map(({ course, faculty: assignments, enrollments: count }) => {
              const assignedFaculty = assignments.map((assignment) => faculty.find((member) => member.email.toLowerCase() === assignment.facultyEmail.toLowerCase())?.name ?? assignment.facultyEmail)
              return (
                <article className="admin-course-card" key={course.id}>
                  <div className="admin-course-card-top">
                    <div className="admin-course-monogram"><BookOpenCheck size={18} /></div>
                    <span className={`admin-course-status ${course.active ? 'is-active' : 'is-inactive'}`}><i />{course.active ? 'Active' : 'Inactive'}</span>
                  </div>
                  <div className="admin-course-title"><span>{course.courseCode}</span><h3>{course.courseName}</h3></div>
                  <div className="admin-course-facts"><span>{course.department}</span><span>{course.credits} Credits</span><span>Semester {course.semester}</span><span>{course.academicYear}</span></div>
                  <div className="admin-course-assignment">
                    <span className="admin-course-assignment-icon"><UserRoundPlus size={16} /></span>
                    <div><small>Assigned faculty</small><strong>{assignedFaculty.length ? assignedFaculty.join(', ') : 'No faculty assigned yet'}</strong></div>
                  </div>
                  <div className="admin-course-card-footer">
                    <span><UsersRound size={15} /> {count} {count === 1 ? 'Student' : 'Students'}</span>
                    <div className="admin-course-actions">
                      <button type="button" className="secondary-action" onClick={() => openEdit(course)}><Pencil size={14} /> Manage</button>
                      <button type="button" className="secondary-action" onClick={() => openAssignment(course)}>Assign Faculty</button>
                    </div>
                  </div>
                  <button type="button" className="admin-course-status-toggle" onClick={() => void toggleStatus(course)}>{course.active ? 'Deactivate course' : 'Activate course'}</button>
                </article>
              )
            })}
          </div>
        )}
      </section>

      {courseDialogOpen && (
        <div className="admin-course-modal-backdrop">
          <section className="admin-course-modal glass-modal" role="dialog" aria-modal="true" aria-labelledby="admin-course-form-title">
            <div className="admin-course-modal-heading"><div><span className="admin-courses-kicker">COURSE OFFERING</span><h2 id="admin-course-form-title">{editing ? 'Manage course' : 'Create course'}</h2></div><button className="icon-action" type="button" aria-label="Close course form" onClick={closeCourseDialog}><X size={18} /></button></div>
            <p className="admin-course-modal-copy">Course details are shared with students when the offering is active.</p>
            <form onSubmit={(event) => void saveCourse(event)}>
              <div className="admin-course-form-grid">
                <label>Course code<input required maxLength={64} value={form.courseCode} onChange={(event) => setForm({ ...form, courseCode: event.target.value })} placeholder="e.g. DS101" /></label>
                <label>Course name<input required maxLength={160} value={form.courseName} onChange={(event) => setForm({ ...form, courseName: event.target.value })} placeholder="e.g. Data Structures" /></label>
                <label>Department<input required maxLength={120} value={form.department} onChange={(event) => setForm({ ...form, department: event.target.value })} placeholder="e.g. CSE" /></label>
                <label>Credits<input required type="number" min="1" step="1" value={form.credits} onChange={(event) => setForm({ ...form, credits: event.target.value })} /></label>
                <label>Semester<input required type="number" min="1" step="1" value={form.semester} onChange={(event) => setForm({ ...form, semester: event.target.value })} /></label>
                <label>Academic year<input required maxLength={32} value={form.academicYear} onChange={(event) => setForm({ ...form, academicYear: event.target.value })} placeholder="e.g. 2026-2027" /></label>
                <label className="admin-course-description">Description<textarea rows={3} maxLength={2000} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
                <label className="admin-course-active-check"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /> Active offering</label>
              </div>
              {dialogError && <p className="dialog-error" role="alert">{dialogError}</p>}
              <div className="dialog-actions"><button type="button" className="secondary-action" disabled={saving} onClick={closeCourseDialog}>Cancel</button><button type="submit" className="primary-action" disabled={saving}>{saving ? <LoaderCircle className="spin" size={16} /> : <Check size={16} />}{saving ? 'Saving…' : editing ? 'Save changes' : 'Create Course'}</button></div>
            </form>
          </section>
        </div>
      )}

      {assignmentCourse && (
        <div className="admin-course-modal-backdrop">
          <section className="admin-course-modal glass-modal" role="dialog" aria-modal="true" aria-labelledby="assign-faculty-title">
            <div className="admin-course-modal-heading"><div><span className="admin-courses-kicker">FACULTY OWNERSHIP</span><h2 id="assign-faculty-title">Assign faculty</h2></div><button className="icon-action" type="button" aria-label="Close faculty assignment" onClick={() => setAssignmentCourse(null)}><X size={18} /></button></div>
            <p className="admin-course-modal-copy">Choose an active faculty account for {assignmentCourse.courseName} ({assignmentCourse.courseCode}).</p>
            <form onSubmit={(event) => void assignFaculty(event)}>
              <label className="admin-course-select-label">Available faculty
                <select required value={selectedFaculty} onChange={(event) => setSelectedFaculty(event.target.value)}>
                  <option value="">Select active faculty</option>
                  {faculty.map((member) => <option key={member.id} value={member.email}>{member.name} · {member.department}</option>)}
                </select>
              </label>
              {faculty.length === 0 && <p className="admin-course-modal-copy">There are no active faculty accounts available to assign.</p>}
              {dialogError && <p className="dialog-error" role="alert">{dialogError}</p>}
              <div className="dialog-actions"><button type="button" className="secondary-action" disabled={saving} onClick={() => setAssignmentCourse(null)}>Cancel</button><button type="submit" className="primary-action" disabled={saving || faculty.length === 0}>{saving ? <LoaderCircle className="spin" size={16} /> : <UserRoundPlus size={16} />}{saving ? 'Assigning…' : 'Confirm assignment'}</button></div>
            </form>
          </section>
        </div>
      )}

      {notice && <Toast message={notice} onDismiss={() => setNotice('')} />}
    </main>
  )
}
