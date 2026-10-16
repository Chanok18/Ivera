import { request } from './client'

export type Unidad = {
  id: number
  nombre: string
}

export type Presentacion = {
  id: number
  unidad: Unidad
  factorConversion: number
  precioPresentacion: number | null
  equivalencia: string
}

export type Producto = {
  id: number
  nombre: string
  descripcion: string | null
  categoria: string | null
  codigoBarras: string | null
  unidadBase: Unidad
  imagenUrl: string | null
  precioUnitario: number
  presentaciones: Presentacion[]
}

export type PresentacionRequest = {
  unidadId: number
  factorConversion: number
  precioPresentacion?: number
}

export type ProductoRequest = {
  nombre: string
  descripcion?: string
  categoria?: string
  codigoBarras?: string
  unidadBaseId: number
  imagenUrl?: string
  precioUnitario: number
  presentaciones: PresentacionRequest[]
}

export function listarProductos() {
  return request<Producto[]>('/api/productos')
}

export function listarUnidades() {
  return request<Unidad[]>('/api/unidades-medida')
}

export function crearProducto(producto: ProductoRequest) {
  return request<Producto>('/api/productos', { method: 'POST', body: JSON.stringify(producto) })
}

export function actualizarProducto(id: number, producto: ProductoRequest) {
  return request<Producto>(`/api/productos/${id}`, { method: 'PUT', body: JSON.stringify(producto) })
}
