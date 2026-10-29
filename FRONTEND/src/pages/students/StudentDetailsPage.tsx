import { useEffect, useState } from 'react'
import { AlertCircle, ArrowLeft, BookOpenCheck, CalendarCheck2, LoaderCircle, Pencil } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../../services/api'
import { studentService } from '../../services/studentService'
import type { StudentProfile } from '../../types/student'

export function StudentDetailsPage() {
  const { id } = useParams()
  const studentId = Number(id)
  const validStudentId = Number.isSafeInteger(studentId) && studentId > 0
  const [profile, setProfile] = useState<StudentProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!validStudentId) return
    let active = true
    studentService.getStudentProfile(studentId)
      .then((response) => {
        if (active) setProfile(response)
      })
      .catch((cause: unknown) => {
        if (active) setError(getApiErrorMessage(cause))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [studentId, validStudentId])

  if (!validStudentId) {
    return (
      <>
        <div className="page-heading"><div><h1>Student profile</h1></div></div>
        <div className="page-error" role="alert"><AlertCircle size={19} />The student ID is invalid.</div>
        <Link className="secondary-action" to="/students"><ArrowLeft size={16} /> Back to students</Link>
      </>
    )
  }

  if (loading) {
    return <div className="page-loading" role="status"><LoaderCircle className="spin" size={20} /> Loading student profile…</div>
  }
  if (error || !profile?.studentDetails) {
    return (
      <>
        <div className="page-heading"><div><h1>Student profile</h1></div></div>
        <div className="page-error" role="alert"><AlertCircle size={19} />{error || 'Student profile was not returned by the service.'}</div>
        <Link className="secondary-action" to="/students"><ArrowLeft size={16} /> Back to students</Link>
      </>
    )
  }

  const { studentDetails: student, attendanceRecords = [], resultRecords = [] } = profile
  const safeAttendanceRecords = attendanceRecords ?? []
  const safeResultRecords = resultRecords ?? []

  return (
    <>
      <div className="page-heading">
        <div>
          <Link className="back-link" to="/students"><ArrowLeft size={16} /> Students</Link>
          <h1>Student profile</h1>
          <p>View student information and available academic records.</p>
        </div>
        <Link className="primary-action" to={`/students/${student.id}/edit`}>
          <Pencil size={16} /><span>Edit student</span>
        </Link>
      </div>
      <section className="profile-card">
        <div className="profile-hero">
          <div className="profile-avatar">{student.name.slice(0, 1)}</div>
          <div className="profile-title">
            <h2>{student.name}</h2>
            <p>{student.email}</p>
          </div>
          <span className={`status-pill status-${student.status.toLowerCase()}`}><span />{student.status.toLowerCase()}</span>
        </div>
        <div className="profile-fields">
          <ProfileField label="Student ID" value={student.id} />
          <ProfileField label="Department" value={student.department} />
          <ProfileField label="Age" value={student.age} />
          <ProfileField label="Contact" value={student.contact || 'Not provided'} />
          <ProfileField label="Courses" value={student.courses || 'Not provided'} />
          <ProfileField label="Semester" value={`Semester ${student.semester}`} />
          <ProfileField label="Academic year" value={student.academicYear} />
          <ProfileField label="Role" value={student.role} />
        </div>
      </section>

      <div className="profile-records">
        <section className="record-panel" id="attendance">
          <div className="record-heading">
            <div className="metric-icon"><CalendarCheck2 size={18} /></div>
            <div><h2>Attendance</h2><p>{safeAttendanceRecords.length} records returned by the profile API</p></div>
          </div>
          {safeAttendanceRecords.length === 0 ? (
            <p className="record-empty">No attendance records are available for this student.</p>
          ) : (
            <div className="record-list">
              {safeAttendanceRecords.map((record) => (
                <div className="record-row" key={record.id}>
                  <span>{record.date}</span>
                  <span>{record.courseCode}</span>
                  <strong>{record.status}</strong>
                </div>
              ))}
            </div>
          )}
        </section>
        <section className="record-panel" id="results">
          <div className="record-heading">
            <div className="metric-icon"><BookOpenCheck size={18} /></div>
            <div><h2>Academic results</h2><p>{safeResultRecords.length} records returned by the profile API</p></div>
          </div>
          {safeResultRecords.length === 0 ? (
            <p className="record-empty">No academic results are available for this student.</p>
          ) : (
            <div className="record-list">
              {safeResultRecords.map((record) => (
                <div className="record-row result-record-row" key={record.id}>
                  <span>{record.courseName} ({record.courseCode})</span>
                  <span>Semester {record.semester}</span>
                  <strong>{record.grade} · {record.totalMarks}</strong>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </>
  )
}

function ProfileField({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="profile-field">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}
