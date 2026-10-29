import axios from 'axios'
import { clearStoredAuth, getStoredAuth } from '../utils/storage'

const configuredApiBase = import.meta.env.VITE_API_BASE_URL || '/api'
const normalizedApiBase = configuredApiBase.replace(/\/$/, '')
const apiBase = normalizedApiBase.startsWith('/') || normalizedApiBase.endsWith('/api')
  ? normalizedApiBase
  : `${normalizedApiBase}/api`

export const api = axios.create({
  baseURL: apiBase,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const auth = getStoredAuth()
  if (auth?.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      !error.config?.url?.includes('/auth/login')
    ) {
      clearStoredAuth()
      window.dispatchEvent(new Event('auth:unauthorized'))
    }
    return Promise.reject(error)
  },
)

function getBackendErrorMessage(data: unknown): string | null {
  if (!data || typeof data !== 'object') return null
  const body = data as Record<string, unknown>
  if (typeof body.message === 'string') {
    const message = body.message.trim()
    if (message && message.length <= 240
      && !/\b(sql[a-z]*|jdbc|exception|stack trace|org\.springframework|database detail)\b/i.test(message)
      && !/the requested (information|endpoint|resource) could not be found/i.test(message)) {
      return message
    }
  }
  if (body.validationErrors && typeof body.validationErrors === 'object') {
    const messages = Object.entries(body.validationErrors as Record<string, unknown>)
      .filter((entry): entry is [string, string] => typeof entry[1] === 'string')
      .map(([field, message]) => `${field}: ${message}`)
    if (messages.length) return messages.join('; ')
  }
  return null
}

export function getApiErrorMessage(error: unknown, context: 'login' | 'request' | 'course' = 'request'): string {
  if (!axios.isAxiosError(error)) {
    return 'Something went wrong. Please try again.'
  }

  if (!error.response) {
    return 'Unable to reach the school services. Check that the API Gateway is running.'
  }

  if (error.response.status !== 401 && error.response.status !== 403 && error.response.status < 500) {
    const backendMessage = getBackendErrorMessage(error.response.data)
    if (backendMessage) return backendMessage
  }

  switch (error.response.status) {
    case 400:
      return 'Some of the information provided is invalid. Please review and try again.'
    case 401:
      return context === 'login'
        ? 'Your session has expired or the email and password are incorrect.'
        : 'Session expired. Please sign in again.'
    case 403:
      return 'You do not have permission to perform this action.'
    case 404:
      return context === 'course'
        ? 'Course management is unavailable through the API Gateway. Verify that its academic course route is enabled and restart it after configuration changes.'
        : 'The requested endpoint or resource could not be found.'
    case 409:
      return 'This information conflicts with an existing record.'
    default:
      return error.response.status >= 500
        ? 'The school service is temporarily unavailable. Please try again later.'
        : 'The request could not be completed. Please try again.'
  }
}
