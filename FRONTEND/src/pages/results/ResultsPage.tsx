import { useCallback, useEffect, useState, cloneElement, type ReactElement } from 'react'
import { AlertCircle, BookOpenCheck, LoaderCircle, Pencil, Save, Trash2, X } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuth } from '../../hooks/useAuth'
import { getApiErrorMessage } from '../../services/api'
import { resultService } from '../../services/resultService'
import { studentService } from '../../services/studentService'
import type { ResultRecord } from '../../types/academic'
import type { Student } from '../../types/student'

const schema = z.object({
  studentId: z.number().int().positive('Select a student'),
  semester: z.number().int().min(1, 'Semester must be at least 1'),
  academicYear: z.string().trim().min(1, 'Academic year is required'),
  courseCode: z.string().trim().min(1, 'Course code is required'),
  courseName: z.string().trim().min(1, 'Course name is required'),
  credits: z.number().int().min(1, 'Credits must be at least 1'),
  internalMarks: z.number().min(0).max(40, 'Internal marks cannot exceed 40'),
  externalMarks: z.number().min(0).max(60, 'External marks cannot exceed 60'),
})
type FormValues = z.infer<typeof schema>

export function ResultsPage() {
  const { user } = useAuth()
  const canRecord = user?.role === 'FACULTY'
  const [records, setRecords] = useState<ResultRecord[]>([])
  const [students, setStudents] = useState<Student[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { studentId: 0, semester: 1, academicYear: '2025-2026', courseCode: '', courseName: '', credits: 3, internalMarks: 0, externalMarks: 0 },
  })

  const load = useCallback(async () => {
    try {
      const [resultRows, studentRows] = await Promise.all([resultService.getResults(), studentService.getStudents()])
      setRecords(resultRows.slice().sort((a, b) => b.semester - a.semester || a.studentId - b.studentId))
      setStudents(studentRows.filter((student) => student.role === 'STUDENT'))
    } catch (cause) { setError(getApiErrorMessage(cause)) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => {
    const request = window.setTimeout(() => { void load() }, 0)
    return () => window.clearTimeout(request)
  }, [load])

  const submit = handleSubmit(async (values) => {
    setError(''); setNotice('')
    try {
      if (editingId === null) await resultService.recordResult(values)
      else await resultService.updateResult(editingId, values)
      setEditingId(null)
      reset({ studentId: 0, semester: 1, academicYear: '2025-2026', courseCode: '', courseName: '', credits: 3, internalMarks: 0, externalMarks: 0 })
      setNotice(`Result ${editingId === null ? 'saved' : 'updated'}. Grade and total were calculated by the Result Service.`)
      await load()
    } catch (cause) { setError(getApiErrorMessage(cause)) }
  })

  const editRecord = (record: ResultRecord) => {
    setEditingId(record.id); setError(''); setNotice('')
    reset({ studentId: record.studentId, semester: record.semester, academicYear: record.academicYear, courseCode: record.courseCode, courseName: record.courseName, credits: record.credits, internalMarks: record.internalMarks, externalMarks: record.externalMarks })
    document.getElementById('result-student')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  const deleteRecord = async (record: ResultRecord) => {
    if (!window.confirm(`Delete ${record.courseName} result for student ${record.studentId}?`)) return
    try {
      setError(''); await resultService.deleteResult(record.id)
      setRecords((current) => current.filter((row) => row.id !== record.id)); setNotice('Result deleted successfully.')
    } catch (cause) { setError(getApiErrorMessage(cause)) }
  }

  const cancelEdit = () => {
    setEditingId(null); reset({ studentId: 0, semester: 1, academicYear: '2025-2026', courseCode: '', courseName: '', credits: 3, internalMarks: 0, externalMarks: 0 })
  }

  return <>
    <div className="page-heading"><div><h1>Results</h1><p>Review academic results and record course marks.</p></div></div>
    {canRecord && <form className="student-form" onSubmit={submit} noValidate>
      <section className="form-section"><div className="form-section-heading"><h2>{editingId === null ? 'Record result' : 'Edit result'}</h2><p>The Result Service calculates total marks, grade, and result status.</p></div>
        <div className="form-grid">
          <FormField label="Student" error={errors.studentId?.message}><select {...register('studentId', { valueAsNumber: true })}><option value={0}>Select a student</option>{students.map((s) => <option key={s.id} value={s.id}>{s.name} · #{s.id}</option>)}</select></FormField>
          <FormField label="Semester" error={errors.semester?.message}><input type="number" min="1" {...register('semester', { valueAsNumber: true })}/></FormField>
          <FormField label="Academic year" error={errors.academicYear?.message}><input {...register('academicYear')}/></FormField>
          <FormField label="Course code" error={errors.courseCode?.message}><input {...register('courseCode')} placeholder="e.g. CS301"/></FormField>
          <FormField label="Course name" error={errors.courseName?.message}><input {...register('courseName')} placeholder="e.g. Data Structures"/></FormField>
          <FormField label="Credits" error={errors.credits?.message}><input type="number" min="1" {...register('credits', { valueAsNumber: true })}/></FormField>
          <FormField label="Internal marks (0–40)" error={errors.internalMarks?.message}><input type="number" min="0" max="40" step="0.5" {...register('internalMarks', { valueAsNumber: true })}/></FormField>
          <FormField label="External marks (0–60)" error={errors.externalMarks?.message}><input type="number" min="0" max="60" step="0.5" {...register('externalMarks', { valueAsNumber: true })}/></FormField>
        </div>
        {error && <div className="form-error" role="alert"><AlertCircle size={18}/>{error}</div>}
        {notice && <p className="auth-success" role="status">{notice}</p>}
        <div className="form-actions">{editingId !== null && <button className="secondary-action" type="button" onClick={cancelEdit}><X size={16}/> Cancel edit</button>}<button className="primary-action" type="submit" disabled={isSubmitting || students.length === 0}>{isSubmitting ? <LoaderCircle className="spin" size={17}/> : <Save size={17}/>}<span>{isSubmitting ? 'Saving…' : editingId === null ? 'Save result' : 'Update result'}</span></button></div>
      </section>
    </form>}
    {!canRecord && <p className="foundation-note">Your role can view results. Recording and editing are restricted to faculty.</p>}
    <section className="record-panel"><div className="record-heading"><div className="metric-icon"><BookOpenCheck size={18}/></div><div><h2>Academic results</h2><p>Live results returned by Result Service</p></div></div>
      {notice && !canRecord && <p className="auth-success" role="status">{notice}</p>}{error && records.length > 0 && <div className="form-error" role="alert"><AlertCircle size={18}/>{error}</div>}
      {loading ? <div className="page-loading" role="status"><LoaderCircle className="spin" size={20}/> Loading results…</div> : error && records.length === 0 ? <div className="page-error" role="alert"><AlertCircle size={19}/>{error}<button className="secondary-action" onClick={() => { setError(''); setLoading(true); void load() }}>Try again</button></div> : records.length === 0 ? <p className="record-empty">No academic results are available.</p> : <div className="table-scroll"><table className="students-table"><thead><tr><th>Student ID</th><th>Course</th><th>Semester</th><th>Marks</th><th>Grade</th><th>Status</th>{canRecord && <th>Actions</th>}</tr></thead><tbody>{records.map((r) => <tr key={r.id}><td>#{r.studentId}</td><td><strong>{r.courseName}</strong><span className="student-secondary">{r.courseCode}</span></td><td>{r.semester} · {r.academicYear}</td><td>{r.totalMarks} <span className="student-secondary">({r.internalMarks}+{r.externalMarks})</span></td><td>{r.grade}</td><td>{r.resultStatus}</td>{canRecord && <td><div className="row-actions"><button className="icon-action" aria-label={`Edit result ${r.id}`} onClick={() => editRecord(r)}><Pencil size={15}/></button><button className="icon-action danger-action" aria-label={`Delete result ${r.id}`} onClick={() => void deleteRecord(r)}><Trash2 size={15}/></button></div></td>}</tr>)}</tbody></table></div>}
    </section>
  </>
}

function FormField({ label, error, children }: { label: string; error?: string; children: ReactElement<{ id?: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string }> }) {
  const id = `result-${label.toLowerCase().replaceAll(/[^a-z0-9]+/g, '-')}`
  return <div className="form-field"><label htmlFor={id}>{label}</label><div className="form-input">{cloneElement(children, { id, 'aria-invalid': Boolean(error), 'aria-describedby': error ? `${id}-error` : undefined })}</div>{error && <p className="field-error" id={`${id}-error`}>{error}</p>}</div>
}
