import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Eye,
  LoaderCircle,
  Pencil,
  Plus,
  Search,
  Trash2,
  UsersRound,
  X,
} from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { getApiErrorMessage } from '../../services/api'
import { studentService } from '../../services/studentService'
import type { Student } from '../../types/student'
import { Toast } from '../../components/common/Toast'

const PAGE_SIZE = 10

interface NoticeState {
  notice?: string
}

export function StudentsPage() {
  const { user } = useAuth()
  const location = useLocation()
  const [students, setStudents] = useState<Student[]>([])
  const [search, setSearch] = useState('')
  const [departmentFilter, setDepartmentFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [sortOrder, setSortOrder] = useState('name')
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [deleteTarget, setDeleteTarget] = useState<Student | null>(null)
  const [deleteError, setDeleteError] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [notice, setNotice] = useState((location.state as NoticeState | null)?.notice ?? '')
  const canDelete = user?.role === 'ADMIN'

  useEffect(() => {
    let active = true
    studentService.getStudents()
      .then((records) => {
        if (active) setStudents(records.filter((student) => student.role === 'STUDENT'))
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
  }, [reloadKey])

  const filteredStudents = useMemo(() => {
    const query = search.trim().toLocaleLowerCase()
    return students.filter((student) =>
      (!query ||
      [
        student.id,
        student.name,
        student.email,
        student.department,
        student.courses,
        student.contact,
        student.status,
      ].some((value) => String(value ?? '').toLocaleLowerCase().includes(query))) &&
      (!departmentFilter || student.department === departmentFilter) &&
      (!statusFilter || student.status === statusFilter),
    ).sort((a, b) => sortOrder === 'name' ? a.name.localeCompare(b.name) : sortOrder === 'name-desc' ? b.name.localeCompare(a.name) : sortOrder === 'newest' ? b.id - a.id : a.id - b.id)
  }, [students, search, departmentFilter, statusFilter, sortOrder])
  const departments = useMemo(() => [...new Set(students.map((student) => student.department).filter(Boolean))].sort(), [students])
  const pageCount = Math.max(1, Math.ceil(filteredStudents.length / PAGE_SIZE))
  const currentPage = Math.min(page, pageCount)
  const visibleStudents = filteredStudents.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

  const confirmDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    setDeleteError('')
    try {
      await studentService.deleteStudent(deleteTarget.id)
      setStudents((current) => current.filter((student) => student.id !== deleteTarget.id))
      setDeleteTarget(null)
      setNotice('Student deleted successfully.')
    } catch (cause: unknown) {
      setDeleteError(getApiErrorMessage(cause))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <>
      <div className="page-heading students-heading">
        <div>
          <h1>Students</h1>
          <p>Manage student records and academic profiles.</p>
        </div>
        <Link className="primary-action" to="/students/add">
          <Plus size={17} />
          <span>Add student</span>
        </Link>
      </div>

      <section className="students-panel" aria-label="Student directory">
        <div className="students-toolbar">
          <div>
            <div className="student-count">
              <span className="student-count-icon"><UsersRound size={17} /></span>
              <strong>{loading ? '…' : students.length}</strong>
              <span>{students.length === 1 ? 'student' : 'students'}</span>
            </div>
            <p className="toolbar-subtitle">All registered student records</p>
          </div>
          <div className="students-directory-tools">
            <label className="search-box">
              <Search size={17} aria-hidden="true" />
              <input type="search" value={search} onChange={(event) => { setSearch(event.target.value); setPage(1) }} placeholder="Search students" aria-label="Search students" />
              {search && <button type="button" aria-label="Clear search" onClick={() => setSearch('')}><X size={15} /></button>}
            </label>
            <div className="students-filter-controls">
              <label><span className="sr-only">Filter by department</span><select aria-label="Filter by department" value={departmentFilter} onChange={(event) => { setDepartmentFilter(event.target.value); setPage(1) }}><option value="">All departments</option>{departments.map((department) => <option key={department} value={department}>{department}</option>)}</select></label>
              <label><span className="sr-only">Filter by status</span><select aria-label="Filter by status" value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(1) }}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option><option value="GRADUATED">Graduated</option></select></label>
              <label><span className="sr-only">Sort students</span><select aria-label="Sort students" value={sortOrder} onChange={(event) => { setSortOrder(event.target.value); setPage(1) }}><option value="name">Name A–Z</option><option value="name-desc">Name Z–A</option><option value="newest">Newest ID</option><option value="oldest">Oldest ID</option></select></label>
            </div>
          </div>
        </div>

        {loading ? (
          <div className="student-loading" role="status" aria-label="Loading students">
            {Array.from({ length: 5 }, (_, index) => (
              <div className="student-skeleton" key={index}>
                <span /><span /><span /><span />
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="student-feedback error-feedback" role="alert">
            <AlertCircle size={21} />
            <strong>Unable to load students</strong>
            <p>{error}</p>
            <button
              className="secondary-action"
              onClick={() => {
                setError('')
                setLoading(true)
                setReloadKey((current) => current + 1)
              }}
            >
              Try again
            </button>
          </div>
        ) : filteredStudents.length === 0 ? (
          <div className="student-feedback">
            <div className="empty-icon"><UsersRound size={23} /></div>
            <strong>{search ? 'No matching students' : 'No students yet'}</strong>
            <p>
              {search
                ? 'Try a different name, email, department, or status.'
                : 'Add a student to begin building your directory.'}
            </p>
            {!search && (
              <Link className="secondary-action" to="/students/add">
                <Plus size={16} /> Add student
              </Link>
            )}
          </div>
        ) : (
          <>
            <div className="table-scroll">
              <table className="students-table">
                <thead>
                  <tr>
                    <th scope="col">Student</th>
                    <th scope="col">Department</th>
                    <th scope="col">Semester</th>
                    <th scope="col">Status</th>
                    <th scope="col" className="actions-heading">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleStudents.map((student) => (
                    <tr key={student.id}>
                      <td>
                        <div className="student-identity">
                          <div className="student-avatar">{student.name.slice(0, 1)}</div>
                          <div>
                            <Link className="student-name" to={`/students/${student.id}`}>{student.name}</Link>
                            <span>{student.email}</span>
                          </div>
                        </div>
                      </td>
                      <td>
                        <span className="department-value">{student.department}</span>
                        {student.courses && <span className="student-secondary">{student.courses}</span>}
                      </td>
                      <td>
                        <span className="semester-value">Semester {student.semester}</span>
                        <span className="student-secondary">{student.academicYear}</span>
                      </td>
                      <td>
                        <span className={`status-pill status-${student.status.toLowerCase()}`}>
                          <span />{student.status.toLowerCase()}
                        </span>
                      </td>
                      <td>
                        <div className="row-actions">
                          <Link
                            className="icon-action"
                            to={`/students/${student.id}`}
                            aria-label={`View ${student.name}`}
                            title="View student"
                          ><Eye size={16} /></Link>
                          <Link
                            className="icon-action"
                            to={`/students/${student.id}/edit`}
                            aria-label={`Edit ${student.name}`}
                            title="Edit student"
                          ><Pencil size={15} /></Link>
                          {canDelete && (
                            <button
                              className="icon-action danger-action"
                              aria-label={`Delete ${student.name}`}
                              title="Delete student"
                              onClick={() => {
                                setDeleteTarget(student)
                                setDeleteError('')
                              }}
                            ><Trash2 size={15} /></button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <footer className="table-footer">
              <span>
                Showing {Math.min((currentPage - 1) * PAGE_SIZE + 1, filteredStudents.length)}–
                {Math.min(currentPage * PAGE_SIZE, filteredStudents.length)} of {filteredStudents.length}
              </span>
              <div className="pagination">
                <button
                  type="button"
                  aria-label="Previous page"
                  disabled={currentPage === 1}
                  onClick={() => setPage(currentPage - 1)}
                ><ChevronLeft size={17} /></button>
                <span>{currentPage} / {pageCount}</span>
                <button
                  type="button"
                  aria-label="Next page"
                  disabled={currentPage === pageCount}
                  onClick={() => setPage(currentPage + 1)}
                ><ChevronRight size={17} /></button>
              </div>
            </footer>
          </>
        )}
      </section>

      {notice && <Toast message={notice} onDismiss={() => setNotice('')} />}
      {deleteTarget && (
        <div className="dialog-backdrop">
          <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-title">
            <div className="confirm-icon"><Trash2 size={20} /></div>
            <h2 id="delete-title">Delete student?</h2>
            <p>
              This will permanently delete <strong>{deleteTarget.name}</strong> and their student record.
              This action cannot be undone.
            </p>
            {deleteError && <p className="dialog-error" role="alert">{deleteError}</p>}
            <div className="dialog-actions">
              <button className="secondary-action" disabled={deleting} onClick={() => setDeleteTarget(null)}>
                Cancel
              </button>
              <button className="delete-action" disabled={deleting} onClick={confirmDelete}>
                {deleting ? <LoaderCircle className="spin" size={16} /> : <Trash2 size={16} />}
                {deleting ? 'Deleting…' : 'Delete student'}
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  )
}
