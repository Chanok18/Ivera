export type Rol = 'ADMINISTRADOR' | 'TRABAJADOR'

export type LoginRequest = {
  email: string
  password: string
}

export type LoginResponse = {
  token: string
  email: string
  rol: Rol
}

export type Session = LoginResponse
