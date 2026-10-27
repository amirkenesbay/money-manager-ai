import { useId } from 'react'

export function NameField({ label, value, placeholder, maxLength, error, onChange }: {
  label: string
  value: string
  placeholder: string
  maxLength: number
  error: string | null
  onChange: (value: string) => void
}) {
  const id = useId()
  return (
    <div className="name-field">
      <label className="field-label" htmlFor={id}>{label}</label>
      <input
        id={id}
        className={`text-field name-input ${error ? 'field-invalid' : ''}`}
        value={value}
        placeholder={placeholder}
        maxLength={maxLength}
        autoComplete="off"
        aria-invalid={error !== null}
        onChange={(event) => onChange(event.target.value)}
      />
      {error && <p className="field-message negative" role="alert">{error}</p>}
    </div>
  )
}
