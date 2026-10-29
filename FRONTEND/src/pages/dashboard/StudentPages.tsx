import { useEffect, useState, type ReactNode } from 'react'
import { Activity, ArrowUpRight, BadgeCheck, BookOpen, BookOpenCheck, Building2, CalendarCheck2, CalendarDays, ChartNoAxesCombined, CircleUserRound, GraduationCap, IdCard, Info, KeyRound, LockKeyhole, Mail, Phone, ShieldCheck, UserRound, Users } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { resultService } from '../../services/resultService'
import { facultyService } from '../../services/facultyService'
import type { Faculty } from '../../services/facultyService'
import { studentService } from '../../services/studentService'
import type { CurrentStudent, StudentProfile } from '../../types/student'
export { MyAttendancePage } from './MyAttendancePage'
export { MyResultsPage } from './MyResultsPage'

export function MyProfilePage() {
  const { user } = useAuth()
  if (user?.role === 'FACULTY') return <FacultySelfProfile />
  if (user?.role === 'ADMIN') return <AdminSelfProfile />
  return <StudentSelfProfile />
}

function StudentSelfProfile() {
  const { currentStudent: contextStudent } = useAuth()
  const [student, setStudent] = useState<CurrentStudent | null>(contextStudent ?? null)
  const [profile, setProfile] = useState<StudentProfile | null>(null)
  const [cgpa, setCgpa] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadProfile = async () => {
    setLoading(true)
    setError('')
    setProfile(null)
    try {
      const identity = await studentService.getCurrentStudent()
      setStudent(identity)
      const detail = await studentService.getStudentProfile(identity.id)
      setProfile({ ...detail, attendanceRecords: detail.attendanceRecords?.filter((record) => record.studentId === identity.id) ?? [], resultRecords: detail.resultRecords?.filter((record) => record.studentId === identity.id) ?? [] })
      setCgpa(await resultService.getStudentCgpa(identity.id).then((value) => value.cgpa).catch(() => null))
    } catch {
      setError('Unable to load your profile. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    const fetchProfile = async () => {
      setLoading(true)
      setError('')
      try {
        const identity = contextStudent ?? await studentService.getCurrentStudent()
        if (!active) return
        setStudent(identity)
        const detail = await studentService.getStudentProfile(identity.id)
        const gradePointAverage = await resultService.getStudentCgpa(identity.id).then((value) => value.cgpa).catch(() => null)
        if (active) { setProfile({ ...detail, attendanceRecords: detail.attendanceRecords?.filter((record) => record.studentId === identity.id) ?? [], resultRecords: detail.resultRecords?.filter((record) => record.studentId === identity.id) ?? [] }); setCgpa(gradePointAverage) }
      } catch {
        if (active) setError('Unable to load your profile. Please try again.')
      } finally {
        if (active) setLoading(false)
      }
    }
    void fetchProfile()
    return () => { active = false }
  }, [contextStudent])

  if (loading) return <StudentProfileSkeleton />
  if (error || !student || !profile?.studentDetails) return <>
    <div className="page-heading"><div><h1>My Student Profile</h1><p>Manage your academic identity, contact information, and account details.</p></div></div>
    <div className="student-profile-error" role="alert"><CircleUserRound size={22}/><div><strong>Unable to load your profile.</strong><p>Please try again.</p></div><button type="button" className="secondary-action" onClick={() => void loadProfile()}>Try again</button></div>
  </>

  const details = profile.studentDetails
  const attendance = profile.attendanceRecords ?? []
  const results = profile.resultRecords ?? []
  const latestResult = [...results].sort((a, b) => b.semester - a.semester)[0]
  const initials = details.name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
  const active = details.status?.toUpperCase() === 'ACTIVE'
  const attended = attendance.filter((record) => ['PRESENT', 'LATE'].includes(record.status.toUpperCase())).length
  const attendanceRate = attendance.length ? Math.round(attended * 100 / attendance.length) : null
  const registeredCredits = results.reduce((total, result) => total + result.credits, 0)
  const earnedCredits = results.filter((result) => result.resultStatus.toUpperCase() === 'PASS').reduce((total, result) => total + result.credits, 0)

  return <div className="student-profile-page">
    <div className="page-heading"><div><h1>My Student Profile</h1><p>Manage your academic identity, contact information, and account details.</p></div></div>
    <section className="student-identity-hero" aria-labelledby="student-name">
      <div className="student-profile-avatar" aria-label={`${details.name} initials`}>{initials}</div>
      <div className="student-identity-copy"><div className="student-identity-kicker"><GraduationCap size={15}/> CAMPUSCORE · STUDENT IDENTITY</div><h2 id="student-name">{details.name}</h2><p>{details.email}</p><div className="student-identity-tags"><span className="student-role-badge">ID · {details.id}</span><span>{details.department || 'Department not available'}</span><span>{[details.academicYear, details.semester ? `Semester ${details.semester}` : ''].filter(Boolean).join(' · ') || 'Academic term not available'}</span></div></div>
      <span className={`student-status-badge ${active ? 'is-active' : 'is-inactive'}`}><span aria-hidden="true"/>{details.status || 'Status unavailable'}</span>
    </section>

    <div className="student-profile-grid">
      <section className="student-info-card academic-info-card"><ProfileSectionTitle icon={<IdCard size={18}/>} title="Academic Information"/><div className="student-info-grid">
        <StudentInfo icon={<IdCard/>} label="Student ID" value={details.id}/>
        <StudentInfo icon={<GraduationCap/>} label="Department" value={details.department}/>
        <StudentInfo icon={<BookOpen/>} label="Program / course" value={details.courses || 'Not available'}/>
        <StudentInfo icon={<CalendarDays/>} label="Semester" value={details.semester || 'Not available'}/>
        <StudentInfo icon={<CalendarDays/>} label="Academic year" value={details.academicYear || 'Not available'}/>
      </div></section>
      <section className="student-info-card"><ProfileSectionTitle icon={<Activity size={18}/>} title="Academic Snapshot"/><div className="student-snapshot-grid">
        <SnapshotMetric icon={<CalendarDays size={17}/>} label="Attendance" value={attendanceRate === null ? '—' : `${attendanceRate}%`} detail={attendance.length ? `${attended} of ${attendance.length} classes` : 'No records yet'}/>
        <SnapshotMetric icon={<ChartNoAxesCombined size={17}/>} label="CGPA" value={cgpa === null ? '—' : cgpa.toFixed(2)} detail="Cumulative grade point average"/>
        <SnapshotMetric icon={<BookOpenCheck size={17}/>} label="Credits" value={`${earnedCredits} / ${registeredCredits}`} detail="Earned / registered"/>
        <SnapshotMetric icon={<BookOpen size={17}/>} label="Subjects" value={results.length} detail={latestResult ? `Latest: ${latestResult.courseName}` : 'No published results'}/>
      </div></section>
      <section className="student-info-card"><ProfileSectionTitle icon={<Mail size={18}/>} title="Contact Information"/><div className="student-info-grid">
        <StudentInfo icon={<Mail/>} label="Email" value={details.email}/><StudentInfo icon={<Phone/>} label="Phone" value={details.contact || 'Not provided'}/>
      </div></section>
      <section className="student-info-card"><ProfileSectionTitle icon={<ShieldCheck size={18}/>} title="Account Security"/><div className="student-security-list">
        <div><span className="student-security-icon"><BadgeCheck size={17}/></span><span><small>Account status</small><strong>{details.status || 'Status unavailable'}</strong></span></div>
        <div><span className="student-security-icon"><KeyRound size={17}/></span><span><small>Authentication</small><strong>Protected sign-in</strong></span></div>
      </div></section>
    </div>
  </div>
}

function StudentProfileSkeleton() {
  return <div className="student-profile-page" aria-label="Loading your profile" role="status"><div className="page-heading"><div><h1>My Student Profile</h1><p>Loading your academic identity…</p></div></div><div className="student-profile-skeleton-hero"><span/><div><i/><i/><i/></div></div><div className="student-profile-grid"><div className="student-info-card student-skeleton-card"/><div className="student-info-card student-skeleton-card"/><div className="student-info-card student-skeleton-card"/><div className="student-info-card student-skeleton-card"/></div><span className="sr-only">Loading profile information</span></div>
}

function ProfileSectionTitle({ icon, title }: { icon: ReactNode; title: string }) { return <h2 className="student-section-title"><span>{icon}</span>{title}</h2> }
function StudentInfo({ icon, label, value }: { icon: ReactNode; label: string; value: string | number | null | undefined }) { return <div className="student-info-item"><span className="student-info-icon" aria-hidden="true">{icon}</span><div><small>{label}</small><strong>{value === null || value === undefined || value === '' ? 'Not available' : value}</strong></div></div> }
function SnapshotMetric({ icon, label, value, detail }: { icon: ReactNode; label: string; value: string | number; detail: string }) { return <div className="student-snapshot-tile"><span className="student-snapshot-icon">{icon}</span><span className="student-snapshot-label">{label}</span><strong>{value}</strong><small>{detail}</small></div> }

function FacultySelfProfile() {
  const [faculty, setFaculty] = useState<Faculty | null>(null)
  const [error, setError] = useState('')
  const loadFaculty = () => {
    setFaculty(null)
    setError('')
    facultyService.getMe().then(setFaculty).catch(() => setError('Unable to load your faculty profile. Please try again.'))
  }
  useEffect(() => {
    let active = true
    facultyService.getMe().then((data) => { if (active) setFaculty(data) }).catch(() => {
      if (active) setError('Unable to load your faculty profile. Please try again.')
    })
    return () => { active = false }
  }, [])
  if (error) return <><ProfilePageHeading title="My Faculty Profile" subtitle="Your professional identity and account security."/><div className="profile-load-error" role="alert"><Info size={20}/><div><strong>Unable to load your faculty profile.</strong><p>Please try again.</p></div><button className="secondary-action" type="button" onClick={loadFaculty}>Try again</button></div></>
  if (!faculty) return <FacultyProfileSkeleton />

  const initials = faculty.name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
  const active = faculty.status?.toUpperCase() === 'ACTIVE'
  return <div className="faculty-profile-page">
    <ProfilePageHeading title="My Faculty Profile" subtitle="Your professional identity, teaching workspace, and account security."/>
    <section className="faculty-profile-hero" aria-labelledby="faculty-name">
      <div className="faculty-profile-avatar" aria-label={`${faculty.name} initials`}>{initials}</div>
      <div className="faculty-profile-identity"><span className="faculty-eyebrow"><Building2 size={14}/> EDU PULSE · FACULTY</span><h2 id="faculty-name">{faculty.name}</h2><p>{faculty.email}</p><div className="faculty-identity-meta"><span className="faculty-role-chip">Faculty</span><span>{faculty.department || 'Department not provided'}</span><span>Employee ID · {faculty.employeeId}</span></div></div>
      <div className="faculty-hero-badges"><span className={`faculty-state-badge ${active ? 'active' : 'inactive'}`}><i/> {faculty.status || 'Status not provided'}</span><span className={`faculty-mfa-badge ${faculty.mfaEnabled ? 'enabled' : 'pending'}`}><LockKeyhole size={14}/>{faculty.mfaEnabled ? 'MFA enabled' : 'MFA setup pending'}</span></div>
    </section>

    <div className="faculty-profile-grid">
      <section className="student-info-card"><ProfileSectionTitle icon={<UserRound size={18}/>} title="Personal Information"/><div className="student-info-grid">
        <StudentInfo icon={<UserRound/>} label="Full name" value={faculty.name}/><StudentInfo icon={<Mail/>} label="Email" value={faculty.email}/><StudentInfo icon={<Phone/>} label="Phone" value={faculty.phone || 'Not provided'}/><StudentInfo icon={<BadgeCheck/>} label="Account status" value={faculty.status || 'Not provided'}/>
      </div></section>
      <section className="student-info-card"><ProfileSectionTitle icon={<GraduationCap size={18}/>} title="Professional Information"/><div className="student-info-grid">
        <StudentInfo icon={<IdCard/>} label="Employee ID" value={faculty.employeeId}/><StudentInfo icon={<Building2/>} label="Department" value={faculty.department}/><StudentInfo icon={<BookOpen/>} label="Designation" value={faculty.designation || 'Not provided'}/><StudentInfo icon={<Users/>} label="Faculty role" value="Faculty"/>
      </div></section>
      <section className="student-info-card" id="faculty-security"><ProfileSectionTitle icon={<ShieldCheck size={18}/>} title="Account Security"/><div className="student-security-list">
        <div><span className="student-security-icon"><LockKeyhole size={17}/></span><span><small>Multi-factor authentication</small><strong>{faculty.mfaEnabled ? 'Enabled for this account' : 'Setup pending'}</strong></span></div>
        <div><span className="student-security-icon"><ShieldCheck size={17}/></span><span><small>Authentication</small><strong>Protected sign-in</strong></span></div>
        <div><span className="student-security-icon"><BadgeCheck size={17}/></span><span><small>Account status</small><strong>{faculty.status || 'Not provided'}</strong></span></div>
        <p className="faculty-security-note">Last login information is not provided by the Faculty API.</p>
      </div></section>
      <section className="student-info-card"><ProfileSectionTitle icon={<ChartNoAxesCombined size={18}/>} title="Teaching Overview"/><div className="faculty-overview-empty"><span><BookOpenCheck size={19}/></span><p>Faculty-specific course and teaching statistics are not available from the current API.</p></div></section>
    </div>

    <section className="faculty-quick-actions"><div className="faculty-quick-heading"><div><span className="faculty-eyebrow">YOUR WORKSPACE</span><h2>Quick Actions</h2></div><p>Jump back into your teaching tools.</p></div><div className="faculty-action-grid">
      <FacultyQuickAction to="/students" icon={<Users size={18}/>} title="View Students" detail="Browse student records"/>
      <FacultyQuickAction to="/attendance" icon={<CalendarCheck2 size={18}/>} title="Mark Attendance" detail="Open attendance workspace"/>
      <FacultyQuickAction to="/results" icon={<ChartNoAxesCombined size={18}/>} title="View Results" detail="Review academic results"/>
      <a className="faculty-action-link" href="#faculty-security"><span className="faculty-action-icon"><ShieldCheck size={18}/></span><span><strong>Security</strong><small>Review MFA status</small></span><ArrowUpRight size={16}/></a>
    </div></section>
  </div>
}

function AdminSelfProfile() {
  const { user } = useAuth()
  const initials = user?.email.split('@')[0].split(/[._-]/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'AD'
  return <div className="faculty-profile-page admin-profile-page">
    <ProfilePageHeading title="My Administrator Profile" subtitle="Your administrative identity and secure access to CampusCore."/>
    <section className="faculty-profile-hero" aria-labelledby="admin-name">
      <div className="faculty-profile-avatar" aria-label="Administrator initials">{initials}</div>
      <div className="faculty-profile-identity"><span className="faculty-eyebrow"><ShieldCheck size={14}/> EDU PULSE · ADMINISTRATION</span><h2 id="admin-name">{user?.email || 'Administrator'}</h2><p>Administrator account</p><div className="faculty-identity-meta"><span className="faculty-role-chip">ADMIN</span><span>{user?.email || 'Email not available'}</span></div></div>
      <div className="faculty-hero-badges"><span className="faculty-state-badge active"><i/> Authenticated</span><span className="faculty-mfa-badge pending"><LockKeyhole size={14}/>MFA not enabled</span></div>
    </section>
    <div className="faculty-profile-grid admin-profile-grid">
      <section className="student-info-card"><ProfileSectionTitle icon={<UserRound size={18}/>} title="Administrative Identity"/><div className="student-info-grid"><StudentInfo icon={<Mail/>} label="Email" value={user?.email || 'Not provided'}/><StudentInfo icon={<ShieldCheck/>} label="Role" value="Administrator"/></div></section>
      <section className="student-info-card"><ProfileSectionTitle icon={<LockKeyhole size={18}/>} title="Account Security"/><div className="student-security-list"><div><span className="student-security-icon"><ShieldCheck size={17}/></span><span><small>Authentication</small><strong>Protected sign-in</strong></span></div><div><span className="student-security-icon"><LockKeyhole size={17}/></span><span><small>Multi-factor authentication</small><strong>Not enabled for admin accounts</strong></span></div><div><span className="student-security-icon"><BadgeCheck size={17}/></span><span><small>Account status</small><strong>Not provided by an Admin profile API</strong></span></div></div></section>
    </div>
    <section className="faculty-quick-actions"><div className="faculty-quick-heading"><div><span className="faculty-eyebrow">ADMIN WORKSPACE</span><h2>Quick Actions</h2></div><p>Manage the school workspace.</p></div><div className="faculty-action-grid"><FacultyQuickAction to="/students" icon={<Users size={18}/>} title="Students" detail="Manage student records"/><FacultyQuickAction to="/faculty" icon={<Building2 size={18}/>} title="Faculty" detail="Manage faculty accounts"/><FacultyQuickAction to="/attendance" icon={<CalendarCheck2 size={18}/>} title="Attendance" detail="Review attendance"/><FacultyQuickAction to="/results" icon={<ChartNoAxesCombined size={18}/>} title="Results" detail="Manage academic results"/></div></section>
  </div>
}

function ProfilePageHeading({ title, subtitle }: { title: string; subtitle: string }) { return <div className="page-heading"><div><h1>{title}</h1><p>{subtitle}</p></div></div> }
function FacultyQuickAction({ to, icon, title, detail }: { to: string; icon: ReactNode; title: string; detail: string }) { return <Link className="faculty-action-link" to={to}><span className="faculty-action-icon">{icon}</span><span><strong>{title}</strong><small>{detail}</small></span><ArrowUpRight size={16}/></Link> }
function FacultyProfileSkeleton() { return <div className="faculty-profile-page" role="status" aria-label="Loading faculty profile"><ProfilePageHeading title="My Faculty Profile" subtitle="Loading your professional identity…"/><div className="student-profile-skeleton-hero"><span/><div><i/><i/><i/></div></div><div className="faculty-profile-grid"><div className="student-info-card student-skeleton-card"/><div className="student-info-card student-skeleton-card"/><div className="student-info-card student-skeleton-card"/><div className="student-info-card student-skeleton-card"/></div><span className="sr-only">Loading faculty profile information</span></div>}

