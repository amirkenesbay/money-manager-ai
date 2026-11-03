import { useCallback } from 'react'
import type { Category, CategoryType } from '../api/types'
import { useAsync } from '../api/useAsync'
import { strings } from '../strings'
import { TrashIcon } from '../ui/icons'
import type { Notify } from '../ui/useToast'
import './categories.css'
import { fetchCategoryRules, forgetCategoryRule } from './categoriesApi'

export function RememberedPlaces({ groupId, type, categories, notify }: {
  groupId: string
  type: CategoryType
  categories: Category[]
  notify: Notify
}) {
  const load = useCallback((signal: AbortSignal) => fetchCategoryRules(groupId, signal), [groupId])
  const rules = useAsync(load)
  if (rules.state.status !== 'ready') return null
  const ofType = rules.state.data.filter((rule) => categories.some((category) => category.id === rule.categoryId && category.type === type))
  if (ofType.length === 0) return null

  const forget = async (ruleId: string) => {
    try {
      await forgetCategoryRule(groupId, ruleId)
      rules.reload()
      notify({ title: strings.placeForgottenToast })
    } catch {
      notify({ title: strings.deleteFailed })
    }
  }

  return (
    <section className="remembered" aria-label={strings.rememberedPlacesTitle}>
      <h2 className="section-title">{strings.rememberedPlacesTitle}</h2>
      <p className="caption">{strings.rememberedPlacesHint}</p>
      <ul className="remembered-list">
        {ofType.map((rule) => (
          <li key={rule.id} className="remembered-row">
            <span className="row-main">
              <span className="row-title">{strings.rememberedPlace(rule.place)}</span>
              <span className="caption">{rule.categoryIcon} {rule.categoryName}</span>
            </span>
            <button type="button" className="round-button pressable" aria-label={strings.forgetPlace(rule.place)} onClick={() => void forget(rule.id)}>
              <TrashIcon />
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
