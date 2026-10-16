import type { ReactNode } from 'react'

type PageHeaderProps = {
  title: string
  description: string
  action?: ReactNode
}

export function PageHeader({ title, description, action }: PageHeaderProps) {
  return (
    <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="text-sm font-semibold text-ivera-primary">Ivera</p>
        <h1 className="mt-1 text-2xl font-bold tracking-tight text-ivera-text sm:text-3xl">{title}</h1>
        <p className="mt-2 text-sm text-ivera-muted">{description}</p>
      </div>
      {action}
    </header>
  )
}
