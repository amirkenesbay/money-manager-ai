import { apiRequest } from '../api/client'
import type { Budget } from '../api/types'

export const fetchBudgets = (groupId: string, signal?: AbortSignal): Promise<Budget[]> =>
  apiRequest<Budget[]>(`/groups/${groupId}/budgets`, {}, signal)
