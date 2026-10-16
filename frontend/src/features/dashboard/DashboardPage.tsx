import { AlertTriangle, ArrowRight, ClipboardCheck, PackageSearch, RefreshCw, ScanLine, Trophy } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { loadDashboard, type InventoryDifference, type LowStockProduct, type Movement, type TopSellingProduct } from '../../api/dashboard'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/StateViews'
import { PageHeader } from '../../components/ui/PageHeader'

type DashboardData = {
  products: number | null
  lowStock: LowStockProduct[] | null
  differences: InventoryDifference[] | null
  movements: Movement[] | null
  topSelling: TopSellingProduct[] | null
}

type DashboardErrors = {
  products: string | null
  lowStock: string | null
  differences: string | null
  movements: string | null
  topSelling: string | null
}

function formatQuantity(value: number) {
  return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 }).format(value)
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function LoadingCard() {
  return <Card className="animate-pulse"><div className="h-4 w-28 rounded bg-stone-100" /><div className="mt-5 h-8 w-16 rounded bg-stone-100" /><div className="mt-3 h-5 w-24 rounded bg-stone-100" /></Card>
}

export function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [errors, setErrors] = useState<DashboardErrors | null>(null)
  const [loading, setLoading] = useState(true)
  const hasLoadedInitially = useRef(false)

  async function refresh() {
    setLoading(true)
    const [products, lowStock, differences, movements, topSelling] = await loadDashboard()
    setData({
      products: products.data?.length ?? null,
      lowStock: lowStock.data,
      differences: differences.data,
      movements: movements.data,
      topSelling: topSelling.data,
    })
    setErrors({
      products: products.error,
      lowStock: lowStock.error,
      differences: differences.error,
      movements: movements.error,
      topSelling: topSelling.error,
    })
    setLoading(false)
  }

  useEffect(() => {
    if (hasLoadedInitially.current) return
    hasLoadedInitially.current = true
    void refresh()
  }, [])

  const indicators = data ? [
    ['Productos registrados', PackageSearch, data.products ?? '—', errors?.products ?? 'Catálogo disponible', 'neutral'],
    ['Stock por revisar', AlertTriangle, data.lowStock?.length ?? '—', errors?.lowStock ?? 'Con umbral de 10', data.lowStock?.length ? 'warning' : 'success'],
    ['Diferencias de conteo', ClipboardCheck, data.differences?.length ?? '—', errors?.differences ?? 'Reportadas por conteos', data.differences?.length ? 'warning' : 'success'],
    ['Movimientos registrados', ScanLine, data.movements?.length ?? '—', errors?.movements ?? 'Sin filtro de fecha', 'neutral'],
  ] as const : []

  return (
    <div className="space-y-6 sm:space-y-8">
      <PageHeader title="Vista general" description="Resumen operativo con datos disponibles para tu nivel de acceso." action={<Button variant="secondary" onClick={() => void refresh()}><RefreshCw className="size-4" />Actualizar</Button>} />
       <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
         {data && !loading ? indicators.map(([label, Icon, value, detail, tone]) => <Card key={label} className="relative overflow-hidden"><Icon className="absolute right-5 top-5 size-5 text-orange-200" aria-hidden="true" /><p className="text-sm font-medium text-ivera-muted">{label}</p><p className="mt-4 text-3xl font-bold tracking-tight text-ivera-text">{value}</p><div className="mt-3"><Badge tone={tone}>{detail}</Badge></div></Card>) : <>{Array.from({ length: 4 }, (_, index) => <LoadingCard key={index} />)}</>}
       </div>
      <div className="grid gap-6 xl:grid-cols-[1.4fr_1fr]">
        <Card>
          <div className="flex items-start justify-between gap-4"><div><h2 className="font-semibold text-ivera-text">Movimientos recientes</h2><p className="mt-1 text-sm text-ivera-muted">Registro devuelto por el reporte, del más reciente al más antiguo.</p></div><ScanLine className="size-5 text-ivera-primary" aria-hidden="true" /></div>
           {!data || loading ? <div className="mt-6 space-y-3 animate-pulse">{Array.from({ length: 4 }, (_, index) => <div key={index} className="h-13 rounded-xl bg-stone-100" />)}</div> : errors?.movements ? <div className="mt-5"><ErrorState description={errors.movements} /></div> : data.movements?.length === 0 ? <div className="mt-5"><EmptyState title="Sin movimientos registrados" description="Los movimientos aparecerán aquí cuando el reporte tenga resultados." /></div> : <div className="mt-5 divide-y divide-stone-100">{data.movements?.slice(0, 5).map((movement) => <div key={movement.id} className="flex items-center gap-3 py-3"><span className="grid size-9 shrink-0 place-items-center rounded-xl bg-orange-50 text-ivera-primary"><ScanLine className="size-4" /></span><div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold text-ivera-text">{movement.productoNombre}</p><p className="truncate text-xs text-ivera-muted">{movement.tipo} · {formatQuantity(movement.cantidad)} {movement.unidad.nombre} · {movement.ubicacionOrigenNombre}</p></div><div className="text-right"><Badge tone={movement.estado === 'APROBADO' ? 'success' : movement.estado === 'PENDIENTE' ? 'warning' : 'danger'}>{movement.estado}</Badge><p className="mt-1 text-xs text-ivera-muted">{formatDate(movement.fecha)}</p></div></div>)}</div>}
        </Card>
        <Card>
          <div className="flex items-start justify-between gap-4"><div><h2 className="font-semibold text-ivera-text">Productos más vendidos</h2><p className="mt-1 text-sm text-ivera-muted">Top 5 calculado por el backend.</p></div><Trophy className="size-5 text-ivera-primary" aria-hidden="true" /></div>
           {!data || loading ? <div className="mt-6 space-y-3 animate-pulse">{Array.from({ length: 4 }, (_, index) => <div key={index} className="h-10 rounded-xl bg-stone-100" />)}</div> : errors?.topSelling ? <div className="mt-5"><ErrorState description={errors.topSelling} /></div> : data.topSelling?.length === 0 ? <div className="mt-5"><EmptyState title="Sin ventas para mostrar" description="Este listado se llenará cuando existan ventas registradas." /></div> : <ol className="mt-5 space-y-3">{data.topSelling?.map((product, index) => <li key={product.productoId} className="flex items-center gap-3"><span className="grid size-7 shrink-0 place-items-center rounded-full bg-stone-100 text-xs font-bold text-ivera-muted">{index + 1}</span><span className="min-w-0 flex-1 truncate text-sm font-medium text-ivera-text">{product.productoNombre}</span><span className="text-sm font-semibold text-ivera-primary">{formatQuantity(product.cantidadTotalVendida)}</span></li>)}</ol>}
        </Card>
      </div>
      <div className="grid gap-6 xl:grid-cols-2">
         <Card><div className="flex items-start justify-between gap-4"><div><h2 className="font-semibold text-ivera-text">Stock por revisar</h2><p className="mt-1 text-sm text-ivera-muted">Productos con stock total menor o igual a 10.</p></div><AlertTriangle className="size-5 text-ivera-warning" aria-hidden="true" /></div>{!data || loading ? <div className="mt-5 h-24 animate-pulse rounded-xl bg-stone-100" /> : errors?.lowStock ? <div className="mt-5"><ErrorState description={errors.lowStock} /></div> : data.lowStock?.length === 0 ? <div className="mt-5"><EmptyState title="Sin stock bajo" description="El reporte no devolvió productos bajo el umbral actual." /></div> : <div className="mt-4 overflow-x-auto"><table className="w-full text-left text-sm"><thead className="text-xs uppercase tracking-wide text-ivera-muted"><tr><th className="pb-3 font-semibold">Producto</th><th className="pb-3 text-right font-semibold">Stock total</th></tr></thead><tbody>{data.lowStock?.slice(0, 5).map((product) => <tr key={product.productoId} className="border-t border-stone-100"><td className="py-3 font-medium text-ivera-text">{product.productoNombre}</td><td className="py-3 text-right font-semibold text-amber-700">{formatQuantity(product.stockTotal)}</td></tr>)}</tbody></table></div>}</Card>
         <Card><div className="flex items-start justify-between gap-4"><div><h2 className="font-semibold text-ivera-text">Diferencias de inventario</h2><p className="mt-1 text-sm text-ivera-muted">Resultados registrados en conteos físicos.</p></div><ClipboardCheck className="size-5 text-ivera-primary" aria-hidden="true" /></div>{!data || loading ? <div className="mt-5 h-24 animate-pulse rounded-xl bg-stone-100" /> : errors?.differences ? <div className="mt-5"><ErrorState description={errors.differences} /></div> : data.differences?.length === 0 ? <div className="mt-5"><EmptyState title="Sin diferencias reportadas" description="No hay diferencias de conteo disponibles para tu alcance." /></div> : <div className="mt-4 space-y-3">{data.differences?.slice(0, 4).map((difference) => <div key={`${difference.conteoId}-${difference.productoId}`} className="flex items-center gap-3 rounded-xl bg-stone-50 p-3"><span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold text-ivera-text">{difference.productoNombre}</span><span className="block truncate text-xs text-ivera-muted">{difference.ubicacionNombre}</span></span><Badge tone="warning">{formatQuantity(difference.diferencia)}</Badge></div>)}</div>}</Card>
      </div>
      <Card className="flex flex-col justify-between gap-4 border border-orange-100 bg-orange-50/55 sm:flex-row sm:items-center"><div><p className="font-semibold text-ivera-text">Información por ubicación</p><p className="mt-1 text-sm text-ivera-muted">Romel, Todopernos y Almacén Central se aplican según los permisos que valide el backend.</p></div><Link className="inline-flex items-center gap-2 text-sm font-semibold text-ivera-primary hover:text-ivera-primary-dark" to="/reportes">Ver reportes <ArrowRight className="size-4" /></Link></Card>
    </div>
  )
}
