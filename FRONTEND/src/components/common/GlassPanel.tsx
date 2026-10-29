import type { ComponentPropsWithoutRef, ReactNode } from 'react'

type GlassPanelProps = ComponentPropsWithoutRef<'section'> & { children: ReactNode }

export function GlassPanel({ className = '', children, ...props }: GlassPanelProps) {
  return <section className={`glass-panel ${className}`.trim()} {...props}>{children}</section>
}
