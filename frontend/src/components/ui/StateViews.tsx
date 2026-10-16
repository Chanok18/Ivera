import { AlertCircle, Inbox, LoaderCircle } from 'lucide-react'
import type { ReactNode } from 'react'
import { Card } from './Card'

type StateProps = { title: string; description: string }

function StateContainer({ children, title, description }: StateProps & { children: ReactNode }) {
  return (
    <Card className="flex min-h-52 flex-col items-center justify-center text-center">
      {children}
      <h2 className="mt-4 text-base font-semibold text-ivera-text">{title}</h2>
      <p className="mt-1 max-w-sm text-sm text-ivera-muted">{description}</p>
    </Card>
  )
}

export function EmptyState({ title, description }: StateProps) {
  return <StateContainer title={title} description={description}><Inbox className="size-8 text-ivera-primary" /></StateContainer>
}

export function LoadingState({ title = 'Cargando', description = 'Estamos preparando la información.' }: Partial<StateProps>) {
  return <StateContainer title={title} description={description}><LoaderCircle className="size-8 animate-spin text-ivera-primary" /></StateContainer>
}

export function ErrorState({ title = 'No se pudo cargar la información', description = 'Intenta nuevamente cuando el servicio esté disponible.' }: Partial<StateProps>) {
  return <StateContainer title={title} description={description}><AlertCircle className="size-8 text-ivera-danger" /></StateContainer>
}
