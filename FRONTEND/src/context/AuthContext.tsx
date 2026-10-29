import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AuthContext } from './auth-context'
import { authService } from '../services/authService'
import type { AuthUser, LoginCredentials } from '../types/auth'
import { clearStoredAuth, getStoredAuth, getTokenExpiresAt, storeAuth } from '../utils/storage'
import { studentService } from '../services/studentService'
import { getApiErrorMessage } from '../services/api'
import type { CurrentStudent } from '../types/student'

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(() => getStoredAuth()?.user ?? null)
  const [currentStudent, setCurrentStudent] = useState<CurrentStudent | null>(null)
  const [currentStudentError, setCurrentStudentError] = useState<string | null>(null)

  const refreshCurrentStudent = useCallback(async () => {
    if (!user || user.role === 'FACULTY') return
    setCurrentStudentError(null)
    try { setCurrentStudent(await studentService.getCurrentStudent()) }
    catch (error: unknown) { setCurrentStudentError(getApiErrorMessage(error)) }
  }, [user])

  useEffect(() => {
    if (!user || user.role === 'FACULTY') return
    let active = true
    studentService.getCurrentStudent().then((profile) => {
      if (active) { setCurrentStudent(profile); setCurrentStudentError(null) }
    }).catch((error: unknown) => {
      if (active) setCurrentStudentError(getApiErrorMessage(error))
    })
    return () => { active = false }
  }, [user])

  useEffect(() => {
    const handleUnauthorized = () => {
      clearStoredAuth()
      setUser(null)
    }
    window.addEventListener('auth:unauthorized', handleUnauthorized)
    const token = getStoredAuth()?.token
    const expiresAt = token ? getTokenExpiresAt(token) : null
    const timeout = expiresAt
      ? window.setTimeout(handleUnauthorized, Math.max(0, expiresAt - Date.now()))
      : undefined
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized)
      if (timeout !== undefined) window.clearTimeout(timeout)
    }
  }, [user])

  const login = useCallback(async (credentials: LoginCredentials) => {
    const response = await authService.login(credentials)
    if (!('token' in response)) return response
    setCurrentStudent(null)
    setCurrentStudentError(null)
    setUser(storeAuth(response).user)
    return null
  }, [])

  const verifyMfa = useCallback(async (challengeId: string, code: string) => {
    const response = await authService.verifyMfa(challengeId, code)
    setCurrentStudent(null)
    setCurrentStudentError(null)
    setUser(storeAuth(response).user)
  }, [])

  const logout = useCallback(() => {
    clearStoredAuth()
    setCurrentStudent(null)
    setCurrentStudentError(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, currentStudent, currentStudentError, refreshCurrentStudent, isAuthenticated: user !== null, login, verifyMfa, logout }),
    [user, currentStudent, currentStudentError, refreshCurrentStudent, login, verifyMfa, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
