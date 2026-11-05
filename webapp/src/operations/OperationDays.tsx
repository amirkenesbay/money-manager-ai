import type { Group, Operation } from '../api/types'
import { formatDayLabel, formatSignedMoney } from '../format'
import { groupByDay } from './groupByDay'
import { OperationList } from './OperationList'

export function OperationDays({ operations, group, language, userId, showDayTotal = true, onSelect }: {
  operations: Operation[]
  group: Group
  language: string
  userId?: number
  showDayTotal?: boolean
  onSelect: (operation: Operation) => void
}) {
  const today = new Date()
  const note = (operation: Operation) => {
    const parts = group.type === 'SHARED' ? [operation.creatorName, operation.description] : [operation.description]
    return parts.filter(Boolean).join(' · ')
  }
  return groupByDay(operations).map((day) => {
    const label = formatDayLabel(day.date, today, language)
    const net = formatSignedMoney(day.net < 0 ? 'EXPENSE' : 'INCOME', String(Math.abs(day.net)), group.currency, language)
    return (
      <section key={day.date} className="day" aria-label={label}>
        <h2 className="day-header"><span>{label}</span>{showDayTotal && <span>{net}</span>}</h2>
        <OperationList operations={day.operations} currency={group.currency} language={language} userId={userId} note={note} onSelect={onSelect} />
      </section>
    )
  })
}
