import { useEffect, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { AlertCircle, BookOpenCheck, Eye, EyeOff, GraduationCap, LoaderCircle, LockKeyhole, Mail, MoveRight, ShieldCheck, UsersRound } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '../../hooks/useAuth'
import { getApiErrorMessage } from '../../services/api'
import { authService } from '../../services/authService'
import type { MfaChallengeResponse, UserRole } from '../../types/auth'

const loginSchema = z.object({
  email: z.email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export function LoginPage() {
  const { login, verifyMfa } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const registrationSucceeded = (location.state as { registered?: boolean } | null)?.registered
  const [showPassword, setShowPassword] = useState(false)
  const [authError, setAuthError] = useState('')
  const [selectedRole, setSelectedRole] = useState<UserRole>('STUDENT')
  const [mfaChallenge, setMfaChallenge] = useState<MfaChallengeResponse | null>(null)
  const [mfaCode, setMfaCode] = useState('')
  const [qrCode, setQrCode] = useState<string | null>(null)
  const [setupError, setSetupError] = useState('')
  const roleMessage = {
    STUDENT: 'Continue your academic journey.',
    FACULTY: 'Manage your classes and empower your students.',
    ADMIN: 'Manage your connected campus from one place.',
  }[selectedRole]
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  useEffect(() => {
    if (!mfaChallenge?.setupRequired) return
    let active = true
    authService.setupMfa(mfaChallenge.challengeId).then((setup) => {
      if (active) setQrCode(setup.qrCodeDataUrl)
    }).catch((error: unknown) => {
      if (active) setSetupError(getApiErrorMessage(error))
    })
    return () => { active = false }
  }, [mfaChallenge])

  const submit = handleSubmit(async (credentials) => {
    setAuthError('')
    try {
      const challenge = await login(credentials)
      if (challenge) {
        setMfaChallenge(challenge)
        setMfaCode('')
        setQrCode(null)
        setSetupError('')
        return
      }
      const destination = (location.state as { from?: string } | null)?.from ?? '/dashboard'
      navigate(destination, { replace: true })
    } catch (error: unknown) {
      setAuthError(getApiErrorMessage(error, 'login'))
    }
  })

  const submitMfa = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!mfaChallenge || !verifyMfa) return
    setAuthError('')
    try {
      await verifyMfa(mfaChallenge.challengeId, mfaCode)
      const destination = (location.state as { from?: string } | null)?.from ?? '/dashboard'
      navigate(destination, { replace: true })
    } catch (error: unknown) {
      setAuthError(getApiErrorMessage(error))
    }
  }

  const backToLogin = () => {
    setMfaChallenge(null)
    setMfaCode('')
    setQrCode(null)
    setAuthError('')
    setSetupError('')
  }

  return (
    <main className="auth-page login-auth-page">
      <section className="auth-brand-panel">
        <div className="brand-lockup">
          <div className="brand-mark"><BookOpenCheck size={22} strokeWidth={2.2} /></div>
          <div className="brand-name">
            <strong>CAMPUSCORE</strong>
            <span>SMART ACADEMIC MANAGEMENT PLATFORM</span>
          </div>
        </div>
        <div className="brand-copy">
          <p className="brand-eyebrow"><span aria-hidden="true" />SMART ACADEMIC MANAGEMENT PLATFORM</p>
          <h1>A smarter way to stay connected with your school.</h1>
          <p>CampusCore brings students, faculty, and administrators together in one simple platform. Manage courses, connect with faculty, track attendance, view results, and stay informed about your academic journey — all from one place.</p>
          <section className="campus-introduction" aria-labelledby="campus-introduction-title">
            <h2 id="campus-introduction-title">Built for a connected campus.</h2>
            <div className="campus-audiences">
              <article>
                <GraduationCap size={17} aria-hidden="true" />
                <div><h3>Students</h3><p>Discover courses, enroll with your faculty, and follow your academic progress.</p></div>
              </article>
              <article>
                <UsersRound size={17} aria-hidden="true" />
                <div><h3>Faculty</h3><p>Manage courses, students, attendance, and academic records.</p></div>
              </article>
              <article>
                <ShieldCheck size={17} aria-hidden="true" />
                <div><h3>Administration</h3><p>Manage courses, faculty, students, and the academic environment.</p></div>
              </article>
            </div>
          </section>
        </div>
        <p className="brand-footer">CampusCore · Smart Academic Management Platform</p>
      </section>

      <section className="auth-form-panel">
        <div className="login-card">
          <div className="login-heading">
            <p className="login-kicker">WELCOME TO CAMPUSCORE</p>
            <h2>{mfaChallenge ? (mfaChallenge.setupRequired ? 'Set up faculty verification' : 'Faculty verification') : 'Welcome back'}</h2>
            <p>{mfaChallenge ? (mfaChallenge.setupRequired ? 'Scan the QR code with an authenticator app, then enter its six-digit code.' : 'Enter the six-digit code from your authenticator app.') : 'Sign in to your CampusCore account.'}</p>
          </div>
          {registrationSucceeded && <div className="auth-success" role="status">Account created successfully. Please sign in.</div>}
          {mfaChallenge ? <>
            {mfaChallenge.setupRequired && <div className="mfa-setup-panel">
              {qrCode ? <img className="mfa-qr" src={qrCode} alt="Authenticator setup QR code" /> : !setupError && <p>Preparing your secure authenticator QR code…</p>}
              {setupError && <div className="auth-error" role="alert"><AlertCircle size={17}/><span>{setupError}</span></div>}
              <p>Setup expires in five minutes. The QR code contains your private authenticator setup key. Keep it private.</p>
            </div>}
            <form onSubmit={submitMfa} noValidate>
              <div className="field">
                <label htmlFor="mfa-code">6-digit authenticator code</label>
                <div className="input-wrap"><ShieldCheck className="input-icon" size={17}/><input className="glass-input" id="mfa-code" type="text" inputMode="numeric" autoComplete="one-time-code" maxLength={6} pattern="[0-9]{6}" value={mfaCode} onChange={(event) => setMfaCode(event.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="000000" aria-label="6-digit authenticator code" required /></div>
              </div>
              {authError && <div className="auth-error" role="alert"><AlertCircle size={17}/><span>{authError}</span></div>}
              <button className="login-submit" type="submit" disabled={mfaCode.length !== 6}><span>Verify and continue</span><MoveRight size={17}/></button>
            </form>
            <button className="mfa-back-button" type="button" onClick={backToLogin}>Back to login</button>
          </> : <>
          <div className="role-selector" role="group" aria-label="Choose account type" data-active-role={selectedRole}>
            {(['STUDENT', 'FACULTY', 'ADMIN'] as UserRole[]).map((role) => <button key={role} type="button" aria-pressed={selectedRole === role} className={selectedRole === role ? 'selected' : ''} onClick={() => setSelectedRole(role)}>{role.charAt(0) + role.slice(1).toLowerCase()}</button>)}
          </div>
          <p key={selectedRole} className="role-guidance" aria-live="polite">{roleMessage}</p>
          <form onSubmit={submit} noValidate>
            <div className="field">
              <label htmlFor="email">Email address</label>
              <div className="input-wrap">
                <Mail className="input-icon" size={17} />
                <input
                  className="glass-input"
                  id="email"
                  type="email"
                  autoComplete="username"
                  placeholder="you@school.edu"
                  aria-invalid={Boolean(errors.email)}
                  aria-describedby={errors.email ? 'email-error' : undefined}
                  {...register('email')}
                />
              </div>
              {errors.email && <p className="field-error" id="email-error">{errors.email.message}</p>}
            </div>
            <div className="field">
              <label htmlFor="password">Password</label>
              <div className="input-wrap">
                <LockKeyhole className="input-icon" size={17} />
                <input
                  className="glass-input"
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  aria-invalid={Boolean(errors.password)}
                  aria-describedby={errors.password ? 'password-error' : undefined}
                  {...register('password')}
                />
                <button
                  className="password-toggle"
                  type="button"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  onClick={() => setShowPassword((visible) => !visible)}
                >
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
              {errors.password && <p className="field-error" id="password-error">{errors.password.message}</p>}
            </div>
            <div className="login-form-options">
              <button className="forgot-password" type="button" disabled aria-describedby="forgot-password-help">Forgot password?</button>
              <span id="forgot-password-help">Ask your school administrator to reset your password.</span>
            </div>
            {authError && (
              <div className="auth-error" role="alert">
                <AlertCircle size={17} aria-hidden="true" />
                <span>{authError}</span>
              </div>
            )}
            <button className="login-submit" type="submit" disabled={isSubmitting} aria-busy={isSubmitting}>
              <span>{isSubmitting ? 'Signing in...' : 'Sign in'}</span>
              {isSubmitting ? <LoaderCircle className="login-spinner" size={17} aria-hidden="true" /> : <MoveRight size={17} aria-hidden="true" />}
            </button>
          </form>
          </>}
          {!mfaChallenge && selectedRole === 'STUDENT' && <p className="login-note"><Link to="/register">Create a student account</Link></p>}
          {!mfaChallenge && selectedRole === 'FACULTY' && <p className="login-note">Faculty accounts are created by a school administrator.</p>}
          {!mfaChallenge && selectedRole === 'ADMIN' && <p className="login-note">Administrator access is managed by your school.</p>}
        </div>
      </section>
    </main>
  )
}
