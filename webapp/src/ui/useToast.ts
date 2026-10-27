import { useCallback, useRef, useState } from 'react'
import type { ToastMessage } from './Toast'

export function useToast() {
  const [current, setCurrent] = useState<ToastMessage | null>(null)
  const nextId = useRef(0)

  const show = useCallback((message: Omit<ToastMessage, 'id'>) => {
    nextId.current += 1
    setCurrent({ ...message, id: nextId.current })
  }, [])
  const hide = useCallback(() => setCurrent(null), [])

  return { current, show, hide }
}
