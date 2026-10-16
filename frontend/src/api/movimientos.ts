import { request } from './client'
import type { Unidad } from './productos'

export type Movimiento = {
  id: number
  productoId: number
  productoNombre: string
  tipo: 'ENTRADA' | 'SALIDA' | 'AJUSTE' | 'TRASLADO'
  cantidad: number
  unidad: Unidad
  ubicacionOrigenTipo: 'TIENDA' | 'ALMACEN'
  ubicacionOrigenId: number
  ubicacionOrigenNombre: string
  ubicacionDestinoTipo: 'TIENDA' | 'ALMACEN' | null
  ubicacionDestinoId: number | null
  ubicacionDestinoNombre: string | null
  usuarioId: number
  usuarioNombre: string
  estado: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO'
  aprobadoPorId: number | null
  aprobadoPorNombre: string | null
  fecha: string
  motivo: string | null
}

export function listarMovimientos(desde?: string, hasta?: string) {
  const params = new URLSearchParams()
  if (desde) params.set('desde', desde)
  if (hasta) params.set('hasta', hasta)
  const query = params.size ? `?${params}` : ''
  return request<Movimiento[]>(`/api/reportes/movimientos${query}`)
}
