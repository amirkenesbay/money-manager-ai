import { backButton } from '@tma.js/sdk-react'
import { useEffect } from 'react'

export function useBackButton(onBack: () => void): boolean {
  const available = backButton.show.isAvailable()

  useEffect(() => {
    if (!backButton.show.isAvailable()) return undefined
    backButton.show()
    const unsubscribe = backButton.onClick(onBack)
    return () => {
      unsubscribe()
      backButton.hide()
    }
  }, [onBack])

  return available
}
