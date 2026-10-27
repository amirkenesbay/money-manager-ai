import { type ReactNode, useMemo, useState } from 'react'
import type { CategoryType, Group, Me, Operation } from '../api/types'
import { useCategories } from '../categories/useCategories'
import { formatDayLabel, formatSignedMoney } from '../format'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { ChipSelect } from '../ui/ChipSelect'
import { CalendarIcon, PlusIcon } from '../ui/icons'
import { LoadError } from '../ui/LoadError'
import { PremiumOffer } from '../ui/PremiumOffer'
import { Segmented } from '../ui/Segmented'
import { SkeletonRows } from '../ui/Skeleton'
import { StateView } from '../ui/StateView'
import { groupByDay } from './groupByDay'
import { OperationList } from './OperationList'
import './OperationsScreen.css'
import type { OperationsQuery } from './operationsApi'
import { DEFAULT_PERIOD, type Period, PERIODS, periodRange } from './periods'
import { useOperationsFeed } from './useOperationsFeed'

const TYPE_FILTERS: (CategoryType | null)[] = [null, 'EXPENSE', 'INCOME']
const ALL_CATEGORIES = ''
const FEED_SKELETON_ROWS = 6

export function OperationsScreen({ me, group, onAddOperation, onSelectOperation }: {
  me: Me
  group: Group
  onAddOperation: () => void
  onSelectOperation: (operation: Operation) => void
}) {
  const [period, setPeriod] = useState<Period>(DEFAULT_PERIOD)
  const [type, setType] = useState<CategoryType | null>(null)
  const [categoryId, setCategoryId] = useState<string | null>(null)
  const categories = useCategories(group.id)
  const query = useMemo<OperationsQuery>(() => ({ ...periodRange(period, new Date()), type, categoryId }), [period, type, categoryId])
  const historyLimited = me.subscription.limits.historyDaysBack !== null
  const premiumOffer = (
    <PremiumOffer historyDaysBack={me.subscription.limits.historyDaysBack} subscribeUrl={me.subscription.subscribeUrl} />
  )

  const selectType = (next: CategoryType | null) => {
    setType(next)
    setCategoryId(null)
  }
  const resetFilters = () => {
    setPeriod(DEFAULT_PERIOD)
    selectType(null)
  }
  const visibleCategories = categories.state.status === 'ready'
    ? categories.state.data.filter((category) => type === null || category.type === type)
    : []

  return (
    <>
      <header className="screen-header-row">
        <h1 className="title">{strings.operationsTitle}</h1>
        <button type="button" className="round-button pressable" aria-label={strings.addOperation} onClick={onAddOperation}><PlusIcon /></button>
      </header>
      {group.type === 'SHARED' && <p className="caption subtitle">{strings.sharedGroupSubtitle(group.name, group.memberCount)}</p>}
      <Segmented
        label={strings.typeFilterLabel}
        options={TYPE_FILTERS}
        value={type}
        optionKey={(option) => option ?? 'all'}
        optionLabel={strings.typeFilter}
        onChange={selectType}
      />
      <div className="filter-chips">
        <ChipSelect
          label={strings.periodFilterLabel}
          icon={<CalendarIcon />}
          value={period}
          options={PERIODS.map((option) => ({ value: option, label: strings.period(option) }))}
          onChange={(value) => setPeriod(value as Period)}
        />
        {visibleCategories.length > 0 && (
          <ChipSelect
            label={strings.categoryFilterLabel}
            value={categoryId ?? ALL_CATEGORIES}
            options={[
              { value: ALL_CATEGORIES, label: strings.allCategories },
              ...visibleCategories.map((category) => ({ value: category.id, label: `${category.icon} ${category.name}` })),
            ]}
            onChange={(value) => setCategoryId(value || null)}
          />
        )}
      </div>
      {period === 'allTime' && historyLimited
        ? premiumOffer
        : <OperationsFeed me={me} group={group} query={query} premiumOffer={premiumOffer} onResetFilters={resetFilters} onSelectOperation={onSelectOperation} />}
    </>
  )
}

function OperationsFeed({ me, group, query, premiumOffer, onResetFilters, onSelectOperation }: {
  me: Me
  group: Group
  query: OperationsQuery
  premiumOffer: ReactNode
  onResetFilters: () => void
  onSelectOperation: (operation: Operation) => void
}) {
  const feed = useOperationsFeed(group.id, query)

  if (feed.state.status === 'loading') return <SkeletonRows count={FEED_SKELETON_ROWS} label={strings.operationsLoading} />
  if (feed.state.status === 'error') {
    if (feed.state.code === 'PREMIUM_REQUIRED') return premiumOffer
    return <LoadError message={feed.state.message} onRetry={feed.reload} />
  }
  if (feed.items.length === 0) {
    return (
      <StateView icon={strings.operationsEmptyIcon} title={strings.operationsEmptyTitle} body={strings.operationsEmptyHint}>
        <Button variant="secondary" size="small" onClick={onResetFilters}>{strings.resetFilters}</Button>
      </StateView>
    )
  }
  return (
    <section className="feed">
      <OperationDays operations={feed.items} group={group} language={me.language} onSelect={onSelectOperation} />
      {feed.loadMoreFailed && <p className="error-text" role="alert">{strings.loadMoreFailed}</p>}
      {feed.hasMore
        ? (
          <Button variant="secondary" size="small" disabled={feed.loadingMore} onClick={feed.loadMore}>
            {feed.loadingMore ? strings.loading : strings.loadMore}
          </Button>
        )
        : <p className="feed-end caption">{strings.operationsEnd}</p>}
    </section>
  )
}

function OperationDays({ operations, group, language, onSelect }: {
  operations: Operation[]
  group: Group
  language: string
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
        <h2 className="day-header"><span>{label}</span><span>{net}</span></h2>
        <OperationList operations={day.operations} currency={group.currency} language={language} note={note} onSelect={onSelect} />
      </section>
    )
  })
}
