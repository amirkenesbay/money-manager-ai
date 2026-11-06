import { useCallback, useMemo, useState } from 'react'
import type { Category } from '../api/types'
import { type AsyncState, useAsync } from '../api/useAsync'
import { fetchCategories } from './categoriesApi'

export function useCategories(groupId: string) {
  const load = useCallback((signal: AbortSignal) => fetchCategories(groupId, signal), [groupId])
  const loaded = useAsync(load)
  const [created, setCreated] = useState<{ groupId: string; categories: Category[] }>({ groupId, categories: [] })

  const state = useMemo((): AsyncState<Category[]> => {
    if (loaded.state.status !== 'ready' || created.groupId !== groupId) return loaded.state
    const known = new Set(loaded.state.data.map((category) => category.id))
    return { status: 'ready', data: [...loaded.state.data, ...created.categories.filter((category) => !known.has(category.id))] }
  }, [loaded.state, created, groupId])

  const add = useCallback((category: Category) => {
    setCreated((previous) => ({ groupId, categories: previous.groupId === groupId ? [...previous.categories, category] : [category] }))
  }, [groupId])

  return { state, reload: loaded.reload, add }
}
