import { useState } from 'react'
import { Box, ClipboardList, FileBarChart, Handshake, House, LogOut, Menu, Package, PackageSearch, ScanLine, Settings2, ShoppingBasket, Truck, X } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'

const navigation = [
  ['/', 'Inicio', House, false],
  ['/productos', 'Productos', Package, false],
  ['/inventario', 'Inventario', Box, false],
  ['/movimientos', 'Movimientos', ScanLine, false],
  ['/conteos', 'Conteos', ClipboardList, false],
  ['/ventas', 'Ventas', ShoppingBasket, false],
  ['/compras', 'Compras', Truck, false],
  ['/proveedores', 'Proveedores', Handshake, false],
  ['/reportes', 'Reportes', FileBarChart, false],
  ['/autorizaciones', 'Autorizaciones', Settings2, true],
] as const

export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const { session, signOut } = useAuth()
  const closeMenu = () => setMenuOpen(false)

  return (
    <div className="min-h-screen bg-ivera-canvas lg:grid lg:grid-cols-[248px_1fr]">
      {menuOpen && <button aria-label="Cerrar menú" className="fixed inset-0 z-30 bg-black/40 lg:hidden" onClick={closeMenu} />}
      <aside className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-ivera-sidebar px-4 py-5 text-white transition-transform lg:static lg:translate-x-0 ${menuOpen ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="flex items-center justify-between px-2">
            <div className="flex items-center gap-3">
              <span className="grid size-9 place-items-center rounded-xl bg-ivera-primary text-lg font-bold">I</span>
              <div><p className="font-bold tracking-tight">Ivera</p><p className="text-xs text-stone-400">Inventario centralizado</p></div>
          </div>
          <button className="rounded-lg p-2 text-stone-300 hover:bg-white/10 lg:hidden" onClick={closeMenu}><X className="size-5" /></button>
        </div>
        <nav className="mt-9 space-y-1" aria-label="Navegación principal">
          {navigation.filter(([, , , administratorOnly]) => !administratorOnly || session?.rol === 'ADMINISTRADOR').map(([path, label, Icon]) => (
            <NavLink key={path} to={path} end={path === '/'} onClick={closeMenu} className={({ isActive }) => `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors ${isActive ? 'bg-ivera-primary text-white shadow-sm' : 'text-stone-300 hover:bg-white/10 hover:text-white'}`}>
              <Icon className="size-4" aria-hidden="true" />{label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto rounded-xl border border-white/10 bg-white/5 p-3 text-xs leading-5 text-stone-300"><PackageSearch className="mb-2 size-4 text-orange-300" aria-hidden="true" />La información visible respeta los permisos de tu sesión.</div>
      </aside>
      <div className="min-w-0">
        <header className="sticky top-0 z-20 flex min-h-18 items-center gap-3 border-b border-stone-200 bg-ivera-canvas/95 px-4 py-3 backdrop-blur sm:px-6 lg:px-8">
          <button className="rounded-xl p-2 text-ivera-text hover:bg-stone-100 lg:hidden" aria-label="Abrir menú" onClick={() => setMenuOpen(true)}><Menu className="size-5" /></button>
          <div className="hidden flex-1 md:block"><p className="text-sm font-semibold text-ivera-text">Operación de inventario</p><p className="text-xs text-ivera-muted">Panel de control</p></div>
          <div className="ml-auto flex items-center gap-2 sm:gap-3">
            <div className="hidden items-center gap-2 rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm text-ivera-text sm:flex"><span className="size-2 rounded-full bg-ivera-primary" /><span className="font-medium">Datos según permisos</span></div>
            <div className="flex items-center gap-2 border-l border-stone-200 pl-3"><span className="grid size-9 place-items-center rounded-full bg-orange-100 text-sm font-bold text-ivera-primary">{session?.email.slice(0, 1).toUpperCase()}</span><div className="hidden sm:block"><p className="max-w-32 truncate text-sm font-semibold text-ivera-text">{session?.email}</p><p className="text-xs text-ivera-muted">{session?.rol}</p></div><button className="rounded-lg p-2 text-ivera-muted hover:bg-stone-100 hover:text-ivera-text" aria-label="Cerrar sesión" onClick={signOut}><LogOut className="size-4" /></button></div>
          </div>
        </header>
        <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8"><Outlet /></main>
      </div>
    </div>
  )
}
