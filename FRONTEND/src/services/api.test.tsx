import { act, cleanup, render, screen } from '@testing-library/react'
import { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, describe, expect, it } from 'vitest'
import { AuthProvider } from '../context/AuthContext'
import { AppRoutes } from '../routes/AppRoutes'
import { getApiErrorMessage, api } from './api'

function createToken(): string {
  const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }))
  return `header.${payload}.signature`
}

afterEach(() => {
  cleanup()
  localStorage.clear()
})

describe('API authentication and errors', () => {
  it('attaches the bearer token and clears the session on a 401 response', async () => {
    const token = createToken()
    localStorage.setItem('edupulse.auth', JSON.stringify({
      token,
      user: { email: 'admin@example.edu', role: 'ADMIN' },
    }))
    render(
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>,
    )
    expect(await screen.findByRole('heading', { name: 'Welcome back, Admin' })).toBeInTheDocument()

    let authorizationHeader = ''
    const adapter = async (config: InternalAxiosRequestConfig) => {
      authorizationHeader = String(config.headers.Authorization ?? '')
      throw new AxiosError(
        'Unauthorized',
        'ERR_BAD_REQUEST',
        config,
        undefined,
        { data: '', status: 401, statusText: 'Unauthorized', headers: {}, config },
      )
    }
    await act(async () => {
      await api.get('/students', { adapter }).catch(() => undefined)
    })

    expect(authorizationHeader).toBe(`Bearer ${token}`)
    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
    expect(localStorage.getItem('edupulse.auth')).toBeNull()
  })

  it('uses a safe user-facing 403 message', () => {
    const error = Object.assign(new Error('hidden backend detail'), {
      isAxiosError: true,
      response: { status: 403 },
    })
    expect(getApiErrorMessage(error)).toBe('You do not have permission to perform this action.')
    expect(getApiErrorMessage(error)).not.toContain('hidden backend detail')
  })

  it('shows backend validation messages without exposing internal details', () => {
    const duplicate = Object.assign(new Error('Request failed'), {
      isAxiosError: true,
      response: { status: 400, data: { status: 400, message: 'Course code already exists.' } },
    })
    const validation = Object.assign(new Error('Request failed'), {
      isAxiosError: true,
      response: { status: 400, data: { validationErrors: { courseCode: 'Course code is required.' } } },
    })
    const internal = Object.assign(new Error('Request failed'), {
      isAxiosError: true,
      response: { status: 400, data: { message: 'SQLException: private database detail' } },
    })
    expect(getApiErrorMessage(duplicate)).toBe('Course code already exists.')
    expect(getApiErrorMessage(validation)).toBe('courseCode: Course code is required.')
    expect(getApiErrorMessage(internal)).toBe('Some of the information provided is invalid. Please review and try again.')
  })

  it('explains a missing admin course route using a course-specific message', () => {
    const error = Object.assign(new Error('Not found'), {
      isAxiosError: true,
      response: { status: 404, data: { path: '/api/admin/courses', status: 404, error: 'Not Found', requestId: 'gateway-id' } },
    })
    expect(getApiErrorMessage(error, 'course')).toContain('academic course route is enabled')
  })

  it('replaces generic not-found text with an actionable course-route message', () => {
    const error = Object.assign(new Error('Not found'), {
      isAxiosError: true,
      response: { status: 404, data: { message: 'The requested information could not be found.' } },
    })
    expect(getApiErrorMessage(error, 'course')).toContain('Course management is unavailable through the API Gateway')
  })
})
