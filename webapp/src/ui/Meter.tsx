import { FULL_PERCENT } from '../percent'

export function Meter({ percent, color = 'var(--accent)' }: { percent: number; color?: string }) {
  return (
    <span className="meter" aria-hidden="true">
      <span style={{ width: `${Math.min(percent, FULL_PERCENT)}%`, background: color }} />
    </span>
  )
}
