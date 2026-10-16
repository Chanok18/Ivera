import type { ReactNode } from 'react'

type BadgeProps = {
  children: ReactNode
  tone?: 'success' | 'warning' | 'danger' | 'neutral'
}

const tones = {
  success: 'bg-green-50 text-green-700',
  warning: 'bg-amber-50 text-amber-700',
  danger: 'bg-red-50 text-red-700',
  neutral: 'bg-stone-100 text-ivera-muted',
}

export function Badge({ children, tone = 'neutral' }: BadgeProps) {
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${tones[tone]}`}>{children}</span>
}
