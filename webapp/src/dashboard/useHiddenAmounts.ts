import { useState } from 'react'
import { clearFlag, isFlagStored, storeFlag } from '../ui/storedFlag'

const HIDDEN_AMOUNTS_KEY = 'mm-amounts-hidden'

export function useHiddenAmounts() {
  const [hidden, setHidden] = useState(() => isFlagStored(HIDDEN_AMOUNTS_KEY))
  const toggle = () => {
    if (hidden) clearFlag(HIDDEN_AMOUNTS_KEY)
    else storeFlag(HIDDEN_AMOUNTS_KEY)
    setHidden(!hidden)
  }
  return { hidden, toggle }
}
