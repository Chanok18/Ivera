import { createContext } from 'react'
import type { LoginRequest, Session } from '../types/auth'

export type AuthContextValue = {
  session: Session | null
  signIn: (credentials: LoginRequest) => Promise<void>
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
