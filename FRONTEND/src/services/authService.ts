import { api } from './api'
import axios from 'axios'
import type { AuthResponse, LoginCredentials, LoginResponse } from '../types/auth'

export const authService = {
  async login(credentials: LoginCredentials): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/auth/login', credentials)
    return response.data
  },
  async setupMfa(challengeId: string): Promise<{ qrCodeDataUrl: string; expiresInSeconds: number }> {
    const response = await api.post<{ qrCodeDataUrl: string; expiresInSeconds: number }>('/auth/mfa/setup', { challengeId })
    return response.data
  },
  async verifyMfa(challengeId: string, code: string): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/mfa/verify', { challengeId, code })
    return response.data
  },
  async register(payload: { name: string; email: string; password: string }): Promise<void> {
    await api.post('/auth/register', payload)
  },
}

export function getRegistrationErrorMessage(error: unknown): string {
  if (!axios.isAxiosError(error)) return 'Registration failed. Please try again.'
  if (!error.response) return 'Unable to connect to the server. Please try again.'
  if (error.response.status === 409) return 'An account with this email already exists.'
  if (error.response.status === 400) {
    const data: unknown = error.response.data
    if (typeof data === 'string' && data.length < 180 && !/<(html|!doctype)/i.test(data)) return data
    if (data && typeof data === 'object' && 'detail' in data && typeof data.detail === 'string') return data.detail
    return 'Please check your name, email address, and password and try again.'
  }
  return 'Registration failed. Please try again.'
}
