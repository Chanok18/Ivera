import { request } from './client'
import type { Movimiento } from './movimientos'

export type StockBajo = {
  productoId: number
  productoNombre: string
  codigoBarras: string | null
  stockTotal: number
}

export type DiferenciaInventario = {
  conteoId: number
  productoId: number
  productoNombre: string
  ubicacionTipo: 'TIENDA' | 'ALMACEN'
  ubicacionId: number
  ubicacionNombre: string
  cantidadContada: number
  cantidadSistema: number
  diferencia: number
  fecha: string
}

export type ProductoMasVendido = {
  productoId: number
  productoNombre: string
  cantidadTotalVendida: number
}

function query(params: Record<string, string | undefined>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value) search.set(key, value)
  })
  return search.size ? `?${search}` : ''
}

export function obtenerStockBajo(umbral?: string) {
  return request<StockBajo[]>(`/api/reportes/stock-bajo${query({ umbral })}`)
}

export function obtenerDiferencias(desde?: string, hasta?: string) {
  return request<DiferenciaInventario[]>(`/api/reportes/diferencias-inventario${query({ desde, hasta })}`)
}

export function obtenerMovimientos(desde?: string, hasta?: string) {
  return request<Movimiento[]>(`/api/reportes/movimientos${query({ desde, hasta })}`)
}

export function obtenerProductosMasVendidos(limite?: string) {
  return request<ProductoMasVendido[]>(`/api/reportes/productos-mas-vendidos${query({ limite })}`)
}
