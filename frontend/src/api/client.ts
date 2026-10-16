import { clearSession, readSession } from '../auth/session'

const baseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '')

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

async function errorMessage(response: Response): Promise<string> {
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    const body = await response.json() as { error?: string; message?: string }
    return body.error ?? body.message ?? 'La solicitud no pudo completarse.'
  }

  return (await response.text()) || 'La solicitud no pudo completarse.'
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  if (!baseUrl) throw new ApiError(0, 'Falta configurar VITE_API_BASE_URL.')

  const session = readSession()
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (session?.token) headers.set('Authorization', `Bearer ${session.token}`)

  let response: Response
  try {
    response = await fetch(`${baseUrl}${path}`, { ...options, headers })
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. Verifica la URL y tu conexión.')
  }

  if (!response.ok) {
    if (response.status === 401) clearSession()
    throw new ApiError(response.status, await errorMessage(response))
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
