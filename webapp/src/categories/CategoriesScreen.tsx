import { useState } from 'react'
import type { Category, CategoryType, Group, Me } from '../api/types'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { EmptyCard } from '../ui/EmptyCard'
import { ChevronRightIcon, GridIcon } from '../ui/icons'
import { LimitScreen } from '../ui/LimitScreen'
import { LoadError } from '../ui/LoadError'
import { ScreenHeader } from '../ui/ScreenHeader'
import { Segmented } from '../ui/Segmented'
import { SkeletonRows } from '../ui/Skeleton'
import type { Notify } from '../ui/useToast'
import { CategoryForm } from './CategoryForm'
import { CATEGORY_TYPES } from './categoryTypes'
import { useCategories } from './useCategories'

const SKELETON_ROWS = 5

type Mode = { kind: 'list' } | { kind: 'form'; category: Category | null } | { kind: 'limit' }

export function CategoriesScreen({ me, group, notify, onBack }: {
  me: Me
  group: Group
  notify: Notify
  onBack: () => void
}) {
  const categories = useCategories(group.id)
  const [type, setType] = useState<CategoryType>('EXPENSE')
  const [mode, setMode] = useState<Mode>({ kind: 'list' })
  const { subscription } = me
  const limit = subscription.limits.categoriesPerType
  const free = subscription.tier === 'FREE'
  const ofType = categories.state.status === 'ready' ? categories.state.data.filter((category) => category.type === type) : []
  const toList = () => setMode({ kind: 'list' })

  const changed = (title: string, category: Category) => {
    categories.reload()
    toList()
    notify({ title, detail: `${category.icon} ${category.name}` })
  }

  if (mode.kind === 'form') {
    return (
      <CategoryForm
        group={group}
        type={type}
        category={mode.category}
        onBack={toList}
        onSaved={(category, created) => {
          setType(category.type)
          changed(created ? strings.categoryCreatedToast : strings.categorySavedToast, category)
        }}
        onDeleted={(category) => changed(strings.categoryDeletedToast, category)}
        onLimit={() => setMode({ kind: 'limit' })}
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
      <Segmented label={strings.categoryTypeTabsLabel} options={CATEGORY_TYPES} value={type} optionKey={(option) => option} optionLabel={strings.typeFilter} onChange={setType} />
      {categories.state.status === 'loading' && <SkeletonRows count={SKELETON_ROWS} />}
      {categories.state.status === 'error' && <LoadError message={categories.state.message} onRetry={categories.reload} />}
      {categories.state.status === 'ready' && (ofType.length === 0
        ? <EmptyCard icon={<GridIcon />} title={strings.categoriesEmptyTitle(type)} body={strings.categoriesEmptyHint(type)} />
        : (
          <ul className="rows">
            {ofType.map((category) => (
              <li key={category.id}>
                <button type="button" className="row row-button pressable" onClick={() => setMode({ kind: 'form', category })}>
                  <span className="icon-circle" aria-hidden="true">{category.icon}</span>
                  <span className="row-main"><span className="row-title">{category.name}</span></span>
                  <ChevronRightIcon />
                </button>
              </li>
            ))}
          </ul>
        ))}
      {free && <p className="caption centered">{strings.categoriesFreeNote(limit)}</p>}
      <BottomBar>
        <Button onClick={add}>{strings.addCategory}</Button>
      </BottomBar>
    </>
  )
}
