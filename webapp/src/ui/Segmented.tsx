export function Segmented<T>({ label, options, value, optionKey, optionLabel, onChange, variant = 'segmented' }: {
  label: string
  variant?: 'segmented' | 'choices'
  options: T[]
  value: T
  optionKey: (option: T) => string
  optionLabel: (option: T) => string
  onChange: (option: T) => void
}) {
  return (
    <div className={variant} role="radiogroup" aria-label={label}>
      {options.map((option) => (
        <button key={optionKey(option)} type="button" role="radio" aria-checked={option === value} onClick={() => onChange(option)}>
          {optionLabel(option)}
        </button>
      ))}
    </div>
  )
}
