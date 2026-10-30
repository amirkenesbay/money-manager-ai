import { type ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { AppHint, Me } from '../api/types'
import { markHintSeen } from '../me/meApi'
import { strings } from '../strings'
import { type Hints, HintsContext, HintVisibilityContext, useHints } from './hintsContext'
import './onboarding.css'

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

export function Hint({ hint, title, body }: { hint: AppHint; title: string; body: string }) {
  const { isSeen, dismiss } = useHints()
  const visible = useContext(HintVisibilityContext)
  const shown = visible && !isSeen(hint)

  useEffect(() => {
    if (!shown) return undefined
    const close = () => dismiss(hint)
    document.addEventListener('pointerdown', close, { capture: true, once: true })
    return () => document.removeEventListener('pointerdown', close, { capture: true })
  }, [shown, hint, dismiss])

  if (!shown) return null
  return (
    <aside className="coach" role="note" aria-label={title}>
      <strong className="coach-title">{title}</strong>
      <p className="coach-body">{body}</p>
      <button type="button" className="coach-ok" onClick={() => dismiss(hint)}>{strings.hintOk}</button>
    </aside>
  )
}
