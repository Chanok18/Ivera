import type { HTMLAttributes, ReactNode } from 'react'

type CardProps = HTMLAttributes<HTMLDivElement> & { children: ReactNode }

export function Card({ children, className = '', ...props }: CardProps) {
  return (
    <section className={`rounded-2xl bg-ivera-card p-5 shadow-[0_1px_3px_rgba(0,0,0,0.08)] ${className}`} {...props}>
      {children}
    </section>
  )
}
