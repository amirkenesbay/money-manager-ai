import { useCallback } from 'react'
import type { AskSuggestions } from '../api/types'
import { useAsync } from '../api/useAsync'
import { fetchAskSuggestions } from './askApi'

export function useAskSuggestions(groupId: string): AskSuggestions | null {
  const load = useCallback((signal: AbortSignal) => fetchAskSuggestions(groupId, signal), [groupId])
  const { state } = useAsync(load)
  return state.status === 'ready' ? state.data : null
}
