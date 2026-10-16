import { EmptyState } from '../components/ui/StateViews'
import { PageHeader } from '../components/ui/PageHeader'

type PlaceholderPageProps = { title: string; description: string }

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return <div className="space-y-6"><PageHeader title={title} description={description} /><EmptyState title="Módulo preparado" description="Esta ruta forma parte de la base visual. La funcionalidad llegará en las tareas posteriores del Sprint 5." /></div>
}
