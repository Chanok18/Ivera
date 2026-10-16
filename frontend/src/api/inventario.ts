import { request } from './client'

export type StockUbicacion = {
  ubicacionId: number
  ubicacionTipo: 'TIENDA' | 'ALMACEN'
  ubicacionNombre: string
  cantidad: number
}

export function consultarStockProducto(productoId: number) {
  return request<StockUbicacion[]>(`/api/stock/${productoId}`)
}
