import { useState } from 'react'
import { tourTarget } from '../onboarding/tourTarget'
import { ScreenTour } from '../onboarding/ScreenTour'
import { serverErrorCode } from '../api/client'
import type { Me, Reminder } from '../api/types'
import { useAsync } from '../api/useAsync'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { EmptyCard } from '../ui/EmptyCard'
import { BellIcon, SendIcon } from '../ui/icons'
import { LimitScreen } from '../ui/LimitScreen'
import { LoadError } from '../ui/LoadError'
import { ScreenHeader } from '../ui/ScreenHeader'
import { SkeletonRows } from '../ui/Skeleton'
import { Switch } from '../ui/Switch'
import type { Notify } from '../ui/useToast'
import { fetchReminders, setReminderActive } from './remindersApi'
import { ReminderForm } from './ReminderForm'
import { describeSchedule } from './schedule'
import './reminders.css'

const SKELETON_ROWS = 3

type Mode = { kind: 'list' } | { kind: 'form'; reminder: Reminder | null } | { kind: 'limit' }


export function RemindersScreen({ me, notify, onBack }: {
  me: Me
  notify: Notify
  onBack: () => void
}) {
  const reminders = useAsync(fetchReminders)
  const [patched, setPatched] = useState<Record<string, Reminder>>({})
  const [toggling, setToggling] = useState<string | null>(null)
  const [mode, setMode] = useState<Mode>({ kind: 'list' })
  const { subscription } = me
  const limit = subscription.limits.activeNotifications
  const list = reminders.state.status === 'ready' ? reminders.state.data.map((reminder) => patched[reminder.id] ?? reminder) : []
  const activeCount = list.filter((reminder) => reminder.isActive).length
  const atLimit = limit !== null && activeCount >= limit

  const refresh = (title: string, reminder: Reminder) => {
    setPatched({})
    reminders.reload()
    setMode({ kind: 'list' })
    notify({ title, detail: `${reminder.icon} ${reminder.name}` })
  }

  const toggle = async (reminder: Reminder, active: boolean) => {
    if (active && atLimit) {
      setMode({ kind: 'limit' })
      return
    }
    setToggling(reminder.id)
    try {
      const updated = await setReminderActive(reminder.id, active)
      setPatched((current) => ({ ...current, [updated.id]: updated }))
    } catch (failure) {
      notifyOutcome('error')
      if (serverErrorCode(failure) === 'NOTIFICATION_LIMIT_REACHED') setMode({ kind: 'limit' })
      else notify({ title: strings.toggleFailed })
    } finally {
      setToggling(null)
    }
  }

  if (mode.kind === 'form') {
    return (
      <ReminderForm
        reminder={mode.reminder}
        language={me.language}
        onBack={() => setMode({ kind: 'list' })}
        onSaved={(reminder, created) => refresh(created ? strings.reminderCreatedToast : strings.reminderSavedToast, reminder)}
        onDeleted={(reminder) => refresh(strings.reminderDeletedToast, reminder)}
        onLimit={() => setMode({ kind: 'limit' })}
      />
    )
  }
  if (mode.kind === 'limit' && limit !== null) {
    return (
      <LimitScreen
        header={strings.remindersTitle}
        title={strings.reminderLimitTitle}
        body={strings.reminderLimitBody(limit)}
        meterLabel={strings.reminderLimitMeter}
        used={activeCount}
        limit={limit}
        subscribeUrl={subscription.subscribeUrl}
        backLabel={strings.backToReminders}
        onBack={() => setMode({ kind: 'list' })}
      />
    )
  }

  return (
    <>
      <ScreenHeader
        title={strings.remindersTitle}
        onBack={onBack}
        action={limit !== null && reminders.state.status === 'ready' ? <span className="counter-chip">{strings.limitCounter(activeCount, limit)}</span> : undefined}
      />
      <p className="reminders-hint caption"><SendIcon />{strings.remindersHint}</p>
      {reminders.state.status === 'loading' && <SkeletonRows count={SKELETON_ROWS} />}
      {reminders.state.status === 'error' && <LoadError message={reminders.state.message} onRetry={reminders.reload} />}
      {reminders.state.status === 'ready' && (list.length === 0
        ? <EmptyCard icon={<BellIcon />} title={strings.remindersEmptyTitle} body={strings.remindersEmptyHint} />
        : (
          <ul className="rows">
            {list.map((reminder) => (
              <li key={reminder.id} className={`row reminder-row ${reminder.isActive ? '' : 'reminder-off'}`}>
                <button type="button" className="reminder-open" aria-label={`${strings.editReminder}: ${reminder.name}`} onClick={() => setMode({ kind: 'form', reminder })}>
                  <span className="icon-circle" aria-hidden="true">{reminder.icon}</span>
                  <span className="row-main">
                    <span className="row-title">{reminder.name}</span>
                    <span className="caption">{describeSchedule(reminder, me.language)}</span>
                  </span>
                </button>
                <Switch
                  checked={reminder.isActive}
                  label={strings.reminderToggleLabel(reminder.name)}
                  disabled={toggling === reminder.id}
                  onChange={(active) => void toggle(reminder, active)}
                />
              </li>
            ))}
          </ul>
        ))}
      {limit !== null && <p className="caption centered">{strings.remindersFreeNote(limit)}</p>}
      <BottomBar>
        <Button {...tourTarget('reminders-add')} onClick={() => setMode(atLimit ? { kind: 'limit' } : { kind: 'form', reminder: null })}>{strings.addReminder}</Button>
        <ScreenTour hint="REMINDERS" />
      </BottomBar>
    </>
  )
}
