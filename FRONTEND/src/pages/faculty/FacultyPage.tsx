import { useEffect, useState } from 'react'
import { AlertCircle, LoaderCircle, Pencil, Plus, Search, UserRoundCog } from 'lucide-react'
import { Toast } from '../../components/common/Toast'
import { getApiErrorMessage } from '../../services/api'
import { facultyService } from '../../services/facultyService'
import type { Faculty, FacultyWrite } from '../../services/facultyService'

const emptyForm = { name: '', email: '', employeeId: '', department: '', password: '', confirmPassword: '' }

export function FacultyPage() {
  const [faculty, setFaculty] = useState<Faculty[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState<Faculty | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState('')

  const loadFaculty = () => {
    facultyService.list().then(setFaculty).catch((cause: unknown) => setError(getApiErrorMessage(cause))).finally(() => setLoading(false))
  }
  const reload = () => { setLoading(true); setError(''); loadFaculty() }
  useEffect(() => { loadFaculty() }, [])

  const startCreate = () => { setEditing(null); setForm(emptyForm); setError(''); setFormOpen(true) }
  const startEdit = (member: Faculty) => {
    setEditing(member)
    setFormOpen(true)
    setForm({ name: member.name, email: member.email, employeeId: member.employeeId, department: member.department, password: '', confirmPassword: '' })
    setError('')
  }
  const closeForm = () => { setEditing(null); setForm(emptyForm); setFormOpen(false) }
  const save = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    if (!editing && form.password.length < 8) { setError('Temporary password must contain at least 8 characters.'); return }
    if (!editing && form.password !== form.confirmPassword) { setError('Passwords do not match.'); return }
    const data: FacultyWrite = { name: form.name.trim(), email: form.email.trim(), employeeId: form.employeeId.trim(), department: form.department.trim() }
    setSaving(true)
    try {
      if (editing) await facultyService.update(editing.id, data)
      else await facultyService.create({ ...data, password: form.password })
      closeForm()
      setNotice(editing ? 'Faculty details updated.' : 'Faculty account created. MFA setup will happen at first sign-in.')
      reload()
    } catch (cause: unknown) { setError(getApiErrorMessage(cause)) } finally { setSaving(false) }
  }
  const toggle = async (member: Faculty) => {
    try {
      const updated = await facultyService.setStatus(member.id, member.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
      setFaculty((items) => items.map((item) => item.id === updated.id ? updated : item))
      setNotice(`Faculty account ${updated.status.toLowerCase()}.`)
    } catch (cause: unknown) { setError(getApiErrorMessage(cause)) }
  }
  const visible = faculty.filter((member) => [member.name, member.email, member.employeeId, member.department, member.status].some((value) => value.toLowerCase().includes(search.trim().toLowerCase())))

  return <>
    <div className="page-heading students-heading"><div><h1>Faculty Management</h1><p>Create and manage faculty accounts. MFA enrollment is required at first sign-in.</p></div><button className="primary-action" onClick={startCreate}><Plus size={17}/> Add faculty</button></div>
    {error && !editing && <div className="student-feedback error-feedback" role="alert"><AlertCircle size={18}/><p>{error}</p></div>}
    <section className="students-panel" aria-label="Faculty directory">
      <div className="students-toolbar"><div><div className="student-count"><span className="student-count-icon"><UserRoundCog size={17}/></span><strong>{loading ? '…' : faculty.length}</strong><span>{faculty.length === 1 ? 'faculty member' : 'faculty members'}</span></div><p className="toolbar-subtitle">School staff accounts</p></div><label className="search-box"><Search size={17}/><input type="search" aria-label="Search faculty" placeholder="Search faculty" value={search} onChange={(event) => setSearch(event.target.value)}/></label></div>
      {loading ? <p className="student-loading" role="status">Loading faculty…</p> : error && faculty.length === 0 ? <div className="student-feedback"><p>Unable to load faculty.</p><button className="secondary-action" onClick={reload}>Try again</button></div> : visible.length === 0 ? <div className="student-feedback"><strong>{search ? 'No matching faculty' : 'No faculty accounts yet'}</strong><p>{search ? 'Try a different search.' : 'Add a faculty member to get started.'}</p></div> : <div className="table-scroll"><table className="students-table"><thead><tr><th>Name</th><th>Email</th><th>Employee ID</th><th>Department</th><th>Status</th><th>MFA</th><th>Actions</th></tr></thead><tbody>{visible.map((member) => <tr key={member.id}><td><strong>{member.name}</strong></td><td>{member.email}</td><td>{member.employeeId}</td><td>{member.department}</td><td><span className={`status-pill status-${member.status.toLowerCase()}`}><span/>{member.status.toLowerCase()}</span></td><td>{member.mfaEnabled ? 'Enabled' : 'Setup pending'}</td><td><div className="row-actions"><button className="icon-action" aria-label={`Edit ${member.name}`} onClick={() => startEdit(member)}><Pencil size={15}/></button><button className="secondary-action" onClick={() => toggle(member)}>{member.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button></div></td></tr>)}</tbody></table></div>}
    </section>
    {formOpen ? <div className="dialog-backdrop"><section className="confirm-dialog faculty-dialog" role="dialog" aria-modal="true" aria-labelledby="faculty-form-title"><h2 id="faculty-form-title">{editing ? 'Edit faculty' : 'Add faculty'}</h2><p>Faculty accounts are provisioned by an administrator. A temporary password is required for new accounts.</p><form onSubmit={save}><div className="faculty-form-grid">{([['name','Full name','text'],['email','Email','email'],['employeeId','Employee ID','text'],['department','Department','text']] as const).map(([key,label,type]) => <label className="field" key={key}>{label}<input type={type} required value={form[key]} onChange={(event) => setForm({ ...form, [key]: event.target.value })}/></label>)}{!editing && <><label className="field">Temporary password<input type="password" minLength={8} required autoComplete="new-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })}/></label><label className="field">Confirm password<input type="password" minLength={8} required autoComplete="new-password" value={form.confirmPassword} onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}/></label></>}</div>{error && <p className="dialog-error" role="alert">{error}</p>}<div className="dialog-actions"><button type="button" className="secondary-action" disabled={saving} onClick={closeForm}>Cancel</button><button type="submit" className="primary-action" disabled={saving}>{saving ? <LoaderCircle className="spin" size={16}/> : <Plus size={16}/>} {saving ? 'Saving…' : editing ? 'Save changes' : 'Create faculty'}</button></div></form></section></div> : null}
    {notice && <Toast message={notice} onDismiss={() => setNotice('')}/>}
  </>
}
