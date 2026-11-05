import type { ReactNode } from 'react'

export function StateView({ icon, title, body, tone = 'status', children }: {
  icon: string
  title: string
  body?: string
  tone?: 'status' | 'alert'
  children?: ReactNode
}) {
  return (
    <section className="state" role={tone}>
      <span className="state-ring" aria-hidden="true"><span>{icon}</span></span>
      <h2 className="state-title">{title}</h2>
      {body && <p className="hint">{body}</p>}
      {children && <div className="state-actions">{children}</div>}
    </section>
  )
}
