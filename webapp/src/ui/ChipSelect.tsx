import type { ReactNode } from 'react'
import { ChevronDownIcon } from './icons'

export function ChipSelect({ label, value, options, icon, onChange }: {
  label: string
  value: string
  options: { value: string; label: string }[]
  icon?: ReactNode
  onChange: (value: string) => void
}) {
  const selected = options.find((option) => option.value === value)
  return (
    <span className="chip pressable">
      {icon}
      <span aria-hidden="true">{selected?.label}</span>
      <ChevronDownIcon />
      <select aria-label={label} value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
    </span>
  )
}
