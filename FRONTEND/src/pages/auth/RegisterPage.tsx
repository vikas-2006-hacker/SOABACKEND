import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { BookOpenCheck, Eye, EyeOff, Mail, UserRound } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { authService, getRegistrationErrorMessage } from '../../services/authService'

const schema = z.object({
  name: z.string().trim().min(1, 'Enter your full name'),
  email: z.email('Please enter a valid email address.'),
  password: z.string().min(8, 'Use at least 8 characters'),
  confirmPassword: z.string().min(1, 'Confirm your password'),
}).refine((v) => v.password === v.confirmPassword, { path: ['confirmPassword'], message: 'Passwords do not match' })
type Values = z.infer<typeof schema>

export function RegisterPage() {
  const navigate = useNavigate()
  const [visible, setVisible] = useState(false)
  const [error, setError] = useState('')
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema) })
  const submit = handleSubmit(async ({ name, email, password }) => {
    setError('')
    try {
      await authService.register({ name, email, password })
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (e) { setError(getRegistrationErrorMessage(e)) }
  })
  return <main className="auth-page">
    <section className="auth-brand-panel"><div className="brand-lockup"><div className="brand-mark"><BookOpenCheck size={22} /></div><div className="brand-name"><strong>CampusCore</strong><span>Smart Academic Management Platform</span></div></div><div className="brand-copy"><p className="brand-eyebrow">Join your school</p><h1>Your learning journey starts here.</h1><p>Create your student account to access your academic information.</p></div><div className="brand-decoration" aria-hidden="true"/><p className="brand-footer">A connected campus. A brighter future.</p></section>
    <section className="auth-form-panel"><div className="login-card"><div className="login-heading"><h2>Create your account</h2><p>Register as a student to get started.</p></div><form onSubmit={submit} noValidate>
      <div className="field"><label htmlFor="name">Full name</label><div className="input-wrap"><UserRound className="input-icon" size={17}/><input id="name" autoComplete="name" placeholder="Your full name" {...register('name')}/></div>{errors.name && <p className="field-error">{errors.name.message}</p>}</div>
      <div className="field"><label htmlFor="email">Email address</label><div className="input-wrap"><Mail className="input-icon" size={17}/><input id="email" type="email" autoComplete="email" placeholder="you@school.edu" {...register('email')}/></div>{errors.email && <p className="field-error">{errors.email.message}</p>}</div>
      {(['password','confirmPassword'] as const).map((field, index) => <div className="field" key={field}><label htmlFor={field}>{index ? 'Confirm password' : 'Password'}</label><div className="input-wrap"><input id={field} type={visible ? 'text' : 'password'} autoComplete={index ? 'new-password' : 'new-password'} placeholder={index ? 'Enter password again' : 'At least 8 characters'} {...register(field)}/><button className="password-toggle" type="button" aria-label={visible ? 'Hide password' : 'Show password'} onClick={() => setVisible(!visible)}>{visible ? <EyeOff size={17}/> : <Eye size={17}/>}</button></div>{errors[field] && <p className="field-error">{errors[field]?.message}</p>}</div>)}
      {error && <div className="auth-error" role="alert">{error}</div>}<button className="login-submit" type="submit" disabled={isSubmitting}><span>{isSubmitting ? 'Creating account…' : 'Register'}</span></button>
    </form><p className="login-note">Already have an account? <Link to="/login">Sign in</Link></p></div></section>
  </main>
}
