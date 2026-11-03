import { useCallback } from 'react'
import { useAsync } from '../api/useAsync'
import { fetchBudgets } from './budgetsApi'

export function useBudgets(groupId: string) {
  const load = useCallback((signal: AbortSignal) => fetchBudgets(groupId, signal), [groupId])
  return useAsync(load)
}
