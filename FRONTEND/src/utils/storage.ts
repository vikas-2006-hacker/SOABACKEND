import type { AuthResponse, AuthUser } from '../types/auth'

const AUTH_STORAGE_KEY = 'edupulse.auth'
const EXPIRY_SKEW_SECONDS = 30

export interface StoredAuth {
  token: string
  user: AuthUser
}

export function getStoredAuth(): StoredAuth | null {
  const value = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!value) return null

  try {
    const stored = JSON.parse(value) as StoredAuth
    if (
      !stored.token ||
      !stored.user?.email ||
      !['ADMIN', 'FACULTY', 'STUDENT'].includes(stored.user?.role)
    ) {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      return null
    }
    if (getTokenExpiresAt(stored.token) === null) {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      return null
    }
    return stored
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export function storeAuth(response: AuthResponse): StoredAuth {
  if (
    !response.token ||
    response.tokenType?.toLowerCase() !== 'bearer' ||
    !response.email ||
    !['ADMIN', 'FACULTY', 'STUDENT'].includes(response.role)
  ) {
    throw new Error('The authentication service returned an invalid login response.')
  }
  if (getTokenExpiresAt(response.token) === null) {
    throw new Error('The authentication service returned an expired token.')
  }
  const auth = {
    token: response.token,
    user: { email: response.email, role: response.role },
  }
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth))
  return auth
}

export function clearStoredAuth(): void {
  localStorage.removeItem(AUTH_STORAGE_KEY)
}

export function getTokenExpiresAt(token: string): number | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/')
    const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '=')
    const decoded = JSON.parse(atob(paddedPayload)) as { exp?: unknown }
    if (typeof decoded.exp !== 'number') return null
    const expiresAt = decoded.exp * 1000
    return expiresAt <= Date.now() + EXPIRY_SKEW_SECONDS * 1000 ? null : expiresAt
  } catch {
    return null
  }
}
