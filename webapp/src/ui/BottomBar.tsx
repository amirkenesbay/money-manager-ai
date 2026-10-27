import type { ReactNode } from 'react'
import { useKeyboardOffset } from './useKeyboardOffset'

export function BottomBar({ children }: { children: ReactNode }) {
  const keyboardOffset = useKeyboardOffset()
  return <div className="bottom-bar" style={{ bottom: keyboardOffset }}>{children}</div>
}
