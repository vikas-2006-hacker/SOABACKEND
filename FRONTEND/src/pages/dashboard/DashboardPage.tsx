import { useCallback, useEffect, useState } from 'react'
import { Activity, ArrowRight, Award, BookOpenCheck, CalendarCheck2, GraduationCap, Info, LoaderCircle, Sparkles, TrendingUp, UserRound, UsersRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { getApiErrorMessage } from '../../services/api'
import { attendanceService } from '../../services/attendanceService'
import { resultService } from '../../services/resultService'
import { academicInsightsService } from '../../services/academicInsightsService'
import { academicService } from '../../services/academicService'
import type { AcademicInsightsResponse } from '../../types/insights'
import type { AttendanceRecord, ResultRecord, StudentCourseEnrollmentView } from '../../types/academic'
import { studentService } from '../../services/studentService'
import { facultyService, type Faculty } from '../../services/facultyService'
import type { Student } from '../../types/student'
import { AnimatedNumber } from '../../components/common/AnimatedNumber'

interface DashboardMetrics { students: number | null; activeStudents: number | null; faculty: number | null; activeFaculty: number | null; attendanceToday: number | null; results: number | null }
interface StudentDashboardData { academic: AcademicInsightsResponse; attendanceRecords: AttendanceRecord[]; results: ResultRecord[] }
interface DisplayInsight { title: string; detail: string; tone: 'positive' | 'attention' | 'neutral' }
const emptyMetrics: DashboardMetrics = { students: null, activeStudents: null, faculty: null, activeFaculty: null, attendanceToday: null, results: null }

export function DashboardPage() {
  const { user } = useAuth()
  return user?.role === 'STUDENT' ? <StudentDashboard/> : <StaffDashboard/>
}

function StudentDashboard() {
  const { currentStudent, currentStudentError, refreshCurrentStudent } = useAuth()
  const [data, setData] = useState<StudentDashboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [myCourses, setMyCourses] = useState<StudentCourseEnrollmentView[]>([])
  const load = useCallback(async () => {
    if (!currentStudent) return
    setLoading(true); setError('')
    void academicService.getMyEnrollments().then(setMyCourses).catch(() => setMyCourses([]))
    try {
      const [academic, attendanceRecords, results] = await Promise.all([
        academicInsightsService.getMyInsights(), attendanceService.getStudentAttendance(currentStudent.id), resultService.getStudentResults(currentStudent.id),
      ])
      const ownResults = results.filter((result) => result.studentId === currentStudent.id
        && ['PASS', 'FAIL'].includes((result.resultStatus ?? '').toUpperCase()))
      setData({ academic, attendanceRecords: attendanceRecords.filter((record) => record.studentId === currentStudent.id).sort((a, b) => b.date.localeCompare(a.date)).slice(0, 4), results: [...ownResults].sort((a, b) => b.semester - a.semester || b.id - a.id) })
    } catch (cause) { setError(getApiErrorMessage(cause)); setData(null) }
    finally { setLoading(false) }
  }, [currentStudent])
  useEffect(() => { const request = window.setTimeout(() => { if (currentStudent) void load() }, 0); return () => window.clearTimeout(request) }, [currentStudent, load])

  const firstName = currentStudent?.name.trim().split(/\s+/)[0] || 'there'
  const initials = currentStudent?.name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'S'
  const resultCount = data?.academic.performance?.resultsCount ?? data?.results.length ?? 0
  const attendance = data?.academic.attendance
  const performance = data?.academic.performance
  const standing = performance?.standing
  if ((loading && !currentStudentError) || (!currentStudent && !currentStudentError)) return <StudentDashboardSkeleton/>
  if (error || currentStudentError || !currentStudent) return <main className="student-portal-page"><StudentPageHeading eyebrow="YOUR LEARNING SPACE" title="Your dashboard" subtitle="A clear view of your academic journey."/><section className="student-state-card" role="alert"><span><Info size={20}/></span><div><h2>Unable to load your academic overview</h2><p>Something went wrong while retrieving your student information.</p></div><button className="student-button" type="button" onClick={() => currentStudent ? void load() : void refreshCurrentStudent?.()}>Try again <ArrowRight size={15}/></button></section></main>

  const latest = data?.results.slice(0, 4) ?? []
  const hasOverview = Boolean(data && ((attendance?.totalClasses ?? 0) > 0 || (performance?.creditsRegistered ?? 0) > 0 || data.results.length > 0 || myCourses.length > 0))
  const attendanceRate = attendance?.percentage ?? 0
  const insights: DisplayInsight[] = data?.academic.insights.map((insight) => ({
    title: insight.title,
    detail: insight.detail,
    tone: insight.severity === 'POSITIVE' ? 'positive' : ['HIGH', 'MODERATE', 'ATTENTION'].includes(insight.severity) ? 'attention' : 'neutral',
  })) ?? []
  return <main className="student-portal-page student-dashboard-page">
    <section className="student-welcome-hero"><div className="student-welcome-copy"><span className="student-eyebrow"><Sparkles size={14}/> CAMPUSCORE · STUDENT PORTAL</span><h1>Good {getTimeGreeting()}, {firstName}</h1><p>Here’s your academic journey at a glance.</p><div className="student-welcome-meta"><span>{currentStudent.academicYear || 'Academic year not available'}</span>{currentStudent.semester ? <span>Semester {currentStudent.semester}</span> : null}<span className={`student-presence ${currentStudent.status === 'ACTIVE' ? 'active' : ''}`}>{currentStudent.status || 'Status not available'}</span></div></div><div className="student-welcome-art"><div className="student-avatar-large">{initials}</div><span className="welcome-orbit orbit-one"/><span className="welcome-orbit orbit-two"/><GraduationCap className="welcome-cap" size={29}/></div><div className="student-welcome-footer"><span><Activity size={15}/> YOUR PERSONAL ACADEMIC SPACE</span><span>STUDENT ID · {currentStudent.id}</span></div></section>
    {!hasOverview ? <><section className="student-empty-card"><span><BookOpenCheck size={24}/></span><h2>Your academic journey starts here</h2><p>Attendance and results will appear as your faculty records classes and publishes grades.</p><Link className="student-button" to="/my-profile">Review your profile <ArrowRight size={15}/></Link></section><DashboardMyCourses courses={myCourses}/><AcademicInsights insights={insights} unavailableSources={data?.academic.unavailableSources ?? []}/></> : <>
      <div className="student-section-heading"><div><span className="student-eyebrow">AT A GLANCE</span><h2>Your overview</h2></div><span className="student-muted-caption">Live academic data</span></div>
      <section className="student-dashboard-metrics" aria-label="Academic summary">
        <Metric icon={<CalendarCheck2/>} label="Attendance" value={attendance ? attendanceRate : 'Unavailable'} suffix="%" note={attendance ? `${attendance.classesAttended} attended · ${attendance.totalClasses} total` : 'Attendance data unavailable'} tone="green" link="/my-attendance"/>
        <Metric icon={<TrendingUp/>} label="CGPA" value={performance?.cumulativeGpa ?? 'Unavailable'} fractionDigits={2} note="Cumulative grade point average" tone="blue" link="/my-results"/>
        <Metric icon={<Award/>} label="Credits" value={performance?.creditsRegistered == null ? 'Unavailable' : `${performance.creditsEarned ?? 0} / ${performance.creditsRegistered}`} note="Earned / registered" tone="amber" link="/my-results"/>
        <Metric icon={<BookOpenCheck/>} label="Subjects" value={resultCount} note="Across your academic record" tone="violet" link="/my-results"/>
      </section>
      <div className="student-dashboard-main-grid">
        <section className="student-card student-attendance-overview"><StudentSectionTitle icon={<CalendarCheck2/>} eyebrow="CONSISTENCY" title="Attendance overview" action={<Link to="/my-attendance">Details <ArrowRight size={14}/></Link>}/><div className="student-attendance-content"><div className="student-progress-ring" style={{ '--student-progress': `${attendanceRate}%` } as React.CSSProperties}><div><strong>{attendance ? `${attendanceRate}%` : '—'}</strong><span>attendance</span></div></div><div className="student-attendance-breakdown"><strong>{attendance ? attendanceStatusLabel(attendance.status) : 'Data unavailable'}</strong><p>{attendance ? `${attendance.classesAttended} classes attended out of ${attendance.totalClasses} recorded.` : 'Attendance summary could not be retrieved.'}</p>{attendance && <div><span><i className="student-dot present"/>Present <b>{attendance.classesAttended}</b></span><span><i className="student-dot absent"/>Absent <b>{attendance.classesAbsent}</b></span></div>}</div></div></section>
        <section className="student-card student-standing-card"><StudentSectionTitle icon={<GraduationCap/>} eyebrow="ACADEMIC MILESTONE" title="Your standing"/><div className="student-standing-content"><span className="standing-medallion"><Award size={24}/></span><strong>{standing || 'Not available'}</strong><p>Based on your available academic transcript.</p></div></section>
      </div>
      <DashboardMyCourses courses={myCourses}/>
      <AcademicInsights insights={insights} unavailableSources={data?.academic.unavailableSources ?? []}/>
      <section className="student-card student-performance-card"><StudentSectionTitle icon={<TrendingUp/>} eyebrow="LATEST RESULTS" title="Academic performance" action={<Link to="/my-results">View all results <ArrowRight size={14}/></Link>}/>{latest.length ? <div className="student-performance-list">{latest.map((result) => <article className="student-performance-row" key={result.id}><span className="performance-course-icon"><BookOpenCheck size={17}/></span><div className="performance-course-name"><strong>{result.courseName}</strong><small>{result.courseCode} · Semester {result.semester}</small></div><div className="performance-meter"><span><i style={{ width: `${Math.min(100, Math.max(0, result.totalMarks))}%` }}/></span><small>{result.totalMarks} / 100</small></div><span className="student-grade-badge">{result.grade}</span><strong className="performance-points">{result.gradePoints}<small> pts</small></strong></article>)}</div> : <p className="student-inline-empty">No subject results are available yet.</p>}</section>
      <section className="student-card student-activity-card"><StudentSectionTitle icon={<Activity/>} eyebrow="RECENT UPDATES" title="Recent activity" action={<Link to="/my-attendance">Attendance history <ArrowRight size={14}/></Link>}/>{data?.attendanceRecords.length || latest.length ? <div className="student-activity-list">{data?.attendanceRecords.slice(0, 3).map((record) => <article key={`attendance-${record.id}`}><span className={`activity-marker ${record.status.toLowerCase()}`}><CalendarCheck2 size={14}/></span><div><strong>{record.courseCode} · {record.status.toLowerCase()}</strong><small>Attendance recorded · {new Date(`${record.date}T12:00:00`).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</small></div><span className="activity-tag">CLASS</span></article>)}{latest.slice(0, 2).map((result) => <article key={`result-${result.id}`}><span className="activity-marker result"><Award size={14}/></span><div><strong>{result.courseName} · {result.grade}</strong><small>{result.totalMarks} marks · Semester {result.semester}</small></div><span className="activity-tag">RESULT</span></article>)}</div> : <p className="student-inline-empty">Your recent attendance and results will appear here.</p>}</section>
      <section className="student-quick-actions"><div><span className="student-eyebrow">KEEP MOVING FORWARD</span><h2>Pick up where you left off</h2></div><div className="student-action-grid"><QuickAction to="/my-attendance" icon={<CalendarCheck2/>} title="View attendance" detail="Explore your class history"/><QuickAction to="/my-results" icon={<BookOpenCheck/>} title="View results" detail="Review your performance"/><QuickAction to="/my-profile" icon={<UserRound/>} title="My profile" detail="Your student identity"/></div></section>
    </>}
  </main>
}

function getTimeGreeting() { const hour = new Date().getHours(); return hour < 12 ? 'morning' : hour < 17 ? 'afternoon' : 'evening' }
function attendanceStatusLabel(status: string) {
  return ({ EXCELLENT: 'Excellent consistency', GOOD: 'Attendance is on track', WARNING: 'Attendance needs attention', CRITICAL: 'Attendance is critically low', INSUFFICIENT_DATA: 'Not enough attendance data' } as Record<string, string>)[status] ?? 'Attendance status unavailable'
}

function AcademicInsights({ insights, unavailableSources = [] }: { insights: DisplayInsight[]; unavailableSources?: string[] }) {
  return <section className="student-card glass-card student-dashboard-insights" aria-label="Your academic insights">
    <StudentSectionTitle icon={<Sparkles/>} eyebrow="BASED ON YOUR RECORDS" title="Your academic insights"/>
    {insights.length ? <div className="dashboard-insight-list">{insights.map((insight) => <article className={`dashboard-insight ${insight.tone}`} key={insight.title}><span aria-hidden="true">{insight.tone === 'attention' ? <Info size={17}/> : <Sparkles size={17}/>}</span><div><strong>{insight.title}</strong><p>{insight.detail}</p></div></article>)}</div> : <p className="student-inline-empty">Not enough academic data yet. Insights will appear when attendance, results, or credit records are available.</p>}
    {unavailableSources.length > 0 && <p className="student-inline-empty">Some data sources are currently unavailable: {unavailableSources.join(', ')}.</p>}
  </section>
}

function DashboardMyCourses({ courses }: { courses: StudentCourseEnrollmentView[] }) {
  return <section className="student-card dashboard-my-courses"><StudentSectionTitle icon={<BookOpenCheck/>} eyebrow="YOUR ENROLLMENTS" title="My courses" action={<Link to="/my-courses">View all courses <ArrowRight size={14}/></Link>}/>{courses.length ? <div className="dashboard-course-list">{courses.slice(0, 3).map((course) => <article key={course.id}><span className="performance-course-icon"><BookOpenCheck size={17}/></span><div><strong>{course.courseName}</strong><small>{course.courseCode} · Semester {course.semester} · {course.academicYear}</small><small>Faculty · {course.facultyName}</small></div><span className="student-result-status">{course.status}</span></article>)}</div> : <div className="dashboard-courses-empty"><p>No courses enrolled yet.</p><Link className="student-button" to="/courses">Browse courses <ArrowRight size={15}/></Link></div>}</section>
}

function StudentPageHeading({ eyebrow, title, subtitle }: { eyebrow: string; title: string; subtitle: string }) { return <header className="student-page-heading"><span className="student-eyebrow">{eyebrow}</span><h1>{title}</h1><p>{subtitle}</p></header> }
function StudentSectionTitle({ icon, eyebrow, title, action }: { icon: React.ReactNode; eyebrow: string; title: string; action?: React.ReactNode }) { return <div className="student-card-heading"><div className="student-card-title-icon">{icon}</div><div><small>{eyebrow}</small><h2>{title}</h2></div>{action && <div className="student-heading-action">{action}</div>}</div> }
function Metric({ icon, label, value, note, tone, link, suffix = '', fractionDigits = 0 }: { icon: React.ReactNode; label: string; value: number | string; note: string; tone: string; link: string; suffix?: string; fractionDigits?: number }) { return <Link className="student-metric-card" to={link}><span className={`student-metric-icon ${tone}`}>{icon}</span><span className="student-metric-label">{label}</span><strong>{typeof value === 'number' ? <AnimatedNumber value={value} suffix={suffix} fractionDigits={fractionDigits}/> : value}</strong><small>{note}</small><ArrowRight className="student-metric-arrow" size={15}/></Link> }
function QuickAction({ to, icon, title, detail }: { to: string; icon: React.ReactNode; title: string; detail: string }) { return <Link className="student-quick-action" to={to}><span>{icon}</span><div><strong>{title}</strong><small>{detail}</small></div><ArrowRight size={16}/></Link> }
function StudentDashboardSkeleton() { return <main className="student-portal-page" role="status" aria-label="Loading your dashboard"><div className="student-skeleton-hero"/><div className="student-skeleton-metrics">{[1,2,3,4].map((item) => <i key={item}/>)}</div><div className="student-skeleton-panes"><i/><i/></div><span className="sr-only">Loading your academic overview</span></main> }

function StaffDashboard() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [metrics, setMetrics] = useState(emptyMetrics)
  const [loading, setLoading] = useState(true)
  const [unavailable, setUnavailable] = useState<string[]>([])
  const [attendanceToday, setAttendanceToday] = useState<AttendanceRecord[]>([])
  const [facultyName, setFacultyName] = useState('')
  useEffect(() => {
    let active = true
    const now = new Date(); const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    Promise.allSettled([studentService.getStudents(), attendanceService.getAttendanceByDate(today), resultService.getResults(), isAdmin ? facultyService.list() : facultyService.getMe()]).then((responses) => {
      if (!active) return
      const next: DashboardMetrics = { ...emptyMetrics }; const failures: string[] = []; const [studentResponse, attendanceResponse, resultResponse, facultyResponse] = responses
      if (studentResponse.status === 'fulfilled') {
        const students = studentResponse.value.filter((student: Student) => student.role === 'STUDENT')
        next.students = students.length; next.activeStudents = students.filter((student) => student.status === 'ACTIVE').length
      } else failures.push(`Student list: ${getApiErrorMessage(studentResponse.reason)}`)
      if (attendanceResponse.status === 'fulfilled') { next.attendanceToday = attendanceResponse.value.length; setAttendanceToday(attendanceResponse.value.slice().sort((a, b) => b.id - a.id).slice(0, 5)) }
      else failures.push(`Today's attendance: ${getApiErrorMessage(attendanceResponse.reason)}`)
      if (resultResponse.status === 'fulfilled') next.results = resultResponse.value.length; else failures.push(`Results: ${getApiErrorMessage(resultResponse.reason)}`)
      if (facultyResponse.status === 'fulfilled') {
        if (isAdmin) { const faculty = facultyResponse.value as Faculty[]; next.faculty = faculty.length; next.activeFaculty = faculty.filter((member) => member.status === 'ACTIVE').length }
        else setFacultyName((facultyResponse.value as Faculty).name)
      } else if (isAdmin) failures.push(`Faculty list: ${getApiErrorMessage(facultyResponse.reason)}`)
      setMetrics(next); setUnavailable(failures)
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [isAdmin])
  const cards = isAdmin
    ? [{ label: 'Students', value: metrics.students, note: `${metrics.activeStudents ?? '—'} active`, icon: GraduationCap }, { label: 'Faculty', value: metrics.faculty, note: `${metrics.activeFaculty ?? '—'} active`, icon: UsersRound }, { label: 'Active academic accounts', value: metrics.activeStudents !== null && metrics.activeFaculty !== null ? metrics.activeStudents + metrics.activeFaculty : null, note: 'Students and faculty', icon: UserRound }, { label: "Today's attendance", value: metrics.attendanceToday, note: 'Records from today', icon: CalendarCheck2 }, { label: 'Academic results', value: metrics.results, note: 'Published records', icon: BookOpenCheck }]
    : [{ label: 'Student directory', value: metrics.students, note: `${metrics.activeStudents ?? '—'} active student accounts`, icon: GraduationCap }, { label: "Today's attendance", value: metrics.attendanceToday, note: 'Records from today', icon: CalendarCheck2 }, { label: 'Academic results', value: metrics.results, note: 'Published records', icon: BookOpenCheck }]
  const firstName = user?.email.split('@')[0]?.split(/[._-]/)[0] ?? 'there'
  const greetingName = isAdmin ? `${firstName.charAt(0).toUpperCase()}${firstName.slice(1)}` : facultyName || 'Faculty'
  return <main className="staff-dashboard">
    <header className="staff-dashboard-heading"><div><span className="student-eyebrow">CAMPUSCORE · {isAdmin ? 'ADMINISTRATION' : 'FACULTY WORKSPACE'}</span><h1>{isAdmin ? `Welcome back, ${greetingName}` : `Good ${getTimeGreeting()}, ${greetingName}`}</h1><p>{isAdmin ? 'Your school operations at a glance.' : 'Here’s your teaching overview for today.'}</p></div><span className="staff-greeting-icon">{isAdmin ? <Activity size={24}/> : <GraduationCap size={26}/>}</span></header>
    <div className="staff-metrics-grid">{cards.map(({ label, value, note, icon: Icon }) => <article className="staff-metric-card" key={label}><span><Icon size={18}/></span><small>{label}</small><strong>{loading ? <LoaderCircle className="spin" size={18}/> : value ?? 'Unavailable'}</strong><em>{note}</em></article>)}</div>
    <div className="staff-dashboard-columns"><section className="staff-glass-panel"><div className="staff-panel-heading"><div><small>{isAdmin ? 'SCHOOL OPERATIONS' : 'CLASSROOM SIGNALS'}</small><h2>{isAdmin ? 'Attendance today' : 'Attendance overview'}</h2></div><Link to="/attendance">Open attendance <ArrowRight size={14}/></Link></div>{loading ? <p className="staff-panel-state">Loading attendance activity…</p> : attendanceToday.length ? <div className="staff-activity-list">{attendanceToday.map((record) => <article key={record.id}><span className={`staff-activity-status ${record.status.toLowerCase()}`}><CalendarCheck2 size={15}/></span><div><strong>{record.courseCode} · {record.status.toLowerCase()}</strong><small>Student #{record.studentId} · {record.date}</small></div><span className="staff-activity-label">ATTENDANCE</span></article>)}</div> : <p className="staff-panel-state">No attendance records are available for today.</p>}</section>
      <section className="staff-glass-panel"><div className="staff-panel-heading"><div><small>WORKSPACE SHORTCUTS</small><h2>{isAdmin ? 'Management' : 'Teaching tools'}</h2></div></div><div className="staff-shortcuts">{isAdmin && <Link to="/faculty"><span><UsersRound size={17}/></span><div><strong>Faculty management</strong><small>Manage staff accounts and MFA status</small></div><ArrowRight size={15}/></Link>}<Link to="/students"><span><GraduationCap size={17}/></span><div><strong>Student directory</strong><small>Search student profiles</small></div><ArrowRight size={15}/></Link><Link to="/attendance"><span><CalendarCheck2 size={17}/></span><div><strong>Attendance</strong><small>Review or record class attendance</small></div><ArrowRight size={15}/></Link><Link to="/results"><span><BookOpenCheck size={17}/></span><div><strong>Academic results</strong><small>Manage published results</small></div><ArrowRight size={15}/></Link></div></section></div>
    <div className="staff-data-footnote"><Info size={16}/><p>All figures use live service responses. Course assignments, pending work, notification counts, and last activity timestamps are not exposed by the current APIs.</p></div>
    {unavailable.length > 0 && <div className="staff-data-errors" role="status">Some live metrics are unavailable: {unavailable.join(' · ')}</div>}
  </main>
}
