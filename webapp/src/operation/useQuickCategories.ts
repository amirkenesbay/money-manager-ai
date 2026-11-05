import { useCallback } from 'react'
import type { Category } from '../api/types'
import { useAsync } from '../api/useAsync'
import { fetchCategories } from '../categories/categoriesApi'
import { fetchOperations, NO_FILTERS } from '../operations/operationsApi'

const QUICK_COUNT = 4
const RECENT_OPERATIONS = 50

export function useQuickCategories(groupId: string): Category[] {
  const load = useCallback(async (signal: AbortSignal) => {
    const [categories, recent] = await Promise.all([
      fetchCategories(groupId, signal),
      fetchOperations(groupId, { ...NO_FILTERS, type: 'EXPENSE' }, 0, signal, RECENT_OPERATIONS),
    ])
    const uses = new Map<string, number>()
    recent.items.forEach((operation) => uses.set(operation.categoryId, (uses.get(operation.categoryId) ?? 0) + 1))
    return categories
      .filter((category) => category.type === 'EXPENSE')
      .sort((left, right) => (uses.get(right.id) ?? 0) - (uses.get(left.id) ?? 0))
      .slice(0, QUICK_COUNT)
  }, [groupId])
  const { state } = useAsync(load)
  return state.status === 'ready' ? state.data : []
}
