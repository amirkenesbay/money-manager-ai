import { useCallback } from 'react'
import { useAsync } from '../api/useAsync'
import { fetchDebts } from './debtsApi'

export function useDebts(groupId: string) {
  const load = useCallback((signal: AbortSignal) => fetchDebts(groupId, signal), [groupId])
  return useAsync(load)
}
