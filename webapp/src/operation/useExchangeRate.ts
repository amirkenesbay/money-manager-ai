import { useCallback } from 'react'
import type { Currency, ExchangeRate } from '../api/types'
import { useAsync } from '../api/useAsync'
import { fetchRate } from '../operations/operationsApi'

export function useExchangeRate(from: Currency, to: Currency, date: string, needed: boolean) {
  const load = useCallback(
    (signal: AbortSignal): Promise<ExchangeRate | null> => (needed && from !== to ? fetchRate(from, to, date, signal) : Promise.resolve(null)),
    [from, to, date, needed],
  )
  return useAsync(load).state
}
