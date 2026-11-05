import { useState } from 'react'
import { strings } from '../strings'
import { CloseIcon, SparkIcon } from '../ui/icons'
import { isFlagStored, storeFlag } from '../ui/storedFlag'
import './ask.css'

const DISMISSED_KEY_PREFIX = 'ask.chip.'

export type AskChipPlace = 'budget' | 'reports'

export function AskChip({ place, question, onAsk }: { place: AskChipPlace; question: string; onAsk: (question: string) => void }) {
  const [dismissed, setDismissed] = useState(() => isFlagStored(DISMISSED_KEY_PREFIX + place))
  if (dismissed) return null
  const hide = () => {
    storeFlag(DISMISSED_KEY_PREFIX + place)
    setDismissed(true)
  }
  return (
    <span className="ask-context">
      <button type="button" className="chip ask-chip pressable" onClick={() => { hide(); onAsk(question) }}><SparkIcon />{question}</button>
      <button type="button" className="ask-context-hide" aria-label={strings.askChipHide} onClick={hide}><CloseIcon /></button>
    </span>
  )
}
