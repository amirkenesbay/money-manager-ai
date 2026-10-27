import { useEffect, useState } from 'react'

export function useKeyboardOffset(): number {
  const [offset, setOffset] = useState(0)

  useEffect(() => {
    const visualViewport = window.visualViewport
    if (!visualViewport) return undefined
    const update = () => setOffset(Math.max(0, window.innerHeight - visualViewport.height - visualViewport.offsetTop))
    visualViewport.addEventListener('resize', update)
    visualViewport.addEventListener('scroll', update)
    return () => {
      visualViewport.removeEventListener('resize', update)
      visualViewport.removeEventListener('scroll', update)
    }
  }, [])

  return offset
}
