import type { Balance, Currency, Group, MonthSummary, Operation } from '../api/types'
import { formatDayLabel, formatMoney, formatPeriod, formatSignedMoney } from '../format'
import { OperationList } from '../operations/OperationList'
import { strings } from '../strings'
import { Moment } from '../three/Moment'
import { AlertIcon, ArrowDownIcon, ChevronDownIcon, ChevronRightIcon, KeyboardIcon, MicIcon } from '../ui/icons'
import { LoadError } from '../ui/LoadError'
import { SkeletonCard, SkeletonRows } from '../ui/Skeleton'
import { useCountUp } from '../ui/useCountUp'
import './Dashboard.css'
import { useDashboard } from './useDashboard'
import { isCalendarMonth } from '../operations/periods'

const RECENT_SKELETON_ROWS = 3

export function Dashboard({ name, groups, groupId, language, switchError, onSwitchGroup, onOpenOperations, onOpenReports, onAddOperation, onRecordVoice, onSelectOperation }: {
  name: string
  groups: Group[]
  groupId: string
  language: string
  switchError: string | null
  onSwitchGroup: (groupId: string) => void
  onOpenOperations: () => void
  onOpenReports: () => void
  onAddOperation: () => void
  onRecordVoice: () => void
  onSelectOperation: (operation: Operation) => void
}) {
  const dashboard = useDashboard(groupId)
  const ready = dashboard.state.status === 'ready' ? dashboard.state.data : null

  return (
    <>
      <header className="home-header">
        <span className="home-greeting">
          <span className="hint">{strings.greetingLabel}</span>
          <span className="title">{name}</span>
        </span>
        <GroupChip groups={groups} activeGroupId={groupId} onSwitch={onSwitchGroup} />
      </header>
      {switchError && <p className="error-text" role="alert">{switchError}</p>}
      {dashboard.state.status === 'loading' && <DashboardSkeleton />}
      {dashboard.state.status === 'error' && <LoadError message={dashboard.state.message} onRetry={dashboard.reload} />}
      {ready && (
        <>
          <BalanceCard balance={ready.balance} summary={ready.summary} language={language} onOpenReport={onOpenReports} />
          <RecentOperations
            operations={ready.operations.items}
            currency={ready.balance.currency}
            language={language}
            onOpenOperations={onOpenOperations}
            onSelectOperation={onSelectOperation}
          />
        </>
      )}
      <VoiceEntry firstTime={ready?.operations.items.length === 0} onRecord={onRecordVoice} onManual={onAddOperation} />
    </>
  )
}

function GroupChip({ groups, activeGroupId, onSwitch }: {
  groups: Group[]
  activeGroupId: string
  onSwitch: (groupId: string) => void
}) {
  const active = groups.find((group) => group.id === activeGroupId)
  const switchable = groups.length > 1
  return (
    <span className={`chip group-chip ${switchable ? 'pressable' : ''}`}>
      <span className="group-chip-text">
        <span className="caption">{strings.groupLabel}</span>
        <span className="row-title">{active?.name}</span>
      </span>
      {switchable && (
        <>
          <ChevronDownIcon />
          <select aria-label={strings.groupLabel} value={activeGroupId} onChange={(event) => onSwitch(event.target.value)}>
            {groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
          </select>
        </>
      )}
    </span>
  )
}

function DashboardSkeleton() {
  return (
    <div className="home-skeleton" aria-busy="true" aria-label={strings.loading}>
      <SkeletonCard />
      <SkeletonRows count={RECENT_SKELETON_ROWS} />
    </div>
  )
}

function BalanceCard({ balance, summary, language, onOpenReport }: {
  balance: Balance
  summary: MonthSummary
  language: string
  onOpenReport: () => void
}) {
  const total = useCountUp(Number(balance.total))
  const shownTotal = total === Number(balance.total) ? balance.total : String(Math.round(total))
  const overspent = Number(summary.net) < 0
  return (
    <section className="well balance">
      <Moment kind="wave" input={{ level: balanceLevel(balance, summary) }} className="balance-wave" fallback={null} />
      <span className="balance-head">
        <h2 className="balance-title">{strings.balanceTitle}</h2>
        <button type="button" className="link-button" onClick={onOpenReport}>{strings.reportLink}<ChevronRightIcon /></button>
      </span>
      <p className={`amount balance-total ${Number(balance.total) < 0 ? 'negative' : ''}`}>
        {formatMoney(shownTotal, balance.currency, language)}
      </p>
      {!isCalendarMonth(summary) && <p className="caption">{formatPeriod(summary.from, summary.to, language)}</p>}
      <MonthFlow summary={summary} language={language} />
      {overspent && (
        <p className="balance-warning negative">
          <AlertIcon />
          {strings.monthOverspent(formatMoney(String(Math.abs(Number(summary.net))), summary.currency, language))}
        </p>
      )}
    </section>
  )
}

function balanceLevel(balance: Balance, summary: MonthSummary): number {
  const total = Number(balance.total)
  return total > 0 ? total / (total + Number(summary.expense)) : 0
}

function MonthFlow({ summary, language }: { summary: MonthSummary; language: string }) {
  if (summary.operationCount === 0) return <p className="balance-flow caption">{strings.monthNoExpense}</p>
  return (
    <p className="balance-flow amount">
      <span>{strings.monthExpense(formatSignedMoney('EXPENSE', summary.expense, summary.currency, language))}</span>
      <span className="income">{strings.monthIncome(formatSignedMoney('INCOME', summary.income, summary.currency, language))}</span>
    </p>
  )
}

function RecentOperations({ operations, currency, language, onOpenOperations, onSelectOperation }: {
  operations: Operation[]
  currency: Currency
  language: string
  onOpenOperations: () => void
  onSelectOperation: (operation: Operation) => void
}) {
  const today = new Date()
  return (
    <section className="recent">
      <span className="recent-head">
        <h2 className="section-title">{strings.recentOperationsTitle}</h2>
        {operations.length > 0 && <button type="button" className="link-button" onClick={onOpenOperations}>{strings.allOperations}</button>}
      </span>
      {operations.length === 0
        ? (
          <>
            <p className="recent-empty caption">{strings.recentOperationsEmpty}</p>
            <span className="recent-empty-arrow"><ArrowDownIcon /></span>
          </>
        )
        : <OperationList operations={operations} currency={currency} language={language} note={(operation) => formatDayLabel(operation.operationDate, today, language)} onSelect={onSelectOperation} />}
    </section>
  )
}

function VoiceEntry({ firstTime, onRecord, onManual }: {
  firstTime: boolean
  onRecord: () => void
  onManual: () => void
}) {
  return (
    <section className="voice">
      <span className="voice-text">
        <span className="voice-title">{firstTime ? strings.voiceTitleFirst : strings.voiceTitle}</span>
        <span className="caption">{strings.voiceExample}</span>
      </span>
      <div className="voice-controls">
        <span aria-hidden="true" />
        <span className="voice-ring">
          <span className="voice-inner-ring">
            <button type="button" className="voice-button pressable" aria-label={strings.recordVoice} onClick={onRecord}><MicIcon /></button>
          </span>
        </span>
        <button type="button" className="manual-button" aria-label={strings.manualEntryLabel} onClick={onManual}>
          <span className="manual-circle pressable"><KeyboardIcon /></span>
          {strings.manualEntry}
        </button>
      </div>
    </section>
  )
}
