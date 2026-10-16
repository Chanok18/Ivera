import { request } from './client'
import type { LoginRequest, LoginResponse } from '../types/auth'

export function login(credentials: LoginRequest) {
  return request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })
}
