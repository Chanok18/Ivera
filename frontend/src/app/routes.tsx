import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireSession } from '../auth/RequireSession'
import { AppLayout } from '../components/layout/AppLayout'
import { LoginPage } from '../features/auth/LoginPage'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { InventoryPage } from '../features/inventario/InventoryPage'
import { MovementsPage } from '../features/movimientos/MovementsPage'
import { ProductsPage } from '../features/productos/ProductsPage'
import { ReportsPage } from '../features/reportes/ReportsPage'
import { PlaceholderPage } from '../pages/PlaceholderPage'

const futurePages = [
  ['conteos', 'Conteos', 'Conteos físicos de inventario'],
  ['ventas', 'Ventas', 'Registro interno de ventas'],
  ['compras', 'Compras', 'Registro de compras a proveedores'],
  ['proveedores', 'Proveedores', 'Directorio de proveedores'],
  ['autorizaciones', 'Autorizaciones', 'Permisos de almacén para trabajadores'],
] as const

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireSession />}>
        <Route element={<AppLayout />}>
           <Route index element={<DashboardPage />} />
           <Route path="productos" element={<ProductsPage />} />
           <Route path="inventario" element={<InventoryPage />} />
            <Route path="movimientos" element={<MovementsPage />} />
            <Route path="reportes" element={<ReportsPage />} />
          {futurePages.map(([path, title, description]) => (
            <Route key={path} path={path} element={<PlaceholderPage title={title} description={description} />} />
          ))}
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
