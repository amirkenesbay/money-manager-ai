import type { ReactNode } from 'react'

export function EmptyCard({ icon, title, body }: { icon: ReactNode; title: string; body: string }) {
  return (
    <section className="empty-card">
      <span className="empty-card-icon" aria-hidden="true">{icon}</span>
      <h2 className="section-title">{title}</h2>
      <p className="caption">{body}</p>
    </section>
  )
}
