import { RefreshCw, Search, ShieldAlert } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { ApiError } from '../../api/client'
import { listarMovimientos, type Movimiento } from '../../api/movimientos'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { EmptyState, ErrorState } from '../../components/ui/StateViews'
import { PageHeader } from '../../components/ui/PageHeader'

function formatQuantity(value: number) {
  return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 }).format(value)
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function MovementSkeleton() {
  return <Card className="overflow-hidden p-0"><div className="hidden animate-pulse divide-y divide-stone-100 md:block">{Array.from({ length: 6 }, (_, index) => <div key={index} className="grid grid-cols-[10rem_1.5fr_7rem_1fr_7rem] gap-5 px-5 py-5"><span className="h-5 rounded bg-stone-100" /><span className="h-5 rounded bg-stone-100" /><span className="h-5 rounded bg-stone-100" /><span className="h-5 rounded bg-stone-100" /><span className="h-5 rounded bg-stone-100" /></div>)}</div><div className="space-y-3 p-5 md:hidden">{Array.from({ length: 4 }, (_, index) => <div key={index} className="h-28 animate-pulse rounded-xl bg-stone-100" />)}</div></Card>
}

function stateTone(state: Movimiento['estado']) {
  return state === 'APROBADO' ? 'success' : state === 'PENDIENTE' ? 'warning' : 'danger'
}

export function MovementsPage() {
  const [movements, setMovements] = useState<Movimiento[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [dateError, setDateError] = useState<string | null>(null)
  const loadedInitially = useRef(false)

  async function refresh() {
    setError(null)
    setDateError(null)
    if (desde && hasta && desde > hasta) {
      setDateError('La fecha inicial no puede ser posterior a la fecha final.')
      return
    }
    try {
      setMovements(await listarMovimientos(desde || undefined, hasta || undefined))
    } catch (reason) {
      setMovements(null)
      setError(reason instanceof ApiError ? reason.message : 'No se pudo cargar el historial de movimientos.')
    }
  }

  useEffect(() => {
    if (loadedInitially.current) return
    loadedInitially.current = true
    void listarMovimientos()
      .then(setMovements)
      .catch((reason) => {
        setMovements(null)
        setError(reason instanceof ApiError ? reason.message : 'No se pudo cargar el historial de movimientos.')
      })
  }, [])

  const normalizedQuery = query.trim().toLocaleLowerCase('es-PE')
  const visibleMovements = (movements ?? []).filter((movement) => !normalizedQuery || [movement.productoNombre, movement.tipo, movement.ubicacionOrigenNombre, movement.ubicacionDestinoNombre, String(movement.id)].some((value) => value?.toLocaleLowerCase('es-PE').includes(normalizedQuery)))

  return <div className="space-y-6">
    <PageHeader title="Movimientos" description="Revisa el historial disponible y los estados de aprobación del inventario." action={<Button variant="secondary" onClick={() => void refresh()}><RefreshCw className="size-4" />Actualizar</Button>} />
    <Card className="p-4 sm:p-5"><div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_10rem_10rem_auto]"><label className="relative block"><span className="sr-only">Buscar en movimientos cargados</span><Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ivera-muted" aria-hidden="true" /><input className="w-full rounded-xl border border-stone-200 bg-white py-2.5 pl-10 pr-3 text-sm text-ivera-text placeholder:text-ivera-muted focus:border-ivera-primary" onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por producto, tipo, ubicación o ID" type="search" value={query} /></label><label><span className="sr-only">Fecha inicial</span><input className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm text-ivera-text" onChange={(event) => setDesde(event.target.value)} type="date" value={desde} /></label><label><span className="sr-only">Fecha final</span><input className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm text-ivera-text" onChange={(event) => setHasta(event.target.value)} type="date" value={hasta} /></label><Button variant="secondary" onClick={() => void refresh()}>Aplicar</Button></div>{dateError && <p aria-live="polite" className="mt-3 text-sm text-red-700">{dateError}</p>}{movements && <p aria-live="polite" className="mt-3 text-sm text-ivera-muted">{visibleMovements.length} {visibleMovements.length === 1 ? 'movimiento encontrado' : 'movimientos encontrados'}</p>}</Card>
    {movements === null && !error ? <MovementSkeleton /> : error ? <div className="space-y-3"><ErrorState title="No se pudo cargar el historial" description={error} /><div className="text-center"><Button variant="secondary" onClick={() => void refresh()}><RefreshCw className="size-4" />Reintentar</Button></div></div> : visibleMovements.length === 0 ? <EmptyState title="Sin movimientos para mostrar" description={query ? 'No hay coincidencias en el historial cargado.' : 'El reporte no devolvió movimientos para el rango seleccionado.'} /> : <Card className="overflow-hidden p-0"><div className="hidden overflow-x-auto md:block"><table className="w-full text-left text-sm"><thead className="border-b border-stone-200 bg-stone-50 text-xs uppercase tracking-wide text-ivera-muted"><tr><th className="px-5 py-3 font-semibold">Fecha</th><th className="px-5 py-3 font-semibold">Producto</th><th className="px-5 py-3 font-semibold">Tipo</th><th className="px-5 py-3 font-semibold">Origen</th><th className="px-5 py-3 text-right font-semibold">Estado</th></tr></thead><tbody className="divide-y divide-stone-100">{visibleMovements.map((movement) => <tr key={movement.id}><td className="whitespace-nowrap px-5 py-4 text-ivera-muted">{formatDate(movement.fecha)}</td><td className="px-5 py-4"><p className="font-semibold text-ivera-text">{movement.productoNombre}</p><p className="mt-1 text-xs text-ivera-muted">{formatQuantity(movement.cantidad)} {movement.unidad.nombre}</p></td><td className="px-5 py-4 text-ivera-muted">{movement.tipo}</td><td className="px-5 py-4 text-ivera-muted">{movement.ubicacionOrigenNombre}{movement.ubicacionDestinoNombre && ` → ${movement.ubicacionDestinoNombre}`}</td><td className="px-5 py-4 text-right"><Badge tone={stateTone(movement.estado)}>{movement.estado}</Badge></td></tr>)}</tbody></table></div><div className="divide-y divide-stone-100 md:hidden">{visibleMovements.map((movement) => <div key={movement.id} className="px-5 py-4"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="truncate font-semibold text-ivera-text">{movement.productoNombre}</p><p className="mt-1 text-xs text-ivera-muted">{formatDate(movement.fecha)} · {movement.tipo}</p></div><Badge tone={stateTone(movement.estado)}>{movement.estado}</Badge></div><p className="mt-3 text-sm text-ivera-muted">{formatQuantity(movement.cantidad)} {movement.unidad.nombre} · {movement.ubicacionOrigenNombre}{movement.ubicacionDestinoNombre && ` → ${movement.ubicacionDestinoNombre}`}</p></div>)}</div></Card>}
    <Card className="border border-orange-100 bg-orange-50/55"><div className="flex gap-3"><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-white text-ivera-primary"><ShieldAlert className="size-5" /></span><div><h2 className="font-semibold text-ivera-text">Traslados no habilitados en esta pantalla</h2><p className="mt-1 text-sm leading-6 text-ivera-muted">El backend registra traslados mediante <code>POST /api/movimientos</code>, pero no expone una consulta de ubicaciones autorizadas para la sesión actual. Sin ese contrato, la interfaz no puede ofrecer orígenes y destinos seguros para un trabajador sin inventar permisos.</p></div></div></Card>
  </div>
}
