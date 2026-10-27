import { useCallback, useState } from 'react'
import { serverErrorCode, serverMessage } from '../api/client'
import type { ApiErrorCode } from '../api/types'
import { notifyOutcome } from '../telegram/haptics'

export interface SubmitError {
  message: string
  code: ApiErrorCode | null
}

export type SubmitResult<T> = { ok: true; value: T } | { ok: false; error: SubmitError }

export function useSubmit() {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<SubmitError | null>(null)

  const run = useCallback(async <T,>(action: () => Promise<T>, fallback: string): Promise<SubmitResult<T>> => {
    setPending(true)
    setError(null)
    try {
      const value = await action()
      notifyOutcome('success')
      setPending(false)
      return { ok: true, value }
    } catch (failure) {
      notifyOutcome('error')
      const submitError = { message: serverMessage(failure) ?? fallback, code: serverErrorCode(failure) }
      setError(submitError)
      setPending(false)
      return { ok: false, error: submitError }
    }
  }, [])

  const clearError = useCallback(() => setError(null), [])

  return { pending, error, run, clearError }
}
