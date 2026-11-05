import { selectionTick } from '../telegram/haptics'
import type { CSSProperties } from 'react'

export function Segmented<T>({ label, options, value, optionKey, optionLabel, onChange, variant = 'segmented' }: {
  label: string
  variant?: 'segmented' | 'choices'
  options: T[]
  value: T
  optionKey: (option: T) => string
  optionLabel: (option: T) => string
  onChange: (option: T) => void
}) {
  const selected = options.indexOf(value)
  const sliding = variant === 'segmented' && selected >= 0
  const thumbPosition = { '--selected': selected, '--count': options.length } as CSSProperties
  return (
    <div className={variant} role="radiogroup" aria-label={label} style={sliding ? thumbPosition : undefined}>
      {sliding && <span className="segmented-thumb" aria-hidden="true" />}
      {options.map((option) => (
        <button key={optionKey(option)} type="button" role="radio" aria-checked={option === value} onClick={() => { selectionTick(); onChange(option) }}>
          {optionLabel(option)}
        </button>
      ))}
    </div>
  )
}
