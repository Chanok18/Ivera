import { AlertTriangle, BarChart3, ClipboardCheck, RefreshCw, ScanLine, Trophy } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { obtenerDiferencias, obtenerMovimientos, obtenerProductosMasVendidos, obtenerStockBajo, type DiferenciaInventario, type ProductoMasVendido, type StockBajo } from '../../api/reportes'
import type { Movimiento } from '../../api/movimientos'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/StateViews'
import { PageHeader } from '../../components/ui/PageHeader'

type Section<T> = { data: T[] | null; error: string | null }
type ReportData = { stock: Section<StockBajo>; differences: Section<DiferenciaInventario>; movements: Section<Movimiento>; topSelling: Section<ProductoMasVendido> }
type Filters = { desde: string; hasta: string; umbral: string; limite: string }

const initialFilters: Filters = { desde: '', hasta: '', umbral: '10', limite: '5' }

async function loadSection<T>(load: () => Promise<T[]>): Promise<Section<T>> {
  try {
    return { data: await load(), error: null }
  } catch (reason) {
    return { data: null, error: reason instanceof Error ? reason.message : 'La solicitud no pudo completarse.' }
  }
}

async function loadReports(filters: Filters): Promise<ReportData> {
  const [stock, differences, movements, topSelling] = await Promise.all([
    loadSection(() => obtenerStockBajo(filters.umbral)),
    loadSection(() => obtenerDiferencias(filters.desde || undefined, filters.hasta || undefined)),
    loadSection(() => obtenerMovimientos(filters.desde || undefined, filters.hasta || undefined)),
    loadSection(() => obtenerProductosMasVendidos(filters.limite)),
  ])
  return { stock, differences, movements, topSelling }
}

function formatQuantity(value: number) {
  return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 }).format(value)
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function ReportSkeleton() {
  return <div className="space-y-3 animate-pulse">{Array.from({ length: 4 }, (_, index) => <div key={index} className="h-12 rounded-xl bg-stone-100" />)}</div>
}

function SectionTitle({ icon: Icon, title, description }: { icon: typeof AlertTriangle; title: string; description: string }) {
  return <div className="flex items-start justify-between gap-4"><div><h2 className="font-semibold text-ivera-text">{title}</h2><p className="mt-1 text-sm text-ivera-muted">{description}</p></div><Icon className="size-5 shrink-0 text-ivera-primary" aria-hidden="true" /></div>
}

export function ReportsPage() {
  const [filters, setFilters] = useState(initialFilters)
  const [reports, setReports] = useState<ReportData | null>(null)
  const [loading, setLoading] = useState(true)
  const [filterError, setFilterError] = useState<string | null>(null)
  const loadedInitially = useRef(false)

  async function refresh() {
    setFilterError(null)
    if (filters.desde && filters.hasta && filters.desde > filters.hasta) {
      setFilterError('La fecha inicial no puede ser posterior a la fecha final.')
      return
    }
    if (!Number.isFinite(Number(filters.umbral)) || Number(filters.umbral) < 0) {
      setFilterError('El umbral debe ser un número mayor o igual a cero.')
      return
    }
    if (!Number.isInteger(Number(filters.limite)) || Number(filters.limite) <= 0) {
      setFilterError('El límite de productos vendidos debe ser un entero positivo.')
      return
    }
    setLoading(true)
    setReports(await loadReports(filters))
    setLoading(false)
  }

  useEffect(() => {
    if (loadedInitially.current) return
    loadedInitially.current = true
    void loadReports(initialFilters).then((data) => {
      setReports(data)
      setLoading(false)
    })
  }, [])

  function updateFilter(key: keyof Filters, value: string) {
    setFilters((current) => ({ ...current, [key]: value }))
  }

  return <div className="space-y-6">
    <PageHeader title="Reportes" description="Consulta indicadores operativos según el alcance que autoriza el backend." action={<Button variant="secondary" onClick={() => void refresh()}><RefreshCw className="size-4" />Actualizar</Button>} />
    <Card className="p-4 sm:p-5"><div className="grid gap-3 lg:grid-cols-[10rem_10rem_8rem_8rem_auto]"><label><span className="mb-1 block text-xs font-medium text-ivera-muted">Desde</span><input className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm text-ivera-text" onChange={(event) => updateFilter('desde', event.target.value)} type="date" value={filters.desde} /></label><label><span className="mb-1 block text-xs font-medium text-ivera-muted">Hasta</span><input className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm text-ivera-text" onChange={(event) => updateFilter('hasta', event.target.value)} type="date" value={filters.hasta} /></label><label><span className="mb-1 block text-xs font-medium text-ivera-muted">Umbral stock</span><input className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm text-ivera-text" min="0" onChange={(event) => updateFilter('umbral', event.target.value)} type="number" value={filters.umbral} /></label><label><span className="mb-1 block text-xs font-medium text-ivera-muted">Top vendidos</span><input className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm text-ivera-text" min="1" onChange={(event) => updateFilter('limite', event.target.value)} step="1" type="number" value={filters.limite} /></label><div className="flex items-end"><Button variant="secondary" onClick={() => void refresh()}>Aplicar filtros</Button></div></div>{filterError && <p aria-live="polite" className="mt-3 text-sm text-red-700">{filterError}</p>}<p className="mt-3 text-xs text-ivera-muted">Las fechas se aplican a diferencias y movimientos. El umbral y el límite se envían solo a sus reportes respectivos.</p></Card>
    <div className="grid gap-6 xl:grid-cols-2">
      <Card><SectionTitle icon={AlertTriangle} title="Stock bajo" description={`Productos con stock total menor o igual a ${filters.umbral || '0'}.`} />{loading ? <div className="mt-5"><ReportSkeleton /></div> : reports?.stock.error ? <div className="mt-5"><ErrorState title="No se pudo cargar stock bajo" description={reports.stock.error} /></div> : reports?.stock.data?.length === 0 ? <div className="mt-5"><EmptyState title="Sin stock bajo" description="El reporte no devolvió productos para el umbral actual." /></div> : <div className="mt-5 overflow-x-auto"><table className="w-full text-left text-sm"><thead className="text-xs uppercase tracking-wide text-ivera-muted"><tr><th className="pb-3 font-semibold">Producto</th><th className="pb-3 text-right font-semibold">Stock total</th></tr></thead><tbody>{reports?.stock.data?.map((item) => <tr key={item.productoId} className="border-t border-stone-100"><td className="py-3"><p className="font-medium text-ivera-text">{item.productoNombre}</p>{item.codigoBarras && <p className="text-xs text-ivera-muted">{item.codigoBarras}</p>}</td><td className="py-3 text-right font-semibold text-amber-700">{formatQuantity(item.stockTotal)}</td></tr>)}</tbody></table></div>}</Card>
      <Card><SectionTitle icon={Trophy} title="Productos más vendidos" description={`Top ${filters.limite || '0'} calculado por el backend.`} />{loading ? <div className="mt-5"><ReportSkeleton /></div> : reports?.topSelling.error ? <div className="mt-5"><ErrorState title="No se pudo cargar ventas" description={reports.topSelling.error} /></div> : reports?.topSelling.data?.length === 0 ? <div className="mt-5"><EmptyState title="Sin ventas para mostrar" description="No hay resultados disponibles para tu alcance." /></div> : <ol className="mt-5 space-y-3">{reports?.topSelling.data?.map((item, index) => <li key={item.productoId} className="flex items-center gap-3"><span className="grid size-7 shrink-0 place-items-center rounded-full bg-stone-100 text-xs font-bold text-ivera-muted">{index + 1}</span><span className="min-w-0 flex-1 truncate text-sm font-medium text-ivera-text">{item.productoNombre}</span><span className="text-sm font-semibold text-ivera-primary">{formatQuantity(item.cantidadTotalVendida)}</span></li>)}</ol>}</Card>
    </div>
    <Card className="overflow-hidden p-0"><div className="p-5"><SectionTitle icon={ClipboardCheck} title="Diferencias de inventario" description="Diferencias registradas en conteos físicos dentro del rango seleccionado." /></div>{loading ? <div className="px-5 pb-5"><ReportSkeleton /></div> : reports?.differences.error ? <div className="px-5 pb-5"><ErrorState title="No se pudieron cargar las diferencias" description={reports.differences.error} /></div> : reports?.differences.data?.length === 0 ? <div className="px-5 pb-5"><EmptyState title="Sin diferencias reportadas" description="El reporte no devolvió diferencias para el rango actual." /></div> : <><div className="hidden overflow-x-auto md:block"><table className="w-full text-left text-sm"><thead className="border-y border-stone-200 bg-stone-50 text-xs uppercase tracking-wide text-ivera-muted"><tr><th className="px-5 py-3 font-semibold">Producto</th><th className="px-5 py-3 font-semibold">Ubicación</th><th className="px-5 py-3 text-right font-semibold">Sistema</th><th className="px-5 py-3 text-right font-semibold">Contado</th><th className="px-5 py-3 text-right font-semibold">Diferencia</th></tr></thead><tbody className="divide-y divide-stone-100">{reports?.differences.data?.map((item) => <tr key={`${item.conteoId}-${item.productoId}`}><td className="px-5 py-4 font-medium text-ivera-text">{item.productoNombre}</td><td className="px-5 py-4 text-ivera-muted">{item.ubicacionNombre}</td><td className="px-5 py-4 text-right text-ivera-muted">{formatQuantity(item.cantidadSistema)}</td><td className="px-5 py-4 text-right text-ivera-muted">{formatQuantity(item.cantidadContada)}</td><td className="px-5 py-4 text-right"><Badge tone="warning">{formatQuantity(item.diferencia)}</Badge></td></tr>)}</tbody></table></div><div className="divide-y divide-stone-100 md:hidden">{reports?.differences.data?.map((item) => <div key={`${item.conteoId}-${item.productoId}`} className="px-5 py-4"><p className="font-medium text-ivera-text">{item.productoNombre}</p><p className="mt-1 text-xs text-ivera-muted">{item.ubicacionNombre} · {formatDate(item.fecha)}</p><p className="mt-3 text-sm text-ivera-muted">Sistema {formatQuantity(item.cantidadSistema)} · Contado {formatQuantity(item.cantidadContada)}</p><div className="mt-2"><Badge tone="warning">Diferencia {formatQuantity(item.diferencia)}</Badge></div></div>)}</div></>}</Card>
    <Card className="overflow-hidden p-0"><div className="p-5"><SectionTitle icon={ScanLine} title="Movimientos" description="Historial de movimientos devuelto por el reporte para el rango seleccionado." /></div>{loading ? <div className="px-5 pb-5"><ReportSkeleton /></div> : reports?.movements.error ? <div className="px-5 pb-5"><ErrorState title="No se pudieron cargar los movimientos" description={reports.movements.error} /></div> : reports?.movements.data?.length === 0 ? <div className="px-5 pb-5"><EmptyState title="Sin movimientos para mostrar" description="El reporte no devolvió movimientos para el rango actual." /></div> : <div className="hidden overflow-x-auto md:block"><table className="w-full text-left text-sm"><thead className="border-y border-stone-200 bg-stone-50 text-xs uppercase tracking-wide text-ivera-muted"><tr><th className="px-5 py-3 font-semibold">Fecha</th><th className="px-5 py-3 font-semibold">Producto</th><th className="px-5 py-3 font-semibold">Ruta</th><th className="px-5 py-3 text-right font-semibold">Estado</th></tr></thead><tbody className="divide-y divide-stone-100">{reports?.movements.data?.map((item) => <tr key={item.id}><td className="whitespace-nowrap px-5 py-4 text-ivera-muted">{formatDate(item.fecha)}</td><td className="px-5 py-4"><p className="font-medium text-ivera-text">{item.productoNombre}</p><p className="text-xs text-ivera-muted">{formatQuantity(item.cantidad)} {item.unidad.nombre} · {item.tipo}</p></td><td className="px-5 py-4 text-ivera-muted">{item.ubicacionOrigenNombre}{item.ubicacionDestinoNombre && ` → ${item.ubicacionDestinoNombre}`}</td><td className="px-5 py-4 text-right"><Badge tone={item.estado === 'APROBADO' ? 'success' : item.estado === 'PENDIENTE' ? 'warning' : 'danger'}>{item.estado}</Badge></td></tr>)}</tbody></table></div>}</Card>
    <Card className="border border-orange-100 bg-orange-50/55"><div className="flex gap-3"><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-white text-ivera-primary"><BarChart3 className="size-5" /></span><p className="text-sm leading-6 text-ivera-muted">Cada sección consulta su endpoint de forma independiente. Si un reporte falla, los demás resultados siguen disponibles y el mensaje mostrado corresponde a la respuesta del backend.</p></div></Card>
  </div>
}
