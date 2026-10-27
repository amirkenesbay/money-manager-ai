import { apiRequest } from '../api/client'
import type { Balance, MonthSummary, OperationsPage } from '../api/types'
import { fetchOperations, NO_FILTERS } from '../operations/operationsApi'

export const RECENT_OPERATIONS_LIMIT = 3

export const fetchBalance = (groupId: string, signal?: AbortSignal): Promise<Balance> =>
  apiRequest<Balance>(`/groups/${groupId}/balance`, {}, signal)

export const fetchMonthSummary = (groupId: string, signal?: AbortSignal): Promise<MonthSummary> =>
  apiRequest<MonthSummary>(`/groups/${groupId}/summary`, {}, signal)

export const fetchRecentOperations = (groupId: string, signal?: AbortSignal): Promise<OperationsPage> =>
  fetchOperations(groupId, NO_FILTERS, 0, signal, RECENT_OPERATIONS_LIMIT)
