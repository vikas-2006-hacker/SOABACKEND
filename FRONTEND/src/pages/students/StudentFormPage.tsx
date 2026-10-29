import { useEffect, useMemo, useState } from 'react'
import { cloneElement, type ReactElement } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { AlertCircle, ArrowLeft, LoaderCircle, Save } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '../../hooks/useAuth'
import { getApiErrorMessage } from '../../services/api'
import { studentService } from '../../services/studentService'
import type { Student, StudentStatus, StudentWrite } from '../../types/student'

const studentSchema = z.object({
  name: z.string().trim().min(1, 'Name is required'),
  email: z.email('Enter a valid email address'),
  age: z.number().int().min(15, 'Students must be at least 15 years old'),
  department: z.string().trim().min(1, 'Department is required'),
  courses: z.string(),
  contact: z.string(),
  password: z.string(),
  semester: z.number().int().min(1, 'Semester must be at least 1'),
  academicYear: z.string(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'GRADUATED']),
  assignedFacultyEmail: z.union([z.email('Enter a valid faculty email'), z.literal('')]),
})

type StudentFormValues = z.infer<typeof studentSchema>

function studentToFormValues(student?: Student): StudentFormValues {
  return {
    name: student?.name ?? '',
    email: student?.email ?? '',
    age: student?.age ?? 15,
    department: student?.department ?? '',
    courses: student?.courses ?? '',
    contact: student?.contact ?? '',
    password: '',
    semester: student?.semester ?? 1,
    academicYear: student?.academicYear ?? '2025-2026',
    status: student?.status ?? 'ACTIVE',
    assignedFacultyEmail: student?.assignedFacultyEmail ?? '',
  }
}

function toStudentWrite(values: StudentFormValues): StudentWrite {
  return {
    name: values.name.trim(),
    email: values.email.trim(),
    age: values.age,
    department: values.department.trim(),
    courses: values.courses.trim(),
    contact: values.contact.trim(),
    semester: values.semester,
    status: values.status as StudentStatus,
    assignedFacultyEmail: values.assignedFacultyEmail.trim(),
    academicYear: values.academicYear.trim(),
    ...(values.password ? { password: values.password } : {}),
  }
}

export function StudentFormPage() {
  const { user } = useAuth()
  const canAssignFaculty = user?.role === 'ADMIN'
  const { id } = useParams()
  const navigate = useNavigate()
  const studentId = useMemo(() => Number(id), [id])
  const isEditing = id !== undefined
  const [loading, setLoading] = useState(isEditing)
  const [loadError, setLoadError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const validStudentId = !isEditing || (Number.isSafeInteger(studentId) && studentId > 0)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<StudentFormValues>({
    resolver: zodResolver(studentSchema),
    defaultValues: studentToFormValues(),
  })

  useEffect(() => {
    if (!isEditing) return
    if (!validStudentId) return
    let active = true
    studentService.getStudent(studentId)
      .then((student) => {
        if (active) reset(studentToFormValues(student))
      })
      .catch((cause: unknown) => {
        if (active) setLoadError(getApiErrorMessage(cause))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [isEditing, studentId, reset, validStudentId])

  const submit = handleSubmit(async (values) => {
    setSubmitError('')
    try {
      const data = toStudentWrite(values)
      if (isEditing) {
        await studentService.updateStudent(studentId, data)
      } else {
        await studentService.createStudent(data)
      }
      navigate('/students', {
        replace: true,
        state: { notice: isEditing ? 'Student updated successfully.' : 'Student created successfully.' },
      })
    } catch (cause: unknown) {
      setSubmitError(getApiErrorMessage(cause))
    }
  })

  if (!validStudentId) {
    return (
      <>
        <div className="page-heading"><div><h1>Edit student</h1></div></div>
        <div className="page-error" role="alert"><AlertCircle size={19} />The student ID is invalid.</div>
        <Link className="secondary-action" to="/students"><ArrowLeft size={16} /> Back to students</Link>
      </>
    )
  }

  if (loading) {
    return <div className="page-loading" role="status"><LoaderCircle className="spin" size={20} /> Loading student…</div>
  }

  if (loadError) {
    return (
      <>
        <div className="page-heading"><div><h1>Edit student</h1></div></div>
        <div className="page-error" role="alert"><AlertCircle size={19} />{loadError}</div>
        <Link className="secondary-action" to="/students"><ArrowLeft size={16} /> Back to students</Link>
      </>
    )
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <Link className="back-link" to="/students"><ArrowLeft size={16} /> Students</Link>
          <h1>{isEditing ? 'Edit student' : 'Add student'}</h1>
          <p>{isEditing ? 'Update the student’s profile information.' : 'Create a student profile in the school directory.'}</p>
        </div>
      </div>
      <form className="student-form" onSubmit={submit} noValidate>
        <section className="form-section">
          <div className="form-section-heading">
            <h2>Personal information</h2>
            <p>Required student identity and contact details.</p>
          </div>
          <div className="form-grid">
            <FormField label="Full name" error={errors.name?.message}>
              <input {...register('name')} autoComplete="name" placeholder="Student name" />
            </FormField>
            <FormField label="Email address" error={errors.email?.message}>
              <input {...register('email')} type="email" autoComplete="email" placeholder="student@school.edu" />
            </FormField>
            <FormField label="Age" error={errors.age?.message}>
              <input {...register('age', { valueAsNumber: true })} type="number" min="15" step="1" />
            </FormField>
            <FormField label="Department" error={errors.department?.message}>
              <input {...register('department')} placeholder="e.g. Computer Science" />
            </FormField>
            {canAssignFaculty && (
              <FormField label="Assigned faculty email" hint="Optional. Only this faculty account can view and manage the student." error={errors.assignedFacultyEmail?.message}>
                <input {...register('assignedFacultyEmail')} type="email" placeholder="faculty@school.edu" />
              </FormField>
            )}
            <FormField label="Contact number" error={errors.contact?.message}>
              <input {...register('contact')} type="tel" autoComplete="tel" placeholder="Optional" />
            </FormField>
            <FormField label="Courses" hint="Optional; enter course codes or names." error={errors.courses?.message}>
              <input {...register('courses')} placeholder="e.g. CS301, MTH201" />
            </FormField>
          </div>
        </section>

        <section className="form-section">
          <div className="form-section-heading">
            <h2>Academic details</h2>
            <p>Enrollment and current academic standing.</p>
          </div>
          <div className="form-grid">
            <FormField label="Semester" error={errors.semester?.message}>
              <input {...register('semester', { valueAsNumber: true })} type="number" min="1" step="1" />
            </FormField>
            <FormField label="Academic year" error={errors.academicYear?.message}>
              <input {...register('academicYear')} placeholder="2025-2026" />
            </FormField>
            <FormField label="Status" error={errors.status?.message}>
              <select {...register('status')}>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
                <option value="GRADUATED">Graduated</option>
              </select>
            </FormField>
            {!isEditing && (
              <FormField
                label="Initial password"
                hint="Optional. Set one if the student should be able to sign in."
                error={errors.password?.message}
              >
                <input {...register('password')} type="password" autoComplete="new-password" placeholder="Optional" />
              </FormField>
            )}
          </div>
        </section>

        {submitError && (
          <div className="form-error" role="alert"><AlertCircle size={18} />{submitError}</div>
        )}
        <div className="form-actions">
          <Link className="secondary-action" to="/students">Cancel</Link>
          <button className="primary-action" type="submit" disabled={isSubmitting}>
            {isSubmitting ? <LoaderCircle className="spin" size={17} /> : <Save size={17} />}
            <span>{isSubmitting ? 'Saving…' : isEditing ? 'Save changes' : 'Create student'}</span>
          </button>
        </div>
      </form>
    </>
  )
}

interface FormFieldProps {
  label: string
  hint?: string
  error?: string
  children: ReactElement<{ id?: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string }>
}

function FormField({ label, hint, error, children }: FormFieldProps) {
  const inputId = `field-${label.toLowerCase().replaceAll(' ', '-')}`
  return (
    <div className="form-field">
      <label htmlFor={inputId}>{label}</label>
      <div className="form-input">{cloneElement(children, {
        id: inputId,
        'aria-invalid': Boolean(error),
        'aria-describedby': error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined,
      })}</div>
      {hint && !error && <p className="field-hint" id={`${inputId}-hint`}>{hint}</p>}
      {error && <p className="field-error" id={`${inputId}-error`}>{error}</p>}
    </div>
  )
}
