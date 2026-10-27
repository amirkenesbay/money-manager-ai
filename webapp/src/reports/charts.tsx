import type { ReactNode } from 'react'
import { chartColor, FULL_PERCENT, percentOf } from './chartMath'

const RADIUS = 64
const STROKE = 20
const VIEW_BOX = 168
const CENTER = VIEW_BOX / 2
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
const SLICE_GAP = 2

export function Donut({ values, label, children }: { values: number[]; label: string; children?: ReactNode }) {
  const total = values.reduce((sum, value) => sum + value, 0) || 1
  const offsets = values.map((_, index) => values.slice(0, index).reduce((sum, value) => sum + (value / total) * CIRCUMFERENCE, 0))
  return (
    <div className="donut">
      <svg viewBox={`0 0 ${VIEW_BOX} ${VIEW_BOX}`} role="img" aria-label={label}>
        <circle cx={CENTER} cy={CENTER} r={RADIUS} fill="none" stroke="var(--line)" strokeWidth={STROKE} />
        {values.map((value, index) => (
          <circle
            key={index}
            cx={CENTER}
            cy={CENTER}
            r={RADIUS}
            fill="none"
            stroke={chartColor(index)}
            strokeWidth={STROKE}
            strokeDasharray={`${Math.max((value / total) * CIRCUMFERENCE - SLICE_GAP, 0)} ${CIRCUMFERENCE}`}
            strokeDashoffset={-offsets[index]}
          />
        ))}
      </svg>
      <div className="donut-center">{children}</div>
    </div>
  )
}

export function Meter({ percent, color = 'var(--accent)' }: { percent: number; color?: string }) {
  return (
    <span className="meter" aria-hidden="true">
      <span style={{ width: `${Math.min(percent, FULL_PERCENT)}%`, background: color }} />
    </span>
  )
}

export function MonthBars({ label, bars }: { label: string; bars: { key: string; name: string; value: number; caption: string }[] }) {
  const max = Math.max(...bars.map((bar) => bar.value), 0)
  return (
    <div className="month-bars well" role="img" aria-label={label}>
      {bars.map((bar, index) => {
        const last = index === bars.length - 1
        return (
          <span key={bar.key} className={`month-bar ${last ? 'month-bar-current' : ''}`}>
            <span className="amount">{bar.caption}</span>
            <span className="month-bar-fill" style={{ height: `${percentOf(bar.value, max)}%` }} />
            <span className="caption">{bar.name}</span>
          </span>
        )
      })}
    </div>
  )
}
