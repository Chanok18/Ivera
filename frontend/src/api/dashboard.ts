import { request } from './client'

export type Product = { id: number }

export type LowStockProduct = {
  productoId: number
  productoNombre: string
  codigoBarras: string | null
  stockTotal: number
}

export type InventoryDifference = {
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

export type Movement = {
  id: number
  productoNombre: string
  tipo: 'ENTRADA' | 'SALIDA' | 'AJUSTE' | 'TRASLADO'
  cantidad: number
  unidad: { nombre: string }
  ubicacionOrigenNombre: string
  ubicacionDestinoNombre: string | null
  estado: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO'
  fecha: string
}

export type TopSellingProduct = {
  productoId: number
  productoNombre: string
  cantidadTotalVendida: number
}

export type DashboardSection<T> = {
  data: T | null
  error: string | null
}

async function loadSection<T>(path: string): Promise<DashboardSection<T>> {
  try {
    return { data: await request<T>(path), error: null }
  } catch (reason) {
    return {
      data: null,
      error: reason instanceof Error ? reason.message : 'La solicitud no pudo completarse.',
    }
  }
}

export function loadDashboard() {
  return Promise.all([
    loadSection<Product[]>('/api/productos'),
    loadSection<LowStockProduct[]>('/api/reportes/stock-bajo'),
    loadSection<InventoryDifference[]>('/api/reportes/diferencias-inventario'),
    loadSection<Movement[]>('/api/reportes/movimientos'),
    loadSection<TopSellingProduct[]>('/api/reportes/productos-mas-vendidos?limite=5'),
  ])
}
