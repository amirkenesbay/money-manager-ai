import { useContext, useMemo } from 'react'
import { strings } from '../strings'
import { HintVisibilityContext, useHints } from './hintsContext'
import { type ScreenHint, screenTourSteps } from './screenTours'
import { SpotlightTour } from './SpotlightTour'

export function ScreenTour({ hint }: { hint: ScreenHint }) {
  const { isSeen, dismiss } = useHints()
  const visible = useContext(HintVisibilityContext)
  const steps = useMemo(() => screenTourSteps(hint), [hint])
  if (!visible || !isSeen('WELCOME') || isSeen(hint)) return null
  return <SpotlightTour steps={steps} finishLabel={strings.tourDone} onFinish={() => dismiss(hint)} />
}
