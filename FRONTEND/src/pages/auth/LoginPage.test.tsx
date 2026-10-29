import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../../context/AuthContext'
import { AppRoutes } from '../../routes/AppRoutes'
import { authService } from '../../services/authService'
import type { LoginResponse } from '../../types/auth'

vi.mock('../../services/authService', () => ({
  authService: { login: vi.fn(), setupMfa: vi.fn(), verifyMfa: vi.fn() },
}))

const loginMock = vi.mocked(authService.login)
const setupMock = vi.mocked(authService.setupMfa)
const verifyMock = vi.mocked(authService.verifyMfa)

function createToken(expiresInSeconds = 3600): string {
  const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + expiresInSeconds }))
  return `header.${payload}.signature`
}

function renderApplication() {
  return render(
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>,
  )
}

afterEach(() => {
  cleanup()
  localStorage.clear()
  window.history.replaceState({}, '', '/login')
})

describe('authentication flow', () => {
  it('presents the CampusCore introduction and all three keyboard-operable account types', () => {
    renderApplication()

    expect(screen.getAllByText('SMART ACADEMIC MANAGEMENT PLATFORM')).toHaveLength(2)
    expect(screen.getByRole('heading', { name: 'A smarter way to stay connected with your school.' })).toBeInTheDocument()
    expect(screen.getByText(/CampusCore brings students, faculty, and administrators together in one simple platform/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Built for a connected campus.' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Students' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Faculty', level: 3 })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Administration' })).toBeInTheDocument()
    expect(screen.getByText('CampusCore · Smart Academic Management Platform')).toBeInTheDocument()
    expect(screen.queryByText('ACADEMIC OVERVIEW')).not.toBeInTheDocument()
    expect(screen.queryByText('94%')).not.toBeInTheDocument()
    expect(screen.queryByText('8.9')).not.toBeInTheDocument()
    expect(screen.queryByText('Strong progress')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'Choose account type' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Student' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Faculty' })).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByRole('button', { name: 'Admin' })).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByText('Continue your academic journey.')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Email address' })).toHaveAttribute('autocomplete', 'username')
    expect(screen.getByLabelText('Password', { selector: 'input' })).toHaveAttribute('autocomplete', 'current-password')
    expect(screen.getByRole('button', { name: 'Forgot password?' })).toBeDisabled()
    expect(screen.getByText('Ask your school administrator to reset your password.')).toBeInTheDocument()
  })

  it('keeps role-specific sign-in guidance and password visibility controls available', async () => {
    const user = userEvent.setup()
    renderApplication()

    expect(screen.getByText('Create a student account')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Faculty' }))
    expect(screen.getByText('Manage your classes and empower your students.')).toBeInTheDocument()
    expect(screen.getByText('Faculty accounts are created by a school administrator.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Admin' }))
    expect(screen.getByText('Manage your connected campus from one place.')).toBeInTheDocument()
    expect(screen.getByText('Administrator access is managed by your school.')).toBeInTheDocument()

    const password = screen.getByLabelText('Password', { selector: 'input' })
    expect(password).toHaveAttribute('type', 'password')
    await user.click(screen.getByRole('button', { name: 'Show password' }))
    expect(password).toHaveAttribute('type', 'text')
    await user.click(screen.getByRole('button', { name: 'Hide password' }))
    expect(password).toHaveAttribute('type', 'password')
  })

  it('routes student registration from the secondary login action', async () => {
    const user = userEvent.setup()
    renderApplication()

    await user.click(screen.getByRole('link', { name: 'Create a student account' }))

    expect(await screen.findByRole('heading', { name: 'Create your account' })).toBeInTheDocument()
  })

  it('shows a steady disabled signing-in state while preventing duplicate authentication requests', async () => {
    let releaseLogin: ((response: LoginResponse) => void) | undefined
    loginMock.mockImplementation(() => new Promise((resolve) => { releaseLogin = resolve }))
    const user = userEvent.setup()
    renderApplication()

    await user.type(screen.getByRole('textbox', { name: 'Email address' }), 'student@example.edu')
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'student-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    const submit = await screen.findByRole('button', { name: 'Signing in...' })
    expect(submit).toBeDisabled()
    expect(submit).toHaveAttribute('aria-busy', 'true')
    expect(loginMock).toHaveBeenCalledOnce()

    releaseLogin?.({ token: createToken(), tokenType: 'Bearer', email: 'student@example.edu', role: 'STUDENT' })
    expect(await screen.findByRole('heading', { name: 'Your dashboard' })).toBeInTheDocument()
  })

  it('logs in, persists only auth data, and navigates to the dashboard', async () => {
    loginMock.mockResolvedValue({
      token: createToken(),
      tokenType: 'Bearer',
      email: 'admin@example.edu',
      role: 'ADMIN',
    })
    const user = userEvent.setup()
    const app = renderApplication()

    await user.type(screen.getByRole('textbox', { name: 'Email address' }), 'admin@example.edu')
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'do-not-store-this')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByRole('heading', { name: 'Welcome back, Admin' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^Students$/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^faculty$/i })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /my attendance/i })).not.toBeInTheDocument()
    expect(setupMock).not.toHaveBeenCalled()
    expect(loginMock).toHaveBeenCalledWith({ email: 'admin@example.edu', password: 'do-not-store-this' })
    expect(localStorage.getItem('edupulse.auth')).toContain('admin@example.edu')
    expect(localStorage.getItem('edupulse.auth')).not.toContain('do-not-store-this')

    app.unmount()
    renderApplication()
    expect(await screen.findByRole('heading', { name: 'Welcome back, Admin' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^faculty$/i })).toBeInTheDocument()
  })

  it('shows a login error without navigating when authentication fails', async () => {
    loginMock.mockRejectedValue(Object.assign(new Error('Unauthorized'), {
      isAxiosError: true,
      response: { status: 401 },
    }))
    const user = userEvent.setup()
    renderApplication()

    await user.type(screen.getByRole('textbox', { name: 'Email address' }), 'wrong@example.edu')
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'wrong-password')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Your session has expired or the email and password are incorrect.',
    )
    expect(screen.getByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
  })

  it('logs a student in directly without an MFA challenge', async () => {
    loginMock.mockResolvedValue({ token: createToken(), tokenType: 'Bearer', email: 'student@example.edu', role: 'STUDENT' })
    const user = userEvent.setup()
    renderApplication()
    await user.type(screen.getByRole('textbox', { name: 'Email address' }), 'student@example.edu')
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'student-password')
    await user.click(screen.getByRole('button', { name: /sign in/i }))
    expect(await screen.findByRole('heading', { name: 'Your dashboard' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: /faculty verification/i })).not.toBeInTheDocument()
    expect(setupMock).not.toHaveBeenCalled()
    expect(verifyMock).not.toHaveBeenCalled()
  })

  it('sends faculty to authenticator enrollment and stores no JWT before verification', async () => {
    loginMock.mockResolvedValue({ mfaRequired: true, setupRequired: true, challengeId: 'faculty-challenge' })
    setupMock.mockResolvedValue({ qrCodeDataUrl: 'data:image/png;base64,qr', expiresInSeconds: 300 })
    verifyMock.mockResolvedValue({ token: createToken(), tokenType: 'Bearer', email: 'faculty@example.edu', role: 'FACULTY' })
    const user = userEvent.setup()
    renderApplication()
    await user.click(screen.getByRole('button', { name: 'Faculty' }))
    await user.type(screen.getByRole('textbox', { name: 'Email address' }), 'faculty@example.edu')
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'faculty-password')
    await user.click(screen.getByRole('button', { name: /sign in/i }))
    expect(await screen.findByRole('heading', { name: 'Set up faculty verification' })).toBeInTheDocument()
    expect(await screen.findByRole('img', { name: 'Authenticator setup QR code' })).toBeInTheDocument()
    expect(localStorage.getItem('edupulse.auth')).toBeNull()
    await user.type(screen.getByRole('textbox', { name: '6-digit authenticator code' }), '123456')
    await user.click(screen.getByRole('button', { name: 'Verify and continue' }))
    expect(await screen.findByRole('heading', { name: /Good (morning|afternoon|evening), Faculty/ })).toBeInTheDocument()
    expect(verifyMock).toHaveBeenCalledWith('faculty-challenge', '123456')
    expect(JSON.parse(localStorage.getItem('edupulse.auth') ?? '{}').token).toBeTruthy()
  })

  it('does not authenticate faculty after an invalid MFA code', async () => {
    loginMock.mockResolvedValue({ mfaRequired: true, setupRequired: false, challengeId: 'faculty-challenge' })
    verifyMock.mockRejectedValue(Object.assign(new Error('Unauthorized'), { isAxiosError: true, response: { status: 401 } }))
    const user = userEvent.setup()
    renderApplication()
    await user.type(screen.getByRole('textbox', { name: 'Email address' }), 'faculty@example.edu')
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'faculty-password')
    await user.click(screen.getByRole('button', { name: /sign in/i }))
    expect(await screen.findByRole('heading', { name: 'Faculty verification' })).toBeInTheDocument()
    await user.type(screen.getByRole('textbox', { name: '6-digit authenticator code' }), '999999')
    await user.click(screen.getByRole('button', { name: 'Verify and continue' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Session expired. Please sign in again.')
    expect(localStorage.getItem('edupulse.auth')).toBeNull()
  })

  it('does not authenticate faculty after an expired MFA challenge', async () => {
    loginMock.mockResolvedValue({ mfaRequired: true, setupRequired: false, challengeId: 'expired-challenge' })
    verifyMock.mockRejectedValue(Object.assign(new Error('Expired'), { isAxiosError: true, response: { status: 401 } }))
    const user = userEvent.setup()
    renderApplication()
    await user.type(screen.getByRole('textbox', { name: 'Email address' }), 'faculty@example.edu')
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'faculty-password')
    await user.click(screen.getByRole('button', { name: /sign in/i }))
    expect(await screen.findByRole('heading', { name: 'Faculty verification' })).toBeInTheDocument()
    await user.type(screen.getByRole('textbox', { name: '6-digit authenticator code' }), '123456')
    await user.click(screen.getByRole('button', { name: 'Verify and continue' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Session expired. Please sign in again.')
    expect(localStorage.getItem('edupulse.auth')).toBeNull()
  })

  it('restores a valid session on refresh and logs out on user request', async () => {
    localStorage.setItem('edupulse.auth', JSON.stringify({
      token: createToken(),
      user: { email: 'faculty@example.edu', role: 'FACULTY' },
    }))
    const user = userEvent.setup()
    const view = renderApplication()

    expect(await screen.findByRole('heading', { name: /Good (morning|afternoon|evening), Faculty/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'My Students' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /settings/i })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /sign out/i }))
    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
    expect(localStorage.getItem('edupulse.auth')).toBeNull()
    view.unmount()
  })

  it('rejects an expired JWT on refresh and redirects protected pages to login', async () => {
    localStorage.setItem('edupulse.auth', JSON.stringify({
      token: createToken(-120),
      user: { email: 'student@example.edu', role: 'STUDENT' },
    }))
    renderApplication()

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Welcome back' })).toBeInTheDocument())
    expect(screen.queryByRole('heading', { name: 'Welcome back, Student' })).not.toBeInTheDocument()
    expect(localStorage.getItem('edupulse.auth')).toBeNull()
  })

  it('shows only the dashboard for a student role', async () => {
    localStorage.setItem('edupulse.auth', JSON.stringify({
      token: createToken(),
      user: { email: 'student@example.edu', role: 'STUDENT' },
    }))
    renderApplication()

    expect(await screen.findByRole('heading', { name: 'Your dashboard' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /students/i })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'My Attendance' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'My Results' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'My Profile' })).toBeInTheDocument()
  })
})
