import { Building2, PackageSearch, RefreshCw, Search, Warehouse } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { ApiError } from '../../api/client'
import { consultarStockProducto, type StockUbicacion } from '../../api/inventario'
import { listarProductos, type Producto } from '../../api/productos'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/StateViews'
import { PageHeader } from '../../components/ui/PageHeader'

function formatQuantity(value: number) {
  return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 }).format(value)
}

function InventorySkeleton() {
  return <Card className="overflow-hidden p-0"><div className="hidden animate-pulse divide-y divide-stone-100 md:block">{Array.from({ length: 3 }, (_, index) => <div key={index} className="grid grid-cols-[1fr_9rem_8rem] gap-5 px-5 py-5"><span className="h-5 rounded bg-stone-100" /><span className="h-5 rounded bg-stone-100" /><span className="h-5 rounded bg-stone-100" /></div>)}</div><div className="space-y-3 p-5 md:hidden">{Array.from({ length: 3 }, (_, index) => <div key={index} className="h-20 animate-pulse rounded-xl bg-stone-100" />)}</div></Card>
}

export function InventoryPage() {
  const [products, setProducts] = useState<Producto[] | null>(null)
  const [productsError, setProductsError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [selectedProduct, setSelectedProduct] = useState<Producto | null>(null)
  const [stock, setStock] = useState<StockUbicacion[] | null>(null)
  const [stockError, setStockError] = useState<string | null>(null)
  const [locationType, setLocationType] = useState<'TIENDA' | 'ALMACEN' | ''>('')
  const loadedInitially = useRef(false)

  async function refreshProducts() {
    setProductsError(null)
    try {
      setProducts(await listarProductos())
    } catch (reason) {
      setProducts(null)
      setProductsError(reason instanceof ApiError ? reason.message : 'No se pudo cargar el catálogo.')
    }
  }

  async function loadStock(product: Producto) {
    setSelectedProduct(product)
    setStock(null)
    setStockError(null)
    setLocationType('')
    try {
      setStock(await consultarStockProducto(product.id))
    } catch (reason) {
      setStockError(reason instanceof ApiError ? reason.message : 'No se pudo cargar el stock del producto.')
    }
  }

  useEffect(() => {
    if (loadedInitially.current) return
    loadedInitially.current = true
    void refreshProducts()
  }, [])

  const normalizedQuery = query.trim().toLocaleLowerCase('es-PE')
  const matchingProducts = (products ?? []).filter((product) => !normalizedQuery || [product.nombre, product.codigoBarras, String(product.id)].some((value) => value?.toLocaleLowerCase('es-PE').includes(normalizedQuery)))
  const visibleStock = (stock ?? []).filter((item) => !locationType || item.ubicacionTipo === locationType)

  return <div className="space-y-6">
    <PageHeader title="Inventario" description="Consulta las existencias por ubicación para cada producto del catálogo." action={<Button variant="secondary" onClick={() => void (selectedProduct ? loadStock(selectedProduct) : refreshProducts())}><RefreshCw className="size-4" />Actualizar</Button>} />
    <Card className="p-4 sm:p-5"><label className="relative block"><span className="sr-only">Buscar producto para consultar inventario</span><Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ivera-muted" aria-hidden="true" /><input className="w-full rounded-xl border border-stone-200 bg-white py-2.5 pl-10 pr-3 text-sm text-ivera-text placeholder:text-ivera-muted focus:border-ivera-primary" onChange={(event) => setQuery(event.target.value)} placeholder="Buscar producto por nombre, código o ID" type="search" value={query} /></label>{products && <p aria-live="polite" className="mt-3 text-sm text-ivera-muted">{matchingProducts.length} {matchingProducts.length === 1 ? 'producto disponible' : 'productos disponibles'} para consulta</p>}<div className="mt-4 max-h-56 overflow-y-auto rounded-xl border border-stone-100">{products === null && !productsError ? <div className="space-y-3 p-4 animate-pulse">{Array.from({ length: 3 }, (_, index) => <div key={index} className="h-12 rounded-xl bg-stone-100" />)}</div> : productsError ? <div className="p-4"><ErrorState description={productsError} /><div className="mt-3 text-center"><Button variant="secondary" onClick={() => void refreshProducts()}>Reintentar</Button></div></div> : matchingProducts.length === 0 ? <p className="p-4 text-sm text-ivera-muted">No hay productos que coincidan con la búsqueda.</p> : matchingProducts.map((product) => <button key={product.id} className={`flex w-full items-center justify-between gap-4 border-b border-stone-100 px-4 py-3 text-left last:border-b-0 ${selectedProduct?.id === product.id ? 'bg-orange-50' : 'hover:bg-stone-50'}`} onClick={() => void loadStock(product)}><span className="min-w-0"><span className="block truncate text-sm font-semibold text-ivera-text">{product.nombre}</span><span className="block truncate text-xs text-ivera-muted">ID {product.id}{product.codigoBarras ? ` · ${product.codigoBarras}` : ''}</span></span><span className="shrink-0 text-xs font-medium text-ivera-primary">Consultar</span></button>)}</div></Card>
    {!selectedProduct ? <EmptyState title="Selecciona un producto" description="Elige un producto del catálogo para consultar su stock por ubicación." /> : <><Card className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-center gap-3"><span className="grid size-10 place-items-center rounded-xl bg-orange-50 text-ivera-primary"><PackageSearch className="size-5" /></span><div><p className="text-sm text-ivera-muted">Existencias de</p><h2 className="font-semibold text-ivera-text">{selectedProduct.nombre}</h2><p className="text-xs text-ivera-muted">Unidad base: {selectedProduct.unidadBase.nombre}</p></div></div>{stock && <label className="text-sm text-ivera-muted"><span className="sr-only">Filtrar ubicaciones por tipo</span><select className="rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm text-ivera-text" onChange={(event) => setLocationType(event.target.value as 'TIENDA' | 'ALMACEN' | '')} value={locationType}><option value="">Todas las ubicaciones</option><option value="TIENDA">Tiendas</option><option value="ALMACEN">Almacenes</option></select></label>}</Card>{stock === null && !stockError ? <InventorySkeleton /> : stockError ? <div className="space-y-3"><ErrorState description={stockError} /><div className="text-center"><Button variant="secondary" onClick={() => void loadStock(selectedProduct)}><RefreshCw className="size-4" />Reintentar</Button></div></div> : visibleStock.length === 0 ? <EmptyState title="Sin ubicaciones para mostrar" description="El filtro actual no tiene existencias devueltas para este producto." /> : <Card className="overflow-hidden p-0"><div className="hidden md:block"><table className="w-full text-left text-sm"><thead className="border-b border-stone-200 bg-stone-50 text-xs uppercase tracking-wide text-ivera-muted"><tr><th className="px-5 py-3 font-semibold">Ubicación</th><th className="px-5 py-3 font-semibold">Tipo</th><th className="px-5 py-3 text-right font-semibold">Cantidad</th></tr></thead><tbody className="divide-y divide-stone-100">{visibleStock.map((item) => <tr key={`${item.ubicacionTipo}-${item.ubicacionId}`}><td className="px-5 py-4 font-semibold text-ivera-text">{item.ubicacionNombre}</td><td className="px-5 py-4 text-ivera-muted">{item.ubicacionTipo === 'TIENDA' ? 'Tienda' : 'Almacén'}</td><td className="px-5 py-4 text-right font-semibold text-ivera-text">{formatQuantity(item.cantidad)} <span className="text-xs font-normal text-ivera-muted">{selectedProduct.unidadBase.nombre}</span></td></tr>)}</tbody></table></div><div className="divide-y divide-stone-100 md:hidden">{visibleStock.map((item) => <div key={`${item.ubicacionTipo}-${item.ubicacionId}`} className="flex items-center gap-3 px-5 py-4"><span className="grid size-9 place-items-center rounded-xl bg-orange-50 text-ivera-primary">{item.ubicacionTipo === 'TIENDA' ? <Building2 className="size-4" /> : <Warehouse className="size-4" />}</span><span className="min-w-0 flex-1"><span className="block truncate font-semibold text-ivera-text">{item.ubicacionNombre}</span><span className="text-xs text-ivera-muted">{item.ubicacionTipo === 'TIENDA' ? 'Tienda' : 'Almacén'}</span></span><span className="text-right font-semibold text-ivera-text">{formatQuantity(item.cantidad)}<span className="ml-1 block text-xs font-normal text-ivera-muted">{selectedProduct.unidadBase.nombre}</span></span></div>)}</div></Card>}</>}
  </div>
}
