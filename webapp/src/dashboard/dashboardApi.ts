import { apiRequest } from '../api/client'
import type { Balance, Group, MonthSummary, OperationsPage } from '../api/types'

export const RECENT_OPERATIONS_LIMIT = 10

export const fetchGroups = (signal?: AbortSignal): Promise<Group[]> => apiRequest<Group[]>('/groups', {}, signal)

export const activateGroup = (groupId: string): Promise<void> =>
  apiRequest<void>(`/groups/${groupId}/activate`, { method: 'POST' })

export const fetchBalance = (groupId: string, signal?: AbortSignal): Promise<Balance> =>
  apiRequest<Balance>(`/groups/${groupId}/balance`, {}, signal)

export const fetchMonthSummary = (groupId: string, signal?: AbortSignal): Promise<MonthSummary> =>
  apiRequest<MonthSummary>(`/groups/${groupId}/summary`, {}, signal)

export const fetchRecentOperations = (groupId: string, signal?: AbortSignal): Promise<OperationsPage> =>
  apiRequest<OperationsPage>(`/groups/${groupId}/operations?size=${RECENT_OPERATIONS_LIMIT}`, {}, signal)
