import { useCallback, useEffect, useMemo, useState } from 'react'
import { Activity, AlertCircle, CalendarDays, Check, CircleAlert, GraduationCap, RotateCw, TrendingUp } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { getApiErrorMessage } from '../../services/api'
import { attendanceService, type AttendanceSummary } from '../../services/attendanceService'
import type { AttendanceRecord } from '../../types/academic'

type CourseStats = { code: string; present: number; absent: number; total: number; rate: number }
const isAttended = (status: string) => ['PRESENT', 'LATE'].includes(status.toUpperCase())
const formatDate = (value: string) => new Date(`${value}T12:00:00`).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })

export function MyAttendancePage() {
  const { currentStudent, currentStudentError, refreshCurrentStudent } = useAuth()
  const [records, setRecords] = useState<AttendanceRecord[] | null>(null)
  const [summary, setSummary] = useState<AttendanceSummary | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const load = useCallback(async () => {
    if (!currentStudent) { setLoading(false); return }
    setLoading(true); setError('')
    try {
      const [ownRecords, attendanceSummary] = await Promise.all([
        attendanceService.getStudentAttendance(currentStudent.id),
        attendanceService.getStudentSummary(currentStudent.id),
      ])
      setRecords(ownRecords.filter((record) => record.studentId === currentStudent.id).sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id))
      setSummary(attendanceSummary)
    } catch (cause) { setError(getApiErrorMessage(cause)); setRecords(null); setSummary(null) }
    finally { setLoading(false) }
  }, [currentStudent])
  useEffect(() => { const request = window.setTimeout(() => { if (currentStudent) void load() }, 0); return () => window.clearTimeout(request) }, [currentStudent, load])

  const stats = useMemo(() => {
    const rows = records ?? []
    const present = summary?.classesAttended ?? rows.filter((record) => isAttended(record.status)).length
    const absent = summary?.classesAbsent ?? rows.filter((record) => record.status.toUpperCase() === 'ABSENT').length
    const total = summary?.totalClasses ?? rows.length
    const courses = new Map<string, AttendanceRecord[]>()
    rows.forEach((record) => courses.set(record.courseCode, [...(courses.get(record.courseCode) ?? []), record]))
    const courseStats: CourseStats[] = [...courses].map(([code, courseRows]) => {
      const coursePresent = courseRows.filter((row) => isAttended(row.status)).length
      const courseAbsent = courseRows.filter((row) => row.status.toUpperCase() === 'ABSENT').length
      return { code, present: coursePresent, absent: courseAbsent, total: courseRows.length, rate: Math.round(coursePresent * 100 / courseRows.length) }
    }).sort((a, b) => a.code.localeCompare(b.code))
    const preciseRate = summary?.preciseAttendancePercentage ?? summary?.attendancePercentage ?? (total ? present * 100 / total : 0)
    return { present, absent, total, preciseRate, rate: Math.round(preciseRate), courses: courseStats }
  }, [records, summary])

  const level = summary?.attendanceStatus ?? 'INSUFFICIENT_DATA'
  const levelLabel = level.charAt(0) + level.slice(1).toLowerCase()
  const levelClass = level === 'CRITICAL' || level === 'WARNING' ? 'attention' : level.toLowerCase()
  const threshold = summary?.requiredAttendanceThreshold ?? 0
  const classesNeeded = stats.total && stats.preciseRate < threshold && threshold < 100
    ? Math.ceil(((threshold / 100) * stats.total - stats.present) / (1 - threshold / 100))
    : 0
  const message = level === 'EXCELLENT' ? "You're maintaining excellent attendance."
    : level === 'GOOD' ? 'Your attendance is healthy. Keep attending regularly.'
      : level === 'WARNING' ? 'Your attendance is in the warning range. Keep attending consistently.'
        : `Your attendance is below the ${threshold}% requirement. Attend upcoming classes consistently.`

  if (loading) return <AttendanceSkeleton />
  if (error || currentStudentError || !currentStudent) return <main className="attendance-page"><header className="attendance-page-heading"><span className="attendance-eyebrow"><GraduationCap size={15}/> STUDENT CENTER</span><h1>My Attendance</h1><p>Track your attendance, understand your consistency, and stay on top of your classes.</p></header><section className="attendance-error-card" role="alert"><span><AlertCircle size={22}/></span><div><h2>Unable to load attendance</h2><p>Something went wrong while retrieving your attendance.</p>{(error || currentStudentError) && <small>{error || currentStudentError}</small>}</div><button className="secondary-action" type="button" onClick={() => currentStudent ? void load() : void refreshCurrentStudent?.()}><RotateCw size={16}/> Try Again</button></section></main>
  if (!records?.length) return <main className="attendance-page"><header className="attendance-page-heading"><span className="attendance-eyebrow"><GraduationCap size={15}/> STUDENT CENTER</span><h1>My Attendance</h1><p>Track your attendance, understand your consistency, and stay on top of your classes.</p></header><section className="attendance-empty"><span><CalendarDays size={25}/></span><h2>No attendance records yet</h2><p>Your attendance records will appear here once your faculty records your classes.</p></section></main>

  return <main className="attendance-page">
    <header className="attendance-page-heading"><span className="attendance-eyebrow"><GraduationCap size={15}/> STUDENT CENTER</span><h1>Your Attendance</h1><p>Stay on track with your academic attendance.</p><div className="attendance-student-meta"><strong>{currentStudent.name}</strong><span>{currentStudent.academicYear || currentStudent.semester ? [currentStudent.academicYear, currentStudent.semester ? `Semester ${currentStudent.semester}` : ''].filter(Boolean).join(' · ') : 'Academic term not available'}</span></div></header>
    <section className="attendance-score-card"><div className="attendance-score-copy"><span className="attendance-eyebrow">YOUR ATTENDANCE OVERVIEW</span><h2>Overall Attendance</h2><p>Your consistency across recorded classes.</p><span className={`attendance-level ${levelClass}`}><i/>{levelLabel}</span><p className="attendance-motivation">{message}</p></div><div className="attendance-ring" style={{ '--attendance-progress': `${stats.preciseRate}%` } as React.CSSProperties} role="img" aria-label={`Overall attendance ${stats.rate}%`}><div><strong>{stats.rate}%</strong><span>attendance rate</span></div></div><div className="attendance-score-breakdown"><div><span className="dot present-dot"/>Present<strong>{stats.present}</strong></div><div><span className="dot absent-dot"/>Absent<strong>{stats.absent}</strong></div><div><span className="dot total-dot"/>Total classes<strong>{stats.total}</strong></div></div></section>
    <section className="attendance-stat-grid" aria-label="Attendance statistics"><Stat icon={<Check/>} title="Present" value={stats.present} note="Classes attended" tone="green"/><Stat icon={<CircleAlert/>} title="Absent" value={stats.absent} note="Classes missed" tone="rose"/><Stat icon={<CalendarDays/>} title="Total Classes" value={stats.total} note="Recorded sessions" tone="blue"/><Stat icon={<TrendingUp/>} title="Attendance Rate" value={`${stats.rate}%`} note="Overall consistency" tone="violet"/></section>
    <section className="attendance-panel"><PanelTitle icon={<Activity/>} title="Course Attendance" detail={`${stats.courses.length} ${stats.courses.length === 1 ? 'course' : 'courses'} with recorded attendance`}/>{stats.courses.length ? <div className="attendance-course-grid">{stats.courses.map((course) => <article className="attendance-course-card" key={course.code}><div className="attendance-course-title"><span>{course.code.slice(0, 2)}</span><div><strong>{course.code}</strong><small>{course.present} attended · {course.absent} absent</small></div><b>{course.rate}%</b></div><div className="attendance-progress"><span style={{ width: `${course.rate}%` }}/></div><small>{course.present} present of {course.total} recorded classes</small></article>)}</div> : <p className="attendance-panel-empty">Course attendance will appear as records are added.</p>}</section>
    <section className="attendance-calculator"><span><TrendingUp size={18}/></span><div><small>ATTENDANCE CALCULATOR</small><h2>{classesNeeded ? `Attend the next ${classesNeeded} ${classesNeeded === 1 ? 'class' : 'classes'} to reach ${threshold}%.` : `You currently meet the ${threshold}% attendance requirement.`}</h2></div><div className="calculator-metrics"><div><small>Current</small><strong>{stats.rate}%</strong></div><div><small>Required</small><strong>{threshold}%</strong></div><div><small>Classes needed</small><strong>{classesNeeded}</strong></div></div></section>
    <section className="attendance-panel"><PanelTitle icon={<CalendarDays/>} title="Recent Attendance" detail="Your latest recorded classes"/><div className="attendance-table-wrap"><table className="attendance-table"><thead><tr><th>Date</th><th>Course</th><th>Status</th><th>Remarks</th></tr></thead><tbody>{records.slice(0, 10).map((record) => <tr key={record.id}><td>{formatDate(record.date)}</td><td><strong>{record.courseCode}</strong></td><td><span className={`attendance-status status-${record.status.toLowerCase()}`}><i/>{record.status}</span></td><td>{record.remarks || '—'}</td></tr>)}</tbody></table></div></section>
    <aside className="attendance-insight"><span><Activity size={19}/></span><div><small>ATTENDANCE STATUS · {level}</small><h2>{message}</h2></div><strong>{stats.rate}%</strong></aside>
  </main>
}

function Stat({ icon, title, value, note, tone }: { icon: React.ReactNode; title: string; value: number | string; note: string; tone: string }) { return <article className="attendance-stat"><span className={`attendance-stat-icon ${tone}`}>{icon}</span><small>{title}</small><strong>{value}</strong><span>{note}</span></article> }
function PanelTitle({ icon, title, detail }: { icon: React.ReactNode; title: string; detail: string }) { return <div className="attendance-panel-heading"><span>{icon}</span><div><h2>{title}</h2><p>{detail}</p></div></div> }

function AttendanceSkeleton() { return <main className="attendance-page" aria-label="Loading attendance" role="status"><div className="attendance-skeleton-heading"><i/><i/><i/></div><div className="attendance-skeleton-score"/><div className="attendance-skeleton-stats">{[0,1,2,3].map((item) => <i key={item}/>)}</div><div className="attendance-skeleton-lower"><i/><i/></div><span className="sr-only">Loading your attendance</span></main> }
