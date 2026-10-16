import type { ButtonHTMLAttributes, ReactNode } from 'react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  children: ReactNode
  variant?: 'primary' | 'secondary' | 'ghost'
}

const variants = {
  primary: 'bg-ivera-primary text-white hover:bg-ivera-primary-dark',
  secondary: 'border border-orange-200 bg-orange-50 text-ivera-primary-dark hover:bg-orange-100',
  ghost: 'text-ivera-muted hover:bg-stone-100 hover:text-ivera-text',
}

export function Button({ children, className = '', variant = 'primary', ...props }: ButtonProps) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
