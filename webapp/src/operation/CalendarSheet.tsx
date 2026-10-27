import { useState } from 'react'
import { formatDayLabel } from '../format'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { Sheet } from '../ui/Sheet'
import { Calendar } from './Calendar'

export function CalendarSheet({ value, max, language, onPick, onClose }: {
  value: string
  max: string
  language: string
  onPick: (date: string) => void
  onClose: () => void
}) {
  const [picked, setPicked] = useState(value)
  return (
    <Sheet label={strings.calendarLabel} onClose={onClose}>
      <Calendar value={picked} max={max} language={language} onChange={setPicked} />
      <p className="caption">{strings.futureDatesHint}</p>
      <Button onClick={() => onPick(picked)}>{strings.confirmDate(formatDayLabel(picked, new Date(), language).toLowerCase())}</Button>
    </Sheet>
  )
}
