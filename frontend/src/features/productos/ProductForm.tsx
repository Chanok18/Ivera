import { Minus, Plus, X } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { ApiError } from '../../api/client'
import { type Producto, type ProductoRequest, type Unidad } from '../../api/productos'
import { Button } from '../../components/ui/Button'

type DraftPresentation = { unidadId: string; factorConversion: string; precioPresentacion: string }
type Draft = { nombre: string; descripcion: string; categoria: string; codigoBarras: string; unidadBaseId: string; imagenUrl: string; precioUnitario: string; presentaciones: DraftPresentation[] }
type ProductFormProps = { product?: Producto; units: Unidad[]; onClose: () => void; onSaved: (request: ProductoRequest) => Promise<void> }

function initialDraft(product?: Producto): Draft {
  return product ? {
    nombre: product.nombre,
    descripcion: product.descripcion ?? '',
    categoria: product.categoria ?? '',
    codigoBarras: product.codigoBarras ?? '',
    unidadBaseId: String(product.unidadBase.id),
    imagenUrl: product.imagenUrl ?? '',
    precioUnitario: String(product.precioUnitario),
    presentaciones: product.presentaciones.map((presentation) => ({ unidadId: String(presentation.unidad.id), factorConversion: String(presentation.factorConversion), precioPresentacion: presentation.precioPresentacion === null ? '' : String(presentation.precioPresentacion) })),
  } : { nombre: '', descripcion: '', categoria: '', codigoBarras: '', unidadBaseId: '', imagenUrl: '', precioUnitario: '', presentaciones: [] }
}

function optionalText(value: string) {
  return value.trim() || undefined
}

export function ProductForm({ product, units, onClose, onSaved }: ProductFormProps) {
  const [draft, setDraft] = useState(() => initialDraft(product))
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const editing = Boolean(product)

  function updateField(field: Exclude<keyof Draft, 'presentaciones'>, value: string) {
    setDraft((current) => ({ ...current, [field]: value }))
  }

  function updatePresentation(index: number, field: keyof DraftPresentation, value: string) {
    setDraft((current) => ({ ...current, presentaciones: current.presentaciones.map((presentation, currentIndex) => currentIndex === index ? { ...presentation, [field]: value } : presentation) }))
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving) return
    const unitId = Number(draft.unidadBaseId)
    const price = Number(draft.precioUnitario)
    if (!draft.nombre.trim()) return setError('El nombre es obligatorio.')
    if (!Number.isInteger(unitId) || unitId <= 0) return setError('Selecciona una unidad base válida.')
    if (!Number.isFinite(price) || price <= 0) return setError('El precio unitario debe ser mayor que 0.')

    const presentations = draft.presentaciones.map((presentation) => ({ unidadId: Number(presentation.unidadId), factorConversion: Number(presentation.factorConversion), precioPresentacion: presentation.precioPresentacion === '' ? undefined : Number(presentation.precioPresentacion) }))
    if (presentations.some((presentation) => !Number.isInteger(presentation.unidadId) || presentation.unidadId <= 0 || !Number.isInteger(presentation.factorConversion) || presentation.factorConversion <= 0 || (presentation.precioPresentacion !== undefined && !Number.isFinite(presentation.precioPresentacion)))) return setError('Cada presentación requiere una unidad y un factor de conversión positivo.')

    setError(null)
    setSaving(true)
    try {
      await onSaved({ nombre: draft.nombre.trim(), descripcion: optionalText(draft.descripcion), categoria: optionalText(draft.categoria), codigoBarras: optionalText(draft.codigoBarras), unidadBaseId: unitId, imagenUrl: optionalText(draft.imagenUrl), precioUnitario: price, presentaciones: presentations })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No se pudo guardar el producto.')
      setSaving(false)
    }
  }

  return <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-0 sm:items-center sm:p-6" role="presentation" onMouseDown={saving ? undefined : onClose}>
    <section aria-labelledby="producto-formulario" aria-modal="true" className="max-h-[92vh] w-full max-w-3xl overflow-y-auto rounded-t-2xl bg-ivera-card p-5 shadow-2xl sm:rounded-2xl sm:p-6" role="dialog" onMouseDown={(event) => event.stopPropagation()}>
      <div className="flex items-start justify-between gap-4"><div><p className="text-sm font-semibold text-ivera-primary">Administración de catálogo</p><h2 id="producto-formulario" className="mt-1 text-xl font-bold text-ivera-text">{editing ? 'Editar producto' : 'Nuevo producto'}</h2><p className="mt-1 text-sm text-ivera-muted">Los campos con asterisco son obligatorios.</p></div><Button aria-label="Cerrar formulario" className="shrink-0 px-3" disabled={saving} variant="ghost" onClick={onClose}><X className="size-5" /></Button></div>
      <form className="mt-6 space-y-6" onSubmit={submit}><fieldset disabled={saving} className="space-y-6"><div className="grid gap-4 sm:grid-cols-2"><label className="sm:col-span-2"><span className="text-sm font-medium text-ivera-text">Nombre *</span><input className="mt-1.5 w-full rounded-xl border border-stone-200 px-3 py-2.5 text-sm" maxLength={255} onChange={(event) => updateField('nombre', event.target.value)} required value={draft.nombre} /></label><label><span className="text-sm font-medium text-ivera-text">Unidad base *</span><select className="mt-1.5 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm" onChange={(event) => updateField('unidadBaseId', event.target.value)} required value={draft.unidadBaseId}><option value="">Selecciona una unidad</option>{units.map((unit) => <option key={unit.id} value={unit.id}>{unit.nombre}</option>)}</select></label><label><span className="text-sm font-medium text-ivera-text">Precio unitario *</span><input className="mt-1.5 w-full rounded-xl border border-stone-200 px-3 py-2.5 text-sm" min="0.01" onChange={(event) => updateField('precioUnitario', event.target.value)} required step="0.01" type="number" value={draft.precioUnitario} /></label><label><span className="text-sm font-medium text-ivera-text">Categoría</span><input className="mt-1.5 w-full rounded-xl border border-stone-200 px-3 py-2.5 text-sm" maxLength={255} onChange={(event) => updateField('categoria', event.target.value)} value={draft.categoria} /></label><label><span className="text-sm font-medium text-ivera-text">Código de barras</span><input className="mt-1.5 w-full rounded-xl border border-stone-200 px-3 py-2.5 text-sm" maxLength={255} onChange={(event) => updateField('codigoBarras', event.target.value)} value={draft.codigoBarras} /></label><label className="sm:col-span-2"><span className="text-sm font-medium text-ivera-text">URL de imagen</span><input className="mt-1.5 w-full rounded-xl border border-stone-200 px-3 py-2.5 text-sm" onChange={(event) => updateField('imagenUrl', event.target.value)} type="url" value={draft.imagenUrl} /></label><label className="sm:col-span-2"><span className="text-sm font-medium text-ivera-text">Descripción</span><textarea className="mt-1.5 min-h-24 w-full rounded-xl border border-stone-200 px-3 py-2.5 text-sm" onChange={(event) => updateField('descripcion', event.target.value)} value={draft.descripcion} /></label></div><div className="border-t border-stone-100 pt-5"><div className="flex items-center justify-between gap-4"><div><h3 className="font-semibold text-ivera-text">Presentaciones</h3><p className="mt-1 text-sm text-ivera-muted">Opcionales. El factor se expresa en unidades base.</p></div><Button className="px-3" type="button" variant="secondary" onClick={() => setDraft((current) => ({ ...current, presentaciones: [...current.presentaciones, { unidadId: '', factorConversion: '', precioPresentacion: '' }] }))}><Plus className="size-4" />Agregar</Button></div>{draft.presentaciones.length > 0 && <div className="mt-4 space-y-3">{draft.presentaciones.map((presentation, index) => <div key={index} className="grid gap-3 rounded-xl bg-stone-50 p-3 sm:grid-cols-[minmax(0,1fr)_9rem_9rem_auto]"><select aria-label={`Unidad de presentación ${index + 1}`} className="rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm" onChange={(event) => updatePresentation(index, 'unidadId', event.target.value)} value={presentation.unidadId}><option value="">Unidad</option>{units.map((unit) => <option key={unit.id} value={unit.id}>{unit.nombre}</option>)}</select><input aria-label={`Factor de conversión ${index + 1}`} className="rounded-xl border border-stone-200 px-3 py-2.5 text-sm" min="1" onChange={(event) => updatePresentation(index, 'factorConversion', event.target.value)} placeholder="Factor" type="number" value={presentation.factorConversion} /><input aria-label={`Precio de presentación ${index + 1}`} className="rounded-xl border border-stone-200 px-3 py-2.5 text-sm" onChange={(event) => updatePresentation(index, 'precioPresentacion', event.target.value)} placeholder="Precio" step="0.01" type="number" value={presentation.precioPresentacion} /><Button aria-label={`Quitar presentación ${index + 1}`} className="px-3" type="button" variant="ghost" onClick={() => setDraft((current) => ({ ...current, presentaciones: current.presentaciones.filter((_, currentIndex) => currentIndex !== index) }))}><Minus className="size-4" /></Button></div>)}</div>}</div></fieldset>{error && <p aria-live="polite" className="rounded-xl bg-red-50 px-3 py-2.5 text-sm text-red-700">{error}</p>}<div className="flex justify-end gap-3 border-t border-stone-100 pt-5"><Button disabled={saving} type="button" variant="secondary" onClick={onClose}>Cancelar</Button><Button disabled={saving || units.length === 0} type="submit">{saving ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear producto'}</Button></div></form>
    </section>
  </div>
}
