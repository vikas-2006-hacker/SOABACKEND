import { createContext } from 'react'
import type { AuthUser, LoginCredentials, MfaChallengeResponse } from '../types/auth'
import type { CurrentStudent } from '../types/student'

export interface AuthContextValue {
  user: AuthUser | null
  currentStudent?: CurrentStudent | null
  currentStudentError?: string | null
  refreshCurrentStudent?: () => Promise<void>
  isAuthenticated: boolean
  login: (credentials: LoginCredentials) => Promise<MfaChallengeResponse | null>
  verifyMfa?: (challengeId: string, code: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
