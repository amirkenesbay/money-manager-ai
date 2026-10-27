import type { ReactNode } from 'react'
import { ChevronRightIcon } from './icons'

export function SettingsRow({ icon, label, value, danger = false, onClick }: {
  icon: ReactNode
  label: string
  value?: string
  danger?: boolean
  onClick: () => void
}) {
  return (
    <button type="button" className={`settings-row ${danger ? 'negative' : ''}`} onClick={onClick}>
      <span className="settings-row-icon" aria-hidden="true">{icon}</span>
      <span className="row-title settings-row-label">{label}</span>
      {value && <span className="caption settings-row-value">{value}</span>}
      {!danger && <ChevronRightIcon />}
    </button>
  )
}
