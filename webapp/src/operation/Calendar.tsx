import { useState } from 'react'
import { formatMonthTitle, shiftMonth, toIsoDate } from '../format'
import { strings } from '../strings'
import { ChevronLeftIcon, ChevronRightIcon } from '../ui/icons'

const DAYS_IN_WEEK = 7
const KNOWN_MONDAY = new Date(2024, 0, 1)

export function Calendar({ value, max, language, onChange }: {
  value: string
  max: string
  language: string
  onChange: (date: string) => void
}) {
  const [month, setMonth] = useState(value.slice(0, 7))
  const lastMonth = max.slice(0, 7)
  return (
    <div className="calendar">
      <div className="month-nav">
        <button type="button" className="round-button pressable month-nav-arrow" aria-label={strings.previousMonth} onClick={() => setMonth(shiftMonth(month, -1))}>
          <ChevronLeftIcon />
        </button>
        <span className="month-nav-title" aria-live="polite">{formatMonthTitle(month, language)}</span>
        <button type="button" className="round-button pressable month-nav-arrow" aria-label={strings.nextMonth} disabled={month >= lastMonth} onClick={() => setMonth(shiftMonth(month, 1))}>
          <ChevronRightIcon />
        </button>
      </div>
      <div className="calendar-grid">
        {weekdayNames(language).map((name) => <span key={name} className="caption" aria-hidden="true">{name}</span>)}
        {monthCells(month).map((date, index) => date === null
          ? <span key={`blank-${index}`} />
          : (
            <button
              key={date}
              type="button"
              className="calendar-day"
              aria-pressed={date === value}
              aria-current={date === max ? 'date' : undefined}
              disabled={date > max}
              onClick={() => onChange(date)}
            >
              {Number(date.slice(8))}
            </button>
          ))}
      </div>
    </div>
  )
}

function weekdayNames(language: string): string[] {
  const format = new Intl.DateTimeFormat(language, { weekday: 'short' })
  return Array.from({ length: DAYS_IN_WEEK }, (_, index) => {
    const name = format.format(new Date(KNOWN_MONDAY.getFullYear(), KNOWN_MONDAY.getMonth(), KNOWN_MONDAY.getDate() + index))
    return name.charAt(0).toUpperCase() + name.slice(1)
  })
}

function monthCells(month: string): (string | null)[] {
  const [year, monthNumber] = month.split('-').map(Number)
  const first = new Date(year, monthNumber - 1, 1)
  const leadingBlanks = (first.getDay() + DAYS_IN_WEEK - 1) % DAYS_IN_WEEK
  const daysInMonth = new Date(year, monthNumber, 0).getDate()
  const days = Array.from({ length: daysInMonth }, (_, index) => toIsoDate(new Date(year, monthNumber - 1, index + 1)))
  return [...Array<null>(leadingBlanks).fill(null), ...days]
}
