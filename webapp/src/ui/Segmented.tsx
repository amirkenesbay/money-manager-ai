export function Segmented<T>({ label, options, value, optionKey, optionLabel, onChange }: {
  label: string
  options: T[]
  value: T
  optionKey: (option: T) => string
  optionLabel: (option: T) => string
  onChange: (option: T) => void
}) {
  return (
    <div className="segmented" role="radiogroup" aria-label={label}>
      {options.map((option) => (
        <button key={optionKey(option)} type="button" role="radio" aria-checked={option === value} onClick={() => onChange(option)}>
          {optionLabel(option)}
        </button>
      ))}
    </div>
  )
}
