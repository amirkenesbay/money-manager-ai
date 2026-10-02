import { type ReactNode, useCallback, useMemo, useState } from 'react'
import type { CategoryType, Group, Me } from '../api/types'
import { useAsync } from '../api/useAsync'
import { strings } from '../strings'
import { LoadError } from '../ui/LoadError'
import { PremiumOffer } from '../ui/PremiumOffer'
import { ScreenHeader } from '../ui/ScreenHeader'
import { OperationList } from './OperationList'
import { fetchCategories, type OperationsQuery } from './operationsApi'
import { DEFAULT_PERIOD, type Period, PERIODS, periodRange } from './periods'
import { useOperationsFeed } from './useOperationsFeed'

const TYPE_FILTERS: (CategoryType | null)[] = [null, 'EXPENSE', 'INCOME']

export function OperationsScreen({ me, group, onBack }: { me: Me; group: Group; onBack: () => void }) {
  const [period, setPeriod] = useState<Period>(DEFAULT_PERIOD)
  const [type, setType] = useState<CategoryType | null>(null)
  const [categoryId, setCategoryId] = useState<string | null>(null)
  const loadCategories = useCallback((signal: AbortSignal) => fetchCategories(group.id, signal), [group.id])
  const categories = useAsync(loadCategories)
  const query = useMemo<OperationsQuery>(() => ({ ...periodRange(period, new Date()), type, categoryId }), [period, type, categoryId])
  const historyLimited = me.subscription.limits.historyDaysBack !== null
  const premiumOffer = (
    <PremiumOffer historyDaysBack={me.subscription.limits.historyDaysBack} subscribeUrl={me.subscription.subscribeUrl} />
  )

  const selectType = (next: CategoryType | null) => {
    setType(next)
    setCategoryId(null)
  }
  const visibleCategories = categories.state.status === 'ready'
    ? categories.state.data.filter((category) => type === null || category.type === type)
    : []

  return (
    <main className="screen">
      <ScreenHeader title={strings.operationsTitle} onBack={onBack} />
      <section className="filters" aria-label={strings.filtersTitle}>
        <div className="segmented" role="group" aria-label={strings.typeFilterLabel}>
          {TYPE_FILTERS.map((option) => (
            <button key={option ?? 'all'} type="button" aria-pressed={type === option} onClick={() => selectType(option)}>
              {strings.typeFilter(option)}
            </button>
          ))}
        </div>
        <select aria-label={strings.periodFilterLabel} value={period} onChange={(event) => setPeriod(event.target.value as Period)}>
          {PERIODS.map((option) => <option key={option} value={option}>{strings.period(option)}</option>)}
        </select>
        {visibleCategories.length > 0 && (
          <select aria-label={strings.categoryFilterLabel} value={categoryId ?? ''} onChange={(event) => setCategoryId(event.target.value || null)}>
            <option value="">{strings.allCategories}</option>
            {visibleCategories.map((category) => (
              <option key={category.id} value={category.id}>{category.icon} {category.name}</option>
            ))}
          </select>
        )}
      </section>
      {period === 'allTime' && historyLimited
        ? premiumOffer
        : <OperationsFeed me={me} group={group} query={query} premiumOffer={premiumOffer} />}
    </main>
  )
}

function OperationsFeed({ me, group, query, premiumOffer }: {
  me: Me
  group: Group
  query: OperationsQuery
  premiumOffer: ReactNode
}) {
  const feed = useOperationsFeed(group.id, query)

  if (feed.state.status === 'loading') return <p className="hint">{strings.loading}</p>
  if (feed.state.status === 'error') {
    if (feed.state.code === 'PREMIUM_REQUIRED') return premiumOffer
    return <LoadError message={feed.state.message} onRetry={feed.reload} />
  }
  if (feed.items.length === 0) {
    return (
      <section className="card">
        <p>{strings.operationsEmptyTitle}</p>
        <p className="hint">{strings.operationsEmptyHint}</p>
      </section>
    )
  }
  return (
    <section className="card">
      <OperationList operations={feed.items} currency={group.currency} language={me.language} />
      {feed.loadMoreFailed && <p className="error-text" role="alert">{strings.loadMoreFailed}</p>}
      {feed.hasMore && (
        <button type="button" className="secondary-button" disabled={feed.loadingMore} onClick={feed.loadMore}>
          {feed.loadingMore ? strings.loading : strings.loadMore}
        </button>
      )}
    </section>
  )
}
