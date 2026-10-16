import { ChevronLeft, ChevronRight, Package, RefreshCw, Search, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { actualizarProducto, crearProducto, listarProductos, listarUnidades, type Producto, type ProductoRequest, type Unidad } from '../../api/productos'
import { ApiError } from '../../api/client'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/StateViews'
import { PageHeader } from '../../components/ui/PageHeader'
import { ProductForm } from './ProductForm'

const pageSize = 8

function formatNumber(value: number) {
  return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 }).format(value)
}

function ProductSkeleton() {
  return <Card className="overflow-hidden p-0"><div className="hidden animate-pulse divide-y divide-stone-100 md:block">{Array.from({ length: 6 }, (_, index) => <div key={index} className="grid grid-cols-[minmax(14rem,2fr)_minmax(8rem,1fr)_minmax(7rem,0.8fr)_6rem] gap-5 px-5 py-5"><span className="h-5 rounded bg-stone-100" /><span className="h-5 rounded bg-stone-100" /><span className="h-5 rounded bg-stone-100" /><span className="h-5 rounded bg-stone-100" /></div>)}</div><div className="space-y-3 p-5 md:hidden">{Array.from({ length: 4 }, (_, index) => <div key={index} className="h-24 animate-pulse rounded-xl bg-stone-100" />)}</div></Card>
}

function ProductDetail({ product, canManage, onClose, onEdit }: { product: Producto; canManage: boolean; onClose: () => void; onEdit: () => void }) {
  useEffect(() => {
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose])

  return <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-0 sm:items-center sm:p-6" role="presentation" onMouseDown={onClose}>
    <section aria-labelledby="producto-detalle" aria-modal="true" className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-t-2xl bg-ivera-card p-5 shadow-2xl sm:rounded-2xl sm:p-6" role="dialog" onMouseDown={(event) => event.stopPropagation()}>
      <div className="flex items-start justify-between gap-4"><div className="min-w-0"><p className="text-sm font-semibold text-ivera-primary">Producto #{product.id}</p><h2 id="producto-detalle" className="mt-1 truncate text-xl font-bold text-ivera-text">{product.nombre}</h2></div><div className="flex shrink-0 gap-1">{canManage && <Button variant="secondary" onClick={onEdit}>Editar</Button>}<Button aria-label="Cerrar detalle" className="px-3" variant="ghost" onClick={onClose}><X className="size-5" /></Button></div></div>
      <div className="mt-6 grid gap-5 sm:grid-cols-[9rem_1fr]">{product.imagenUrl ? <img alt={product.nombre} className="h-36 w-full rounded-xl object-cover sm:h-32" src={product.imagenUrl} /> : <div className="grid h-28 place-items-center rounded-xl bg-orange-50 text-ivera-primary sm:h-32"><Package className="size-9" aria-hidden="true" /></div>}<dl className="grid grid-cols-2 gap-x-5 gap-y-4 text-sm"><div><dt className="text-ivera-muted">Categoría</dt><dd className="mt-1 font-semibold text-ivera-text">{product.categoria || 'Sin categoría'}</dd></div><div><dt className="text-ivera-muted">Código de barras</dt><dd className="mt-1 break-all font-semibold text-ivera-text">{product.codigoBarras || 'No registrado'}</dd></div><div><dt className="text-ivera-muted">Unidad base</dt><dd className="mt-1 font-semibold text-ivera-text">{product.unidadBase.nombre}</dd></div><div><dt className="text-ivera-muted">Precio unitario</dt><dd className="mt-1 font-semibold text-ivera-text">{formatNumber(product.precioUnitario)}</dd></div></dl></div>
      {product.descripcion && <div className="mt-6 border-t border-stone-100 pt-5"><h3 className="text-sm font-semibold text-ivera-text">Descripción</h3><p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-ivera-muted">{product.descripcion}</p></div>}
      <div className="mt-6 border-t border-stone-100 pt-5"><h3 className="text-sm font-semibold text-ivera-text">Presentaciones</h3>{product.presentaciones.length === 0 ? <p className="mt-2 text-sm text-ivera-muted">No hay presentaciones registradas.</p> : <ul className="mt-3 space-y-2">{product.presentaciones.map((presentation) => <li key={presentation.id} className="rounded-xl bg-stone-50 px-3 py-2.5 text-sm"><p className="font-semibold text-ivera-text">{presentation.unidad.nombre}</p><p className="mt-1 text-ivera-muted">{presentation.equivalencia}</p>{presentation.precioPresentacion !== null && <p className="mt-1 text-ivera-muted">Precio: {formatNumber(presentation.precioPresentacion)}</p>}</li>)}</ul>}</div>
    </section>
  </div>
}

export function ProductsPage() {
  const { session } = useAuth()
  const [products, setProducts] = useState<Producto[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(1)
  const [selected, setSelected] = useState<Producto | null>(null)
  const [formProduct, setFormProduct] = useState<Producto | null | 'new'>(null)
  const [units, setUnits] = useState<Unidad[] | null>(null)
  const [unitsError, setUnitsError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const loadedInitially = useRef(false)

  async function refresh() {
    setError(null)
    try {
      setProducts(await listarProductos())
    } catch (reason) {
      setProducts(null)
      setError(reason instanceof ApiError ? reason.message : 'No se pudo cargar el catálogo.')
    }
  }

  useEffect(() => {
    if (loadedInitially.current) return
    loadedInitially.current = true
    void refresh()
  }, [])

  const categories = Array.from(new Set((products ?? []).map((product) => product.categoria).filter((value): value is string => Boolean(value)))).sort((a, b) => a.localeCompare(b, 'es'))
  const normalizedQuery = query.trim().toLocaleLowerCase('es-PE')
  const filtered = (products ?? []).filter((product) => {
    const matchesQuery = !normalizedQuery || [product.nombre, product.codigoBarras, String(product.id)].some((value) => value?.toLocaleLowerCase('es-PE').includes(normalizedQuery))
    return matchesQuery && (!category || product.categoria === category)
  })
  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize))
  const currentPage = Math.min(page, totalPages)
  const visibleProducts = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize)

  function updateQuery(value: string) {
    setQuery(value)
    setPage(1)
  }

  function updateCategory(value: string) {
    setCategory(value)
    setPage(1)
  }

  async function openForm(product: Producto | 'new') {
    setUnitsError(null)
    try {
      const availableUnits = units ?? await listarUnidades()
      setUnits(availableUnits)
      setSelected(null)
      setFormProduct(product)
    } catch (reason) {
      setUnitsError(reason instanceof ApiError ? reason.message : 'No se pudieron cargar las unidades de medida.')
    }
  }

  async function saveProduct(request: ProductoRequest) {
    if (formProduct === 'new') {
      const created = await crearProducto(request)
      setProducts((current) => current ? [created, ...current] : [created])
      setSuccess('Producto creado correctamente.')
    } else if (formProduct) {
      const updated = await actualizarProducto(formProduct.id, request)
      setProducts((current) => current?.map((product) => product.id === updated.id ? updated : product) ?? [updated])
      setSuccess('Producto actualizado correctamente.')
    }
    setFormProduct(null)
  }

  const canManage = session?.rol === 'ADMINISTRADOR'

  return <div className="space-y-6">
    <PageHeader title="Productos" description="Consulta el catálogo compartido y sus presentaciones disponibles." action={<div className="flex flex-wrap gap-2">{canManage && <Button onClick={() => void openForm('new')}>Nuevo producto</Button>}<Button variant="secondary" onClick={() => void refresh()}><RefreshCw className="size-4" />Actualizar</Button></div>} />
    {success && <div aria-live="polite" className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-green-50 px-4 py-3 text-sm text-green-700"><span>{success}</span><Button variant="ghost" onClick={() => setSuccess(null)}>Cerrar</Button></div>}
    {unitsError && <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700"><span>{unitsError}</span><Button variant="ghost" onClick={() => void openForm('new')}>Reintentar</Button></div>}
    <Card className="p-4 sm:p-5"><div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between"><div className="grid flex-1 gap-3 sm:grid-cols-[minmax(0,1fr)_13rem]"><label className="relative block"><span className="sr-only">Buscar producto</span><Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ivera-muted" aria-hidden="true" /><input className="w-full rounded-xl border border-stone-200 bg-white py-2.5 pl-10 pr-3 text-sm text-ivera-text placeholder:text-ivera-muted focus:border-ivera-primary" onChange={(event) => updateQuery(event.target.value)} placeholder="Buscar por nombre, código o ID" type="search" value={query} /></label>{categories.length > 0 && <label><span className="sr-only">Filtrar por categoría</span><select className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm text-ivera-text focus:border-ivera-primary" onChange={(event) => updateCategory(event.target.value)} value={category}><option value="">Todas las categorías</option>{categories.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>}</div><p aria-live="polite" className="text-sm text-ivera-muted">{products === null ? 'Cargando resultados...' : `${filtered.length} ${filtered.length === 1 ? 'resultado' : 'resultados'}`}</p></div></Card>
    {products === null && !error ? <ProductSkeleton /> : error ? <div className="space-y-3"><ErrorState description={error} /><div className="text-center"><Button variant="secondary" onClick={() => void refresh()}><RefreshCw className="size-4" />Reintentar</Button></div></div> : filtered.length === 0 ? <EmptyState title="No se encontraron productos" description={query || category ? 'Prueba con otra búsqueda o categoría.' : 'El catálogo aún no contiene productos.'} /> : <Card className="overflow-hidden p-0"><div className="hidden overflow-x-auto md:block"><table className="w-full text-left text-sm"><thead className="border-b border-stone-200 bg-stone-50 text-xs uppercase tracking-wide text-ivera-muted"><tr><th className="px-5 py-3 font-semibold">Producto</th><th className="px-5 py-3 font-semibold">Categoría</th><th className="px-5 py-3 font-semibold">Unidad base</th><th className="px-5 py-3 text-right font-semibold">Precio unitario</th></tr></thead><tbody className="divide-y divide-stone-100">{visibleProducts.map((product) => <tr key={product.id} className="cursor-pointer transition-colors hover:bg-orange-50/50 focus-within:bg-orange-50/50"><td className="px-5 py-4"><button className="max-w-full text-left" onClick={() => setSelected(product)}><span className="block truncate font-semibold text-ivera-text">{product.nombre}</span><span className="mt-1 block truncate text-xs text-ivera-muted">ID {product.id}{product.codigoBarras ? ` · ${product.codigoBarras}` : ''}</span></button></td><td className="px-5 py-4 text-ivera-muted">{product.categoria || 'Sin categoría'}</td><td className="px-5 py-4 text-ivera-muted">{product.unidadBase.nombre}</td><td className="px-5 py-4 text-right font-semibold text-ivera-text">{formatNumber(product.precioUnitario)}</td></tr>)}</tbody></table></div><div className="divide-y divide-stone-100 md:hidden">{visibleProducts.map((product) => <button key={product.id} className="block w-full px-5 py-4 text-left hover:bg-orange-50/50" onClick={() => setSelected(product)}><div className="flex items-start justify-between gap-4"><div className="min-w-0"><p className="truncate font-semibold text-ivera-text">{product.nombre}</p><p className="mt-1 text-xs text-ivera-muted">ID {product.id}{product.codigoBarras ? ` · ${product.codigoBarras}` : ''}</p></div><span className="shrink-0 font-semibold text-ivera-text">{formatNumber(product.precioUnitario)}</span></div><p className="mt-2 text-sm text-ivera-muted">{product.categoria || 'Sin categoría'} · {product.unidadBase.nombre}</p></button>)}</div><div className="flex items-center justify-between border-t border-stone-100 px-4 py-3"><p className="text-sm text-ivera-muted">Página {currentPage} de {totalPages}</p><div className="flex gap-2"><Button aria-label="Página anterior" className="px-3" disabled={currentPage === 1} variant="ghost" onClick={() => setPage(currentPage - 1)}><ChevronLeft className="size-4" /></Button><Button aria-label="Página siguiente" className="px-3" disabled={currentPage === totalPages} variant="ghost" onClick={() => setPage(currentPage + 1)}><ChevronRight className="size-4" /></Button></div></div></Card>}
    {selected && <ProductDetail canManage={canManage} product={selected} onClose={() => setSelected(null)} onEdit={() => void openForm(selected)} />}
    {formProduct && units && <ProductForm product={formProduct === 'new' ? undefined : formProduct} units={units} onClose={() => setFormProduct(null)} onSaved={saveProduct} />}
  </div>
}
