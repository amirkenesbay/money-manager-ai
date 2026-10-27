import { useCallback, useEffect, useRef, useState } from 'react'

const NOTICE_MS = 2500

export function useSoonNotice(): { visible: boolean; show: () => void } {
  const [visible, setVisible] = useState(false)
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined)

  useEffect(() => () => clearTimeout(timer.current), [])

  const show = useCallback(() => {
    clearTimeout(timer.current)
    setVisible(true)
    timer.current = setTimeout(() => setVisible(false), NOTICE_MS)
  }, [])

  return { visible, show }
}
