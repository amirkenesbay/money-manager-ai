import type { Group, Operation } from '../api/types'
import { BudgetsSummary } from '../budgets/BudgetsSummary'
import { formatPeriod } from '../format'
import { OperationDays } from '../operations/OperationDays'
import { strings } from '../strings'
import { ArrowDownIcon, ChevronDownIcon } from '../ui/icons'
import { LoadError } from '../ui/LoadError'
import { SkeletonCard, SkeletonRows } from '../ui/Skeleton'
import './Dashboard.css'
import { HomeHero } from './HomeHero'
import { useDashboard } from './useDashboard'

const RECENT_SKELETON_ROWS = 3

export function Dashboard({ groups, groupId, language, switchError, onSwitchGroup, onOpenOperations, onOpenBudgets, onAddIncome, onSelectOperation }: {
  groups: Group[]
  groupId: string
  language: string
  switchError: string | null
  onSwitchGroup: (groupId: string) => void
  onOpenOperations: () => void
  onOpenBudgets: () => void
  onAddIncome: () => void
  onSelectOperation: (operation: Operation) => void
}) {
  const dashboard = useDashboard(groupId)
  const ready = dashboard.state.status === 'ready' ? dashboard.state.data : null
  const group = groups.find((candidate) => candidate.id === groupId)

  return (
    <>
      <header className="home-header">
        <GroupSwitch groups={groups} activeGroupId={groupId} onSwitch={onSwitchGroup} />
        {ready && <span className="caption home-period">{formatPeriod(ready.summary.from, ready.summary.to, language)}</span>}
      </header>
      {switchError && <p className="error-text" role="alert">{switchError}</p>}
      {dashboard.state.status === 'loading' && <DashboardSkeleton />}
      {dashboard.state.status === 'error' && <LoadError message={dashboard.state.message} onRetry={dashboard.reload} />}
      {ready && group && (
        <>
          <HomeHero balance={ready.balance} summary={ready.summary} language={language} onAddIncome={onAddIncome} />
          <BudgetsSummary groupId={groupId} currency={ready.balance.currency} language={language} onOpenAll={onOpenBudgets} />
          <RecentOperations
            operations={ready.operations.items}
            group={group}
            language={language}
            onOpenOperations={onOpenOperations}
            onSelectOperation={onSelectOperation}
          />
        </>
      )}
    </>
  )
}

function GroupSwitch({ groups, activeGroupId, onSwitch }: {
  groups: Group[]
  activeGroupId: string
  onSwitch: (groupId: string) => void
}) {
  const active = groups.find((group) => group.id === activeGroupId)
  if (groups.length < 2) return <h1 className="home-group">{active?.name}</h1>
  return (
    <label className="home-group home-group-switch">
      <span>{active?.name}</span>
      <ChevronDownIcon />
      <select aria-label={strings.groupLabel} value={activeGroupId} onChange={(event) => onSwitch(event.target.value)}>
        {groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
      </select>
    </label>
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

function RecentOperations({ operations, group, language, onOpenOperations, onSelectOperation }: {
  operations: Operation[]
  group: Group
  language: string
  onOpenOperations: () => void
  onSelectOperation: (operation: Operation) => void
}) {
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
        : <OperationDays operations={operations} group={group} language={language} showDayTotal={false} onSelect={onSelectOperation} />}
    </section>
  )
}
