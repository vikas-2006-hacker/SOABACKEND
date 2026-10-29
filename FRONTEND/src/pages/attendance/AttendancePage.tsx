import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AlertCircle, CalendarCheck2, Check, LoaderCircle, UsersRound } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { academicService } from '../../services/academicService'
import { attendanceService } from '../../services/attendanceService'
import { getApiErrorMessage } from '../../services/api'
import type { AcademicCourse, AttendanceRecord, AttendanceStatus } from '../../types/academic'
import type { Student } from '../../types/student'

type RosterEntry = { student: Student; status: AttendanceStatus | ''; remarks: string }
const attendanceStatuses: AttendanceStatus[] = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED']
const localDate = () => {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
const displayDate = (date: string) => new Date(`${date}T12:00:00`).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })

export function AttendancePage() {
  const { user } = useAuth()
  return user?.role === 'FACULTY' ? <FacultyAttendance /> : <AttendanceOverview />
}

function FacultyAttendance() {
  const [searchParams] = useSearchParams()
  const requestedCourseId = searchParams.get('courseId')
  const [courses, setCourses] = useState<AcademicCourse[]>([])
  const [selectedCourse, setSelectedCourse] = useState<AcademicCourse | null>(null)
  const [date, setDate] = useState(localDate)
  const [roster, setRoster] = useState<RosterEntry[]>([])
  const [history, setHistory] = useState<AttendanceRecord[]>([])
  const [loadingCourses, setLoadingCourses] = useState(true)
  const [loadingRoster, setLoadingRoster] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    academicService.getFacultyCourses().then((items) => {
      if (!active) return
      setCourses(items)
      const requestedId = Number(requestedCourseId)
      const requestedCourse = items.find((item) => item.id === requestedId)
      if (requestedCourse) { setLoadingRoster(true); setSelectedCourse(requestedCourse) }
    }).catch((cause: unknown) => {
      if (active) setError(getApiErrorMessage(cause))
    }).finally(() => { if (active) setLoadingCourses(false) })
    return () => { active = false }
  }, [requestedCourseId])

  useEffect(() => {
    if (!selectedCourse) return
    let active = true
    Promise.all([
      academicService.getFacultyStudents(selectedCourse.id),
      attendanceService.getAttendanceByCourse(selectedCourse.courseCode),
    ]).then(([students, courseRecords]) => {
      if (!active) return
      const courseAttendance = courseRecords.filter((record) => record.courseCode.toLowerCase() === selectedCourse.courseCode.toLowerCase())
      setHistory(courseAttendance)
      const studentIds = new Set(students.map((student) => student.id))
      const currentRecords = courseAttendance.filter((record) => record.date === date && studentIds.has(record.studentId))
      const byStudent = new Map(currentRecords.map((record) => [record.studentId, record]))
      setRoster(students.map((student) => {
        const record = byStudent.get(student.id)
        return { student, status: (record?.status.toUpperCase() as AttendanceStatus | undefined) ?? '', remarks: record?.remarks ?? '' }
      }))
    }).catch((cause: unknown) => {
      if (active) { setError(getApiErrorMessage(cause)); setRoster([]); setHistory([]) }
    }).finally(() => { if (active) setLoadingRoster(false) })
    return () => { active = false }
  }, [selectedCourse, date])

  const counts = useMemo(() => roster.reduce((result, row) => {
    if (row.status) result[row.status] += 1
    return result
  }, { PRESENT: 0, ABSENT: 0, LATE: 0, EXCUSED: 0 }), [roster])

  const historyByDate = useMemo(() => {
    const grouped = new Map<string, AttendanceRecord[]>()
    for (const record of history) grouped.set(record.date, [...(grouped.get(record.date) ?? []), record])
    return [...grouped.entries()].sort(([left], [right]) => right.localeCompare(left))
  }, [history])

  const selectCourse = (courseId: string) => {
    const course = courses.find((item) => item.id === Number(courseId)) ?? null
    setLoadingRoster(Boolean(course))
    setRoster([])
    setHistory([])
    setError('')
    setNotice('')
    setSelectedCourse(course)
  }

  const selectDate = (nextDate: string) => {
    setLoadingRoster(true)
    setError('')
    setNotice('')
    setDate(nextDate)
  }

  const changeEntry = (studentId: number, change: Partial<Pick<RosterEntry, 'status' | 'remarks'>>) => {
    setRoster((current) => current.map((entry) => entry.student.id === studentId ? { ...entry, ...change } : entry))
  }

  const markAllPresent = () => setRoster((current) => current.map((entry) => ({ ...entry, status: 'PRESENT' })))

  const saveAttendance = async () => {
    if (!selectedCourse || roster.length === 0) return
    const unmarked = roster.filter((entry) => !entry.status).length
    if (unmarked) { setError(`Choose an attendance status for all students. ${unmarked} remaining.`); return }
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const saved = await attendanceService.markBulkAttendance({
        courseCode: selectedCourse.courseCode,
        date,
        attendanceRecords: roster.map(({ student, status, remarks }) => ({ studentId: student.id, status: status as AttendanceStatus, remarks })),
      })
      setHistory((current) => [...current.filter((record) => record.date !== date), ...saved])
      setNotice(`Attendance saved for ${saved.length} students.`)
    } catch (cause) { setError(getApiErrorMessage(cause)) }
    finally { setSaving(false) }
  }

  return <main className="faculty-attendance-page">
    <header className="faculty-attendance-heading"><div><span className="student-eyebrow">FACULTY WORKSPACE</span><h1>Attendance</h1><p>Take and review attendance for your assigned courses.</p></div><CalendarCheck2 size={25} aria-hidden="true"/></header>
    <section className="attendance-course-toolbar" aria-label="Attendance course and date">
      <label><span>Select Course</span><select aria-label="Select Course" value={selectedCourse?.id ?? ''} onChange={(event) => selectCourse(event.target.value)} disabled={loadingCourses}><option value="">{loadingCourses ? 'Loading assigned courses…' : 'Choose a course'}</option>{courses.map((course) => <option key={course.id} value={course.id}>{course.courseName} — {course.courseCode}</option>)}</select></label>
      <label><span>Select Date</span><input aria-label="Select Date" type="date" value={date} onChange={(event) => selectDate(event.target.value)}/></label>
    </section>
    {error && <div className="faculty-attendance-alert" role="alert"><AlertCircle size={17}/><span>{error}</span>{!loadingCourses && courses.length === 0 && <button type="button" onClick={() => window.location.reload()}>Try again</button>}</div>}
    {notice && <p className="faculty-attendance-notice" role="status"><Check size={16}/>{notice}</p>}
    {loadingCourses ? <div className="page-loading" role="status"><LoaderCircle className="spin" size={19}/> Loading your assigned courses…</div>
      : courses.length === 0 ? <div className="student-empty-card"><span><CalendarCheck2 size={24}/></span><h2>No courses assigned</h2><p>Your assigned courses will appear here when they are available.</p></div>
      : !selectedCourse ? <div className="student-empty-card attendance-prompt"><span><UsersRound size={24}/></span><h2>Select a course to begin</h2><p>Your active enrollment roster will load automatically.</p></div>
      : <>
        <section className="attendance-class-summary">
          <div className="attendance-class-title"><span className="student-eyebrow">CLASS ROSTER</span><h2>{selectedCourse.courseName}</h2><p>{selectedCourse.courseCode} · Semester {selectedCourse.semester} · {selectedCourse.academicYear}</p></div>
          <div className="attendance-summary-metrics"><span><strong>{roster.length}</strong><small>Students</small></span><span className="present"><strong>{counts.PRESENT}</strong><small>Present</small></span><span className="absent"><strong>{counts.ABSENT}</strong><small>Absent</small></span><span className="late"><strong>{counts.LATE}</strong><small>Late</small></span></div>
        </section>
        <section className="attendance-roster-panel">
          <header className="attendance-roster-toolbar"><div><h2>Attendance sheet</h2><p>{displayDate(date)} · {roster.length} enrolled students</p></div><button type="button" className="attendance-mark-all" onClick={markAllPresent} disabled={loadingRoster || roster.length === 0}><Check size={15}/>Mark All Present</button></header>
          {loadingRoster ? <div className="page-loading" role="status"><LoaderCircle className="spin" size={19}/> Loading enrolled students and attendance…</div>
            : roster.length === 0 ? <div className="attendance-roster-empty"><UsersRound size={20}/><p>No active students are enrolled in this course.</p></div>
            : <div className="attendance-roster-list"><div className="attendance-roster-labels"><span>Student</span><span>Student ID</span><span>Attendance</span><span>Remarks</span></div>{roster.map(({ student, status, remarks }) => <article className="attendance-roster-row" key={student.id}>
              <div className="attendance-student-name"><span className="faculty-initial-avatar" aria-hidden="true">{student.name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()}</span><strong>{student.name}</strong></div><span className="attendance-student-id">#{student.id}</span>
              <div className="attendance-status-controls" role="group" aria-label={`Attendance for ${student.name}`}>{attendanceStatuses.map((item) => <button key={item} type="button" aria-pressed={status === item} className={`attendance-status-option ${item.toLowerCase()} ${status === item ? 'selected' : ''}`} onClick={() => changeEntry(student.id, { status: item })}>{item.charAt(0) + item.slice(1).toLowerCase()}</button>)}</div>
              <input className="attendance-remarks-input" aria-label={`Remarks for ${student.name}`} value={remarks} onChange={(event) => changeEntry(student.id, { remarks: event.target.value })} placeholder="Optional"/>
            </article>)}</div>}
          <footer className="attendance-save-footer"><span>{roster.length - Object.values(counts).reduce((sum, count) => sum + count, 0)} students need a status</span><button className="primary-action" type="button" onClick={() => void saveAttendance()} disabled={saving || loadingRoster || !roster.length || Object.values(counts).reduce((sum, count) => sum + count, 0) !== roster.length}>{saving ? <LoaderCircle className="spin" size={17}/> : <Check size={17}/>}Save Attendance — {roster.length} Students</button></footer>
        </section>
        <section className="attendance-history-panel"><header><div><h2>Attendance history</h2><p>Previous attendance sheets for this course.</p></div></header>{historyByDate.length === 0 ? <p className="attendance-history-empty">No attendance history for this course yet.</p> : <div className="attendance-history-list">{historyByDate.map(([historyDate, records]) => {
          const present = records.filter((record) => record.status.toUpperCase() === 'PRESENT').length
          const absent = records.filter((record) => record.status.toUpperCase() === 'ABSENT').length
          const late = records.filter((record) => record.status.toUpperCase() === 'LATE').length
          const percent = records.length ? Math.round((present / records.length) * 100) : 0
          return <button type="button" className={`attendance-history-item ${date === historyDate ? 'selected' : ''}`} key={historyDate} onClick={() => setDate(historyDate)}><strong>{displayDate(historyDate)}</strong><span>{records.length} Students · {present} Present · {absent} Absent · {late} Late</span><span className="attendance-history-percent">{percent}%</span></button>
        })}</div>}</section>
      </>}
  </main>
}

function AttendanceOverview() {
  const [records, setRecords] = useState<AttendanceRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    try { setRecords(await attendanceService.getAttendance()) }
    catch (cause) { setError(getApiErrorMessage(cause)) }
    finally { setLoading(false) }
  }, [])
  useEffect(() => {
    let active = true
    attendanceService.getAttendance().then((items) => { if (active) setRecords(items) })
      .catch((cause: unknown) => { if (active) setError(getApiErrorMessage(cause)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])
  return <>
    <div className="page-heading"><div><h1>Attendance</h1><p>Review attendance records.</p></div></div>
    {error && <div className="form-error" role="alert">{error}<button type="button" className="secondary-action" onClick={() => { setLoading(true); void load() }}>Try again</button></div>}
    <section className="record-panel attendance-list"><div className="record-heading"><div className="metric-icon"><CalendarCheck2 size={18}/></div><div><h2>Attendance records</h2><p>Attendance submitted by faculty</p></div></div>
      {loading ? <div className="page-loading" role="status"><LoaderCircle className="spin" size={20}/> Loading attendance…</div> : records.length === 0 ? <p className="record-empty">No attendance records are available.</p> : <div className="table-scroll"><table className="students-table"><thead><tr><th>Student ID</th><th>Course</th><th>Date</th><th>Status</th><th>Remarks</th></tr></thead><tbody>{records.map((record) => <tr key={record.id}><td>#{record.studentId}</td><td>{record.courseCode}</td><td>{record.date}</td><td>{record.status}</td><td>{record.remarks || '—'}</td></tr>)}</tbody></table></div>}
    </section>
  </>
}
