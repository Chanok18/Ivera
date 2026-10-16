import type { Session } from '../types/auth'

const SESSION_KEY = 'ivera.session'

export function readSession(): Session | null {
  const serialized = sessionStorage.getItem(SESSION_KEY)
  if (!serialized) return null

  try {
    const session = JSON.parse(serialized) as Session
    return session.token && session.email && session.rol ? session : null
  } catch {
    sessionStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function saveSession(session: Session) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY)
}
