import { addToHomeScreen, checkHomeScreenStatus, isTMA, on } from '@tma.js/sdk-react'
import { useEffect, useState } from 'react'

export type HomeScreenState = 'unsupported' | 'available' | 'added'

const ADDED_STATUS = 'added'
const UNSUPPORTED_STATUS = 'unsupported'

async function readHomeScreenState(): Promise<HomeScreenState> {
  if (!isTMA()) return 'unsupported'
  const check = checkHomeScreenStatus.ifAvailable()
  if (!check.ok) return 'unsupported'
  try {
    const status = await check.data
    if (status === ADDED_STATUS) return 'added'
    return status === UNSUPPORTED_STATUS ? 'unsupported' : 'available'
  } catch {
    return 'unsupported'
  }
}

export function useHomeScreen(): { state: HomeScreenState; add: () => void } {
  const [state, setState] = useState<HomeScreenState>('unsupported')

  useEffect(() => {
    let active = true
    void readHomeScreenState().then((next) => {
      if (active) setState(next)
    })
    const stopListening = isTMA() ? on('home_screen_added', () => setState('added')) : undefined
    return () => {
      active = false
      stopListening?.()
    }
  }, [])

  return { state, add: () => void addToHomeScreen.ifAvailable() }
}
