import { useCallback } from 'react'
import { useAsync } from '../api/useAsync'
import { fetchCategories } from './categoriesApi'

export function useCategories(groupId: string) {
  const load = useCallback((signal: AbortSignal) => fetchCategories(groupId, signal), [groupId])
  return useAsync(load)
}
