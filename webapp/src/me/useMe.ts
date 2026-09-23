import { useCallback, useEffect, useState } from 'react'
import { ApiRequestError } from '../api/client'
import type { Me } from '../api/types'
import { fetchMe } from './meApi'

export type MeState =
  | { status: 'loading' }
  | { status: 'error'; message: string | null }
  | { status: 'ready'; me: Me }

export function useMe(): { state: MeState; reload: () => void } {
  const [state, setState] = useState<MeState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchMe(controller.signal)
      .then((me) => setState({ status: 'ready', me }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) return
        setState({ status: 'error', message: error instanceof ApiRequestError ? (error.body?.message ?? null) : null })
      })
    return () => controller.abort()
  }, [attempt])

  const reload = useCallback(() => {
    setState({ status: 'loading' })
    setAttempt((value) => value + 1)
  }, [])
  return { state, reload }
}
