import { useCallback, useEffect, useMemo, useState } from 'react'
import { serverMessage } from './client'

export type AsyncState<T> =
  | { status: 'loading' }
  | { status: 'error'; message: string | null }
  | { status: 'ready'; data: T }

type SettledState<T> = Exclude<AsyncState<T>, { status: 'loading' }>

export function useAsync<T>(load: (signal: AbortSignal) => Promise<T>): { state: AsyncState<T>; reload: () => void } {
  const [attempt, setAttempt] = useState(0)
  const request = useMemo(() => ({ load, attempt }), [load, attempt])
  const [settled, setSettled] = useState<{ request: typeof request; state: SettledState<T> } | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    request.load(controller.signal)
      .then((data) => setSettled({ request, state: { status: 'ready', data } }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        setSettled({ request, state: { status: 'error', message: serverMessage(error) } })
      })
    return () => controller.abort()
  }, [request])

  const reload = useCallback(() => setAttempt((value) => value + 1), [])
  const state: AsyncState<T> = settled?.request === request ? settled.state : { status: 'loading' }
  return { state, reload }
}
