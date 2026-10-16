import { useState, type ReactNode } from 'react'
import { login } from '../api/auth'
import { clearSession, readSession, saveSession } from './session'
import type { LoginRequest, Session } from '../types/auth'
import { AuthContext } from './context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => readSession())

  async function signIn(credentials: LoginRequest) {
    const nextSession = await login(credentials)
    saveSession(nextSession)
    setSession(nextSession)
  }

  function signOut() {
    clearSession()
    setSession(null)
  }

  return <AuthContext.Provider value={{ session, signIn, signOut }}>{children}</AuthContext.Provider>
}
