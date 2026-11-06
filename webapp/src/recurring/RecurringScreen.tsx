import { useCallback, useState } from 'react'
import { serverErrorCode } from '../api/client'
import type { Group, Me, RecurringPayment } from '../api/types'
import { useAsync } from '../api/useAsync'
import { formatDayLabel, formatSignedMoney } from '../format'
import { updateMe } from '../me/meApi'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { EmptyCard } from '../ui/EmptyCard'
import { RepeatIcon } from '../ui/icons'
import { LimitScreen } from '../ui/LimitScreen'
import { LoadError } from '../ui/LoadError'
import { ScreenHeader } from '../ui/ScreenHeader'
import { SkeletonRows } from '../ui/Skeleton'
import { Switch } from '../ui/Switch'
import type { Notify } from '../ui/useToast'
import { describeRepeat } from './recurrence'
import { fetchRecurring, setRecurringActive } from './recurringApi'
import { RecurringForm } from './RecurringForm'
import './recurring.css'
import '../reminders/reminders.css'

const SKELETON_ROWS = 3

type Mode = { kind: 'list' } | { kind: 'form'; payment: RecurringPayment | null } | { kind: 'limit' }

const paymentName = (payment: RecurringPayment): string => payment.description ?? payment.categoryName

export function RecurringScreen({ me, group, onMeChanged, notify, onBack }: {
  me: Me
  group: Group
  onMeChanged: (me: Me) => void
  notify: Notify
  onBack: () => void
}) {
  const load = useCallback((signal: AbortSignal) => fetchRecurring(group.id, signal), [group.id])
  const recurring = useAsync(load)
  const [mode, setMode] = useState<Mode>({ kind: 'list' })
  const [toggling, setToggling] = useState<string | null>(null)
  const limit = me.subscription.limits.recurringPayments
  const items = recurring.state.status === 'ready' ? recurring.state.data.items : []
  const activeCount = recurring.state.status === 'ready' ? recurring.state.data.activeByMe : 0
  const atLimit = limit !== null && activeCount >= limit
  const money = (type: RecurringPayment['type'], amount: string) => formatSignedMoney(type, amount, group.currency, me.language)

  const showLimit = () => {
    recurring.reload()
    setMode({ kind: 'limit' })
  }

  const done = (title: string, payment: RecurringPayment) => {
    recurring.reload()
    setMode({ kind: 'list' })
    notify({ title, detail: paymentName(payment) })
  }

  const toggle = async (payment: RecurringPayment, active: boolean) => {
    setToggling(payment.id)
    try {
      await setRecurringActive(group.id, payment.id, active)
      recurring.reload()
    } catch (failure) {
      notifyOutcome('error')
      if (serverErrorCode(failure) === 'RECURRING_LIMIT_REACHED') showLimit()
      else notify({ title: strings.toggleFailed })
    } finally {
      setToggling(null)
    }
  }

  const toggleHeadsUp = (enabled: boolean) => {
    updateMe({ recurringHeadsUp: enabled }).then(onMeChanged).catch(() => notify({ title: strings.toggleFailed }))
  }

  if (mode.kind === 'form') {
    return (
      <RecurringForm
        group={group}
        payment={mode.payment}
        onBack={() => setMode({ kind: 'list' })}
        onSaved={(payment, created) => done(created ? strings.recurringCreatedToast : strings.recurringSavedToast, payment)}
        onDeleted={(payment) => done(strings.recurringDeletedToast, payment)}
        onLimit={showLimit}
      />
    )
  }
  if (mode.kind === 'limit' && limit !== null) {
    return (
      <LimitScreen
        header={strings.recurringTitle}
        title={strings.recurringLimitTitle}
        body={strings.recurringLimitBody(limit)}
        meterLabel={strings.recurringLimitMeter}
        used={activeCount}
        limit={limit}
        subscribeUrl={me.subscription.subscribeUrl}
        backLabel={strings.backToRecurring}
        onBack={() => setMode({ kind: 'list' })}
      />
    )
  }

  return (
    <>
      <ScreenHeader
        title={strings.recurringTitle}
        onBack={onBack}
        action={limit !== null && recurring.state.status === 'ready' ? <span className="counter-chip">{strings.limitCounter(activeCount, limit)}</span> : undefined}
      />
      <p className="caption">{strings.recurringHint}</p>
      {recurring.state.status === 'loading' && <SkeletonRows count={SKELETON_ROWS} />}
      {recurring.state.status === 'error' && <LoadError message={recurring.state.message} onRetry={recurring.reload} />}
      {recurring.state.status === 'ready' && (items.length === 0
        ? <EmptyCard icon={<RepeatIcon />} title={strings.recurringEmptyTitle} body={strings.recurringEmptyHint} />
        : (
          <>
            <p className="recurring-totals">
              <span>{strings.recurringPerMonth(money('EXPENSE', recurring.state.data.monthlyExpense))}</span>
              {Number(recurring.state.data.monthlyIncome) > 0 && <span className="amount income">{money('INCOME', recurring.state.data.monthlyIncome)}</span>}
            </p>
            <ul className="rows">
              {items.map((payment) => (
                <li key={payment.id} className={`row reminder-row recurring-row ${payment.active ? '' : 'reminder-off'}`}>
                  <button type="button" className="reminder-open" aria-label={`${strings.editRecurringTitle}: ${paymentName(payment)}`} onClick={() => setMode({ kind: 'form', payment })}>
                    <span className="icon-circle" aria-hidden="true">{payment.categoryIcon}</span>
                    <span className="row-main">
                      <span className="row-title">{paymentName(payment)}</span>
                      <span className="caption">
                        {describeRepeat(payment)} · {payment.active ? strings.recurringNext(formatDayLabel(payment.nextDate, new Date(), me.language)) : strings.recurringPaused}
                      </span>
                    </span>
                    <span className={`amount ${payment.type === 'INCOME' ? 'income' : ''}`}>{money(payment.type, payment.amount)}</span>
                  </button>
                  <Switch
                    checked={payment.active}
                    label={strings.recurringToggleLabel(paymentName(payment))}
                    disabled={toggling === payment.id}
                    onChange={(active) => void toggle(payment, active)}
                  />
                </li>
              ))}
            </ul>
          </>
        ))}
      <div className="switch-row">
        <span className="row-main">
          <span className="row-title">{strings.recurringHeadsUpLabel}</span>
          <span className="caption">{strings.recurringHeadsUpHint}</span>
        </span>
        <Switch checked={me.recurringHeadsUp} label={strings.recurringHeadsUpLabel} onChange={toggleHeadsUp} />
      </div>
      <BottomBar>
        <Button onClick={() => setMode(atLimit ? { kind: 'limit' } : { kind: 'form', payment: null })}>{strings.addRecurring}</Button>
      </BottomBar>
    </>
  )
}
