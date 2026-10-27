import { useId } from 'react'
import { strings } from '../strings'
import { Segmented } from '../ui/Segmented'
import {
  anchorOf,
  formatTime,
  frequencyOf,
  INTERVAL_UNITS,
  MAX_DAY_OF_MONTH,
  MAX_INTERVAL,
  MONTH_COUNT,
  monthName,
  parseTime,
  REPEAT_OPTIONS,
  type ScheduleDraft,
  WEEKDAYS,
  weekdayName,
} from './schedule'

const DAYS_OF_MONTH = Array.from({ length: MAX_DAY_OF_MONTH }, (_, index) => index + 1)
const MONTHS = Array.from({ length: MONTH_COUNT }, (_, index) => index + 1)

export function ScheduleFields({ draft, language, intervalInvalid, onChange }: {
  draft: ScheduleDraft
  language: string
  intervalInvalid: boolean
  onChange: (draft: ScheduleDraft) => void
}) {
  const ids = { interval: useId(), dayOfMonth: useId(), month: useId(), time: useId() }
  const anchor = anchorOf(frequencyOf(draft))
  const change = (patch: Partial<ScheduleDraft>) => onChange({ ...draft, ...patch })

  return (
    <>
      <fieldset className="field-group">
        <legend className="field-label">{strings.repeatLabel}</legend>
        <Segmented variant="choices" label={strings.repeatLabel} options={REPEAT_OPTIONS} value={draft.repeat} optionKey={(option) => option} optionLabel={strings.repeatOption} onChange={(repeat) => change({ repeat })} />
      </fieldset>
      {draft.repeat === 'custom' && (
        <div className="interval-row">
          <label className="field-label" htmlFor={ids.interval}>{strings.intervalLabel}</label>
          <input
            id={ids.interval}
            className={`text-field interval-input ${intervalInvalid ? 'field-invalid' : ''}`}
            type="number"
            inputMode="numeric"
            min={1}
            max={MAX_INTERVAL[draft.unit]}
            value={Number.isNaN(draft.interval) ? '' : draft.interval}
            aria-invalid={intervalInvalid}
            onChange={(event) => change({ interval: event.target.valueAsNumber })}
          />
          <Segmented label={strings.intervalLabel} options={INTERVAL_UNITS} value={draft.unit} optionKey={(unit) => unit} optionLabel={strings.intervalUnit} onChange={(unit) => change({ unit })} />
        </div>
      )}
      {anchor === 'weekday' && (
        <fieldset className="field-group">
          <legend className="field-label">{strings.weekdayLabel}</legend>
          <Segmented variant="choices" label={strings.weekdayLabel} options={WEEKDAYS} value={draft.weekday} optionKey={(day) => day} optionLabel={(day) => weekdayName(day, language)} onChange={(weekday) => change({ weekday })} />
        </fieldset>
      )}
      {(anchor === 'monthDay' || anchor === 'yearDay') && (
        <div className="select-row">
          {anchor === 'yearDay' && (
            <span className="select-field">
              <label className="field-label" htmlFor={ids.month}>{strings.monthLabel}</label>
              <select id={ids.month} className="text-field" value={draft.month} onChange={(event) => change({ month: Number(event.target.value) })}>
                {MONTHS.map((month) => <option key={month} value={month}>{monthName(month, language)}</option>)}
              </select>
            </span>
          )}
          <span className="select-field">
            <label className="field-label" htmlFor={ids.dayOfMonth}>{strings.dayOfMonthLabel}</label>
            <select id={ids.dayOfMonth} className="text-field" value={draft.dayOfMonth} onChange={(event) => change({ dayOfMonth: Number(event.target.value) })}>
              {DAYS_OF_MONTH.map((day) => <option key={day} value={day}>{day}</option>)}
            </select>
          </span>
        </div>
      )}
      {(anchor === 'monthDay' || anchor === 'yearDay') && <p className="caption">{strings.dayOfMonthHint}</p>}
      <span className="select-field">
        <label className="field-label" htmlFor={ids.time}>{strings.timeLabel}</label>
        <input
          id={ids.time}
          className="text-field"
          type="time"
          value={formatTime(draft.hour, draft.minute)}
          onChange={(event) => {
            const time = parseTime(event.target.value)
            if (time) change(time)
          }}
        />
      </span>
    </>
  )
}
