import { createContext, useContext } from 'react'
import type { AppHint } from '../api/types'

export interface Hints {
  isSeen: (hint: AppHint) => boolean
  dismiss: (hint: AppHint) => void
}

const EVERY_HINT_SEEN: Hints = { isSeen: () => true, dismiss: () => undefined }

export const HintsContext = createContext<Hints>(EVERY_HINT_SEEN)

export const HintVisibilityContext = createContext(true)

export const useHints = (): Hints => useContext(HintsContext)
