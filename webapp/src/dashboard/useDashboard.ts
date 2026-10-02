import { useCallback } from 'react'
import type { Balance, MonthSummary, OperationsPage } from '../api/types'
import { useAsync } from '../api/useAsync'
import { fetchBalance, fetchMonthSummary, fetchRecentOperations } from './dashboardApi'

export interface DashboardData {
  balance: Balance
  summary: MonthSummary
  operations: OperationsPage
}

export function useDashboard(groupId: string) {
  const load = useCallback(async (signal: AbortSignal): Promise<DashboardData> => {
    const [balance, summary, operations] = await Promise.all([
      fetchBalance(groupId, signal),
      fetchMonthSummary(groupId, signal),
      fetchRecentOperations(groupId, signal),
    ])
    return { balance, summary, operations }
  }, [groupId])
  return useAsync(load)
}
