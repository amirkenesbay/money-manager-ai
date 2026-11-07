import { type ReactNode, useMemo, useState } from 'react'
import { tourTarget } from '../onboarding/tourTarget'
import { ScreenTour } from '../onboarding/ScreenTour'
import type { CategoryType, Group, Me, Operation, OperationTotals } from '../api/types'
import { formatSignedMoney } from '../format'
import { useMembers } from '../groups/useMembers'
import { useCategories } from '../categories/useCategories'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { ChipSelect } from '../ui/ChipSelect'
import { CalendarIcon, CloseIcon, SearchIcon, UserIcon } from '../ui/icons'
import { useDebounced } from '../ui/useDebounced'
import { LoadError } from '../ui/LoadError'
import { PremiumOffer } from '../ui/PremiumOffer'
import { Segmented } from '../ui/Segmented'
import { SkeletonRows } from '../ui/Skeleton'
import { StateView } from '../ui/StateView'
import { OperationDays } from './OperationDays'
import type { OperationActions } from './operationActions'
import './OperationsScreen.css'
import type { OperationsQuery } from './operationsApi'
import { DEFAULT_PERIOD, type Period, PERIODS, periodRange } from './periods'
import { useOperationsFeed } from './useOperationsFeed'

const TYPE_FILTERS: (CategoryType | null)[] = [null, 'EXPENSE', 'INCOME']
const ALL_CATEGORIES = ''
const FEED_SKELETON_ROWS = 6
const ALL_MEMBERS = ''
const SEARCH_DELAY_MS = 300

export function OperationsScreen({ me, group, initialSearch = null, actions, onSelectOperation }: {
  me: Me
  group: Group
  initialSearch?: string | null
  actions?: OperationActions
  onSelectOperation: (operation: Operation) => void
}) {
  const [period, setPeriod] = useState<Period>(DEFAULT_PERIOD)
  const [type, setType] = useState<CategoryType | null>(null)
  const [categoryId, setCategoryId] = useState<string | null>(null)
  const [creatorId, setCreatorId] = useState<number | null>(null)
  const [searchText, setSearchText] = useState(initialSearch ?? '')
  const search = useDebounced(searchText.trim(), SEARCH_DELAY_MS)
  const categories = useCategories(group.id)
  const query = useMemo<OperationsQuery>(
    () => ({ ...periodRange(period, new Date(), group.monthStartDay), type, categoryId, creatorId, search: search || null }),
    [period, type, categoryId, creatorId, search, group.monthStartDay],
  )
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
    setCreatorId(null)
    setSearchText('')
  }
  const visibleCategories = categories.state.status === 'ready'
    ? categories.state.data.filter((category) => type === null || category.type === type)
    : []

  return (
    <>
      <h1 className="title">{strings.operationsTitle}</h1>
      {group.type === 'SHARED' && <p className="caption subtitle">{strings.sharedGroupSubtitle(group.name, group.memberCount)}</p>}
      <Segmented
        label={strings.typeFilterLabel}
        options={TYPE_FILTERS}
        value={type}
        optionKey={(option) => option ?? 'all'}
        optionLabel={strings.typeFilter}
        onChange={selectType}
      />
      <ScreenTour hint="OPERATIONS" />
      <SearchField value={searchText} onChange={setSearchText} />
      <div className="filter-chips" {...tourTarget('operations-filters')}>
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
        {group.type === 'SHARED' && <MemberFilter groupId={group.id} value={creatorId} onChange={setCreatorId} />}
      </div>
      {period === 'allTime' && historyLimited
        ? premiumOffer
        : <OperationsFeed me={me} group={group} query={query} premiumOffer={premiumOffer} actions={actions} onResetFilters={resetFilters} onSelectOperation={onSelectOperation} />}
    </>
  )
}

function OperationsFeed({ me, group, query, premiumOffer, actions, onResetFilters, onSelectOperation }: {
  me: Me
  group: Group
  query: OperationsQuery
  actions?: OperationActions
  premiumOffer: ReactNode
  onResetFilters: () => void
  onSelectOperation: (operation: Operation) => void
}) {
  const feed = useOperationsFeed(group.id, query)

  if (feed.state.status === 'loading' && !feed.refreshing) return <SkeletonRows count={FEED_SKELETON_ROWS} label={strings.operationsLoading} />
  if (feed.state.status === 'error') {
    if (feed.state.code === 'PREMIUM_REQUIRED') return premiumOffer
    return <LoadError message={feed.state.message} onRetry={feed.reload} />
  }
  if (feed.items.length === 0 && !feed.refreshing) {
    return (
      <StateView icon={strings.operationsEmptyIcon} title={strings.operationsEmptyTitle} body={strings.operationsEmptyHint}>
        <Button variant="secondary" size="small" onClick={onResetFilters}>{strings.resetFilters}</Button>
      </StateView>
    )
  }
  return (
    <section className="feed" {...tourTarget('operations-feed')} aria-busy={feed.refreshing}>
      {feed.totals && <SearchTotals totals={feed.totals} group={group} language={me.language} />}
      <OperationDays operations={feed.items} group={group} language={me.language} userId={me.userId} actions={actions} onSelect={onSelectOperation} />
      {feed.loadMoreFailed && <p className="error-text" role="alert">{strings.loadMoreFailed}</p>}
      {feed.refreshing ? null : feed.hasMore
        ? (
          <Button variant="secondary" size="small" disabled={feed.loadingMore} onClick={feed.loadMore}>
            {feed.loadingMore ? strings.loading : strings.loadMore}
          </Button>
        )
        : <p className="feed-end caption">{strings.operationsEnd}</p>}
    </section>
  )
}

function SearchField({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return (
    <label className="search-field" {...tourTarget('operations-search')}>
      <SearchIcon />
      <input
        type="search"
        enterKeyHint="search"
        aria-label={strings.operationsSearchLabel}
        placeholder={strings.operationsSearchPlaceholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      {value && <button type="button" className="search-clear" aria-label={strings.clearSearch} onClick={() => onChange('')}><CloseIcon /></button>}
    </label>
  )
}

function MemberFilter({ groupId, value, onChange }: { groupId: string; value: number | null; onChange: (creatorId: number | null) => void }) {
  const members = useMembers(groupId)
  if (members.state.status !== 'ready' || members.state.data.length < 2) return null
  return (
    <ChipSelect
      label={strings.memberFilterLabel}
      icon={<UserIcon />}
      value={value === null ? ALL_MEMBERS : String(value)}
      options={[
        { value: ALL_MEMBERS, label: strings.allMembers },
        ...members.state.data.map((member) => ({ value: String(member.userId), label: member.name })),
      ]}
      onChange={(next) => onChange(next ? Number(next) : null)}
    />
  )
}

function SearchTotals({ totals, group, language }: { totals: OperationTotals; group: Group; language: string }) {
  const sums = [
    Number(totals.expense) > 0 ? formatSignedMoney('EXPENSE', totals.expense, group.currency, language) : null,
    Number(totals.income) > 0 ? formatSignedMoney('INCOME', totals.income, group.currency, language) : null,
  ].filter(Boolean)
  return (
    <p className="search-totals">
      <span>{strings.searchFound(totals.count)}</span>
      {sums.map((sum) => <span key={sum} className="amount">{sum}</span>)}
    </p>
  )
}
