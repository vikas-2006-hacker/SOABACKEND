import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { RegisterPage } from './RegisterPage'
import { authService } from '../../services/authService'

vi.mock('../../services/authService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../services/authService')>()
  return { ...actual, authService: { ...actual.authService, login: vi.fn(), register: vi.fn() } }
})

const registerMock = vi.mocked(authService.register)
afterEach(() => { cleanup(); vi.clearAllMocks() })

function renderRegister() {
  function LoginNotice() {
    const location = useLocation()
    return <p role="status">{(location.state as { registered?: boolean } | null)?.registered ? 'Account created successfully. Please sign in.' : 'Login'}</p>
  }
  return render(<MemoryRouter initialEntries={['/register']}><Routes>
    <Route path="/register" element={<RegisterPage />} />
    <Route path="/login" element={<LoginNotice />} />
  </Routes></MemoryRouter>)
}

async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>, email = 'new@example.com', password = 'Student@123', confirm = password) {
  await user.type(screen.getByLabelText('Full name'), 'Test Student')
  await user.type(screen.getByLabelText('Email address'), email)
  await user.type(screen.getByLabelText('Password', { selector: 'input' }), password)
  await user.type(screen.getByLabelText('Confirm password', { selector: 'input' }), confirm)
  await user.click(screen.getByRole('button', { name: 'Register' }))
}

describe('registration flow', () => {
  it('posts the backend request shape and redirects with a success message', async () => {
    registerMock.mockResolvedValue()
    const user = userEvent.setup()
    renderRegister()
    await fillAndSubmit(user)
    expect(registerMock).toHaveBeenCalledWith({ name: 'Test Student', email: 'new@example.com', password: 'Student@123' })
    expect(await screen.findByRole('status')).toHaveTextContent('Account created successfully. Please sign in.')
  })

  it('shows a registration-specific duplicate email error', async () => {
    registerMock.mockRejectedValue(Object.assign(new Error('conflict'), { isAxiosError: true, response: { status: 409 } }))
    const user = userEvent.setup(); renderRegister(); await fillAndSubmit(user)
    expect(await screen.findByRole('alert')).toHaveTextContent('An account with this email already exists.')
  })

  it('validates malformed email and mismatched passwords without sending the request', async () => {
    const user = userEvent.setup(); renderRegister()
    await fillAndSubmit(user, 'bad-email', 'Student@123', 'different')
    expect(await screen.findByText('Please enter a valid email address.')).toBeInTheDocument()
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument()
    expect(registerMock).not.toHaveBeenCalled()
  })

  it('shows the backend password validation message when available', async () => {
    registerMock.mockRejectedValue(Object.assign(new Error('invalid'), { isAxiosError: true, response: { status: 400, data: 'Password must include a number' } }))
    const user = userEvent.setup(); renderRegister(); await fillAndSubmit(user)
    expect(await screen.findByRole('alert')).toHaveTextContent('Password must include a number')
  })

  it('shows a generic registration error for server failures', async () => {
    registerMock.mockRejectedValue(Object.assign(new Error('server'), { isAxiosError: true, response: { status: 500 } }))
    const user = userEvent.setup(); renderRegister(); await fillAndSubmit(user)
    expect(await screen.findByRole('alert')).toHaveTextContent('Registration failed. Please try again.')
  })

  it('shows a connection-specific error when the server cannot be reached', async () => {
    registerMock.mockRejectedValue(Object.assign(new Error('offline'), { isAxiosError: true, request: {} }))
    const user = userEvent.setup(); renderRegister(); await fillAndSubmit(user)
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Unable to connect to the server. Please try again.'))
  })
})
