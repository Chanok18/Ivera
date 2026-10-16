import { useState, type FormEvent } from 'react'
import { ArrowRight, LockKeyhole, Mail, PackageCheck } from 'lucide-react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/ui/Button'

export function LoginPage() {
  const { session, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (session) return <Navigate to="/" replace />

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await signIn({ email, password })
      const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/'
      navigate(from, { replace: true })
    } catch (cause) {
      if (typeof cause === 'object' && cause !== null && 'status' in cause && cause.status === 401) setError('Correo o contraseña incorrectos')
      else if (cause instanceof ApiError) setError(cause.message)
      else setError('Ocurrió un error inesperado al iniciar sesión.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="grid min-h-screen bg-ivera-canvas lg:grid-cols-[1.05fr_0.95fr]">
      <section className="hidden bg-ivera-sidebar p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="flex items-center gap-3"><span className="grid size-11 place-items-center rounded-2xl bg-ivera-primary text-xl font-bold">I</span><div><p className="text-xl font-bold">Ivera</p><p className="text-sm text-stone-400">Inventario centralizado</p></div></div>
        <div className="max-w-md"><PackageCheck className="size-10 text-ivera-primary" /><h1 className="mt-6 text-4xl font-bold leading-tight">Control claro para cada ubicación.</h1><p className="mt-4 text-stone-300">Romel, Todopernos y el Almacén Central en una sola plataforma.</p></div>
        <p className="text-sm text-stone-500">Acceso seguro con credenciales autorizadas.</p>
      </section>
      <section className="flex items-center justify-center p-5 sm:p-10">
        <div className="w-full max-w-md rounded-3xl bg-white p-7 shadow-[0_16px_40px_rgba(31,41,55,0.09)] sm:p-9">
          <div className="lg:hidden"><span className="grid size-10 place-items-center rounded-2xl bg-ivera-primary text-lg font-bold text-white">I</span><p className="mt-4 font-bold text-ivera-text">Ivera</p></div>
          <h2 className="mt-6 text-2xl font-bold tracking-tight text-ivera-text">Bienvenido</h2>
          <p className="mt-2 text-sm text-ivera-muted">Ingresa con tu cuenta autorizada.</p>
          <form className="mt-7 space-y-5" onSubmit={handleSubmit}>
            <label className="block text-sm font-medium text-ivera-text">Correo electrónico<div className="relative mt-2"><Mail className="pointer-events-none absolute left-3 top-3 size-4 text-ivera-muted" /><input className="w-full rounded-xl border border-stone-200 py-2.5 pr-3 pl-10 outline-none transition focus:border-ivera-primary" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></div></label>
            <label className="block text-sm font-medium text-ivera-text">Contraseña<div className="relative mt-2"><LockKeyhole className="pointer-events-none absolute left-3 top-3 size-4 text-ivera-muted" /><input className="w-full rounded-xl border border-stone-200 py-2.5 pr-3 pl-10 outline-none transition focus:border-ivera-primary" type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required /></div></label>
            {error && <p className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{error}</p>}
            <Button type="submit" className="w-full" disabled={submitting}>{submitting ? 'Verificando acceso...' : <>Ingresar <ArrowRight className="size-4" /></>}</Button>
          </form>
        </div>
      </section>
    </main>
  )
}
