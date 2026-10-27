import { useState } from 'react'
import { strings } from '../strings'

export function useNameInput(initial: string, onEdit: () => void = () => undefined) {
  const [value, setValue] = useState(initial)
  const [missing, setMissing] = useState(false)

  const change = (next: string) => {
    setValue(next)
    setMissing(false)
    onEdit()
  }

  const require = (): string | null => {
    const trimmed = value.trim()
    if (!trimmed) setMissing(true)
    return trimmed || null
  }

  return { value, change, require, missingError: missing ? strings.nameRequired : null }
}
