import { useCallback } from 'react'
import type { Member } from '../api/types'
import { useAsync } from '../api/useAsync'
import { fetchMembers } from './groupsApi'

export function useMembers(groupId: string) {
  const load = useCallback((signal: AbortSignal) => fetchMembers(groupId, signal), [groupId])
  return useAsync<Member[]>(load)
}
