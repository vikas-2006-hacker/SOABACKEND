export type UserRole = 'ADMIN' | 'FACULTY' | 'STUDENT'

export interface AuthResponse {
  token: string
  tokenType: string
  email: string
  role: UserRole
}

export interface MfaChallengeResponse {
  mfaRequired: true
  setupRequired: boolean
  challengeId: string
}

export type LoginResponse = AuthResponse | MfaChallengeResponse

export interface AuthUser {
  email: string
  role: UserRole
}

export interface LoginCredentials {
  email: string
  password: string
}
