import { useState } from 'react'
import { tourTarget } from '../onboarding/tourTarget'
import { ScreenTour } from '../onboarding/ScreenTour'
import type { Budget, Category, CategoryType, Currency, Group, Me } from '../api/types'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { EmptyCard } from '../ui/EmptyCard'
import { ChevronRightIcon, GridIcon, GripIcon } from '../ui/icons'
import { LimitScreen } from '../ui/LimitScreen'
import { LoadError } from '../ui/LoadError'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Segmented } from '../ui/Segmented'
import { SkeletonRows } from '../ui/Skeleton'
import type { Notify } from '../ui/useToast'
import { CategoryForm } from './CategoryForm'
import { RememberedPlaces } from './RememberedPlaces'
import { CATEGORY_TYPES } from './categoryTypes'
import { useCategories } from './useCategories'
import { reorderCategories } from './categoriesApi'
import { useReorder } from '../ui/useReorder'
import { serverMessage } from '../api/client'
import { useBudgets } from '../budgets/useBudgets'
import { budgetColor } from '../budgets/budgetTone'
import { Meter } from '../ui/Meter'
import { formatMoney } from '../format'

const SKELETON_ROWS = 5

type Mode = { kind: 'list' } | { kind: 'form'; category: Category | null } | { kind: 'limit' } | { kind: 'budgetLimit' }

export function CategoriesScreen({ me, group, notify, onBack }: {
  me: Me
  group: Group
  notify: Notify
  onBack: () => void
}) {
  const categories = useCategories(group.id)
  const budgets = useBudgets(group.id)
  const [type, setType] = useState<CategoryType>('EXPENSE')
  const [mode, setMode] = useState<Mode>({ kind: 'list' })
  const { subscription } = me
  const limit = subscription.limits.categoriesPerType
  const free = subscription.tier === 'FREE'
  const ofType = categories.state.status === 'ready' ? categories.state.data.filter((category) => category.type === type) : []
  const toList = () => setMode({ kind: 'list' })
  const saveOrder = async (ids: string[]) => {
    const others = categories.state.status === 'ready' ? categories.state.data.filter((category) => category.type !== type).map((category) => category.id) : []
    try {
      await reorderCategories(group.id, [...ids, ...others])
      categories.reload()
    } catch (error) {
      notify({ title: serverMessage(error) ?? strings.saveFailed })
      categories.reload()
    }
  }

  const reorder = useReorder(ofType.map((category) => category.id), (ids) => void saveOrder(ids))

  const budgetOf = (categoryId: string) => (budgets.state.status === 'ready' ? budgets.state.data.find((budget) => budget.categoryId === categoryId) : undefined)

  const changed = (title: string, category: Category) => {
    categories.reload()
    budgets.reload()
    toList()
    notify({ title, detail: `${category.icon} ${category.name}` })
  }

  if (mode.kind === 'form') {
    return (
      <CategoryForm
        group={group}
        language={me.language}
        type={type}
        category={mode.category}
        onBack={toList}
        onSaved={(category, created) => {
          setType(category.type)
          changed(created ? strings.categoryCreatedToast : strings.categorySavedToast, category)
        }}
        onDeleted={(category) => changed(strings.categoryDeletedToast, category)}
        onLimit={() => setMode({ kind: 'limit' })}
        onBudgetLimit={() => setMode({ kind: 'budgetLimit' })}
      />
    )
  }
  if (mode.kind === 'budgetLimit') {
    const budgetLimit = subscription.limits.budgets ?? 0
    return (
      <LimitScreen
        header={strings.categoriesTitle}
        title={strings.budgetLimitTitle}
        body={strings.budgetLimitBody(budgetLimit)}
        meterLabel={strings.budgetLimitMeter}
        used={budgetLimit}
        limit={budgetLimit}
        subscribeUrl={subscription.subscribeUrl}
        backLabel={strings.backToCategories}
        onBack={toList}
      />
    )
  }
  if (mode.kind === 'limit') {
    return (
      <LimitScreen
        header={strings.categoriesTitle}
        title={strings.categoryLimitTitle}
        body={strings.categoryLimitBody(limit)}
        meterLabel={strings.categoryLimitMeter(type)}
        used={ofType.length}
        limit={limit}
        subscribeUrl={subscription.subscribeUrl}
        backLabel={strings.backToCategories}
        onBack={toList}
      />
    )
  }

  const add = () => setMode(free && ofType.length >= limit ? { kind: 'limit' } : { kind: 'form', category: null })

  return (
    <>
      <ScreenHeader
        title={strings.categoriesTitle}
        onBack={onBack}
        action={free && categories.state.status === 'ready' ? <span className="counter-chip">{strings.limitCounter(ofType.length, limit)}</span> : undefined}
      />
      <div {...tourTarget('categories-type')}>
        <Segmented label={strings.categoryTypeTabsLabel} options={CATEGORY_TYPES} value={type} optionKey={(option) => option} optionLabel={strings.typeFilter} onChange={setType} />
      </div>
      <ScreenTour hint="CATEGORIES" />
      {categories.state.status === 'loading' && <SkeletonRows count={SKELETON_ROWS} />}
      {categories.state.status === 'error' && <LoadError message={categories.state.message} onRetry={categories.reload} />}
      {categories.state.status === 'ready' && (ofType.length === 0
        ? <EmptyCard icon={<GridIcon />} title={strings.categoriesEmptyTitle(type)} body={strings.categoriesEmptyHint(type)} />
        : (
          <ul className="rows" {...tourTarget('categories-list')}>
            {reorder.order.map((id) => ofType.find((category) => category.id === id)).filter((category): category is Category => Boolean(category)).map((category) => {
              const budget = budgetOf(category.id)
              return (
                <li key={category.id} data-reorder-id={category.id} className={reorder.dragging === category.id ? 'reorder-dragging' : undefined}>
                  <span className="reorder-row">
                    <button type="button" className="row row-button pressable" onClick={() => setMode({ kind: 'form', category })}>
                      <span className="icon-circle" aria-hidden="true">{category.icon}</span>
                      <span className="row-main">
                        <span className="row-title">{category.name}</span>
                        {budget && <CategoryBudget budget={budget} currency={group.currency} language={me.language} />}
                      </span>
                      <ChevronRightIcon />
                    </button>
                    <button type="button" className="reorder-handle" aria-label={strings.moveCategory(category.name)} {...reorder.handle(category.id)}>
                      <GripIcon />
                    </button>
                  </span>
                </li>
              )
            })}
          </ul>
        ))}
      {categories.state.status === 'ready' && (
        <RememberedPlaces groupId={group.id} type={type} categories={categories.state.data} notify={notify} />
      )}
      {free && <p className="caption centered">{strings.categoriesFreeNote(limit)}</p>}
      <BottomBar>
        <Button {...tourTarget('categories-add')} onClick={add}>{strings.addCategory}</Button>
      </BottomBar>
    </>
  )
}

function CategoryBudget({ budget, currency, language }: { budget: Budget; currency: Currency; language: string }) {
  return (
    <span className="category-budget">
      <Meter percent={budget.percent} color={budgetColor(budget.percent)} />
      <span className="caption">{strings.budgetSpentOf(formatMoney(budget.spent, currency, language), formatMoney(budget.limit, currency, language))}</span>
    </span>
  )
}
