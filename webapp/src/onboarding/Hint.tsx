import { type ReactNode, useCallback, useMemo, useState } from 'react'
import type { AppHint, Me } from '../api/types'
import { markHintSeen } from '../me/meApi'
import { type Hints, HintsContext, HintVisibilityContext } from './hintsContext'

export function HintsProvider({ me, onMeChanged, children }: { me: Me; onMeChanged: (me: Me) => void; children: ReactNode }) {
  const [dismissed, setDismissed] = useState<AppHint[]>([])

  const dismiss = useCallback((hint: AppHint) => {
    setDismissed((list) => (list.includes(hint) ? list : [...list, hint]))
    markHintSeen(hint).then(onMeChanged).catch(() => undefined)
  }, [onMeChanged])

  const hints = useMemo<Hints>(() => ({
    isSeen: (hint) => me.seenHints.includes(hint) || dismissed.includes(hint),
    dismiss,
  }), [me.seenHints, dismissed, dismiss])

  return <HintsContext.Provider value={hints}>{children}</HintsContext.Provider>
}

export function HintScope({ visible, children }: { visible: boolean; children: ReactNode }) {
  return <HintVisibilityContext.Provider value={visible}>{children}</HintVisibilityContext.Provider>
}
