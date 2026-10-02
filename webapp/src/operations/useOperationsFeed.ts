import { useCallback, useState } from 'react'
import type { OperationsPage } from '../api/types'
import { useAsync } from '../api/useAsync'
import { fetchOperations, type OperationsQuery } from './operationsApi'

interface ExtraPages {
  base: OperationsPage
  pages: OperationsPage[]
  loading: boolean
  failed: boolean
}

export function useOperationsFeed(groupId: string, query: OperationsQuery) {
  const loadFirstPage = useCallback((signal: AbortSignal) => fetchOperations(groupId, query, 0, signal), [groupId, query])
  const firstPage = useAsync(loadFirstPage)
  const [extra, setExtra] = useState<ExtraPages | null>(null)

  const base = firstPage.state.status === 'ready' ? firstPage.state.data : null
  const current = extra !== null && extra.base === base ? extra : null
  const items = base === null ? [] : [...base.items, ...(current?.pages.flatMap((page) => page.items) ?? [])]
  const hasMore = base !== null && items.length < base.totalItems

  const loadMore = useCallback(async () => {
    if (base === null) return
    const pages = current?.pages ?? []
    setExtra({ base, pages, loading: true, failed: false })
    try {
      const next = await fetchOperations(groupId, query, pages.length + 1)
      setExtra({ base, pages: [...pages, next], loading: false, failed: false })
    } catch {
      setExtra({ base, pages, loading: false, failed: true })
    }
  }, [base, current, groupId, query])

  return {
    state: firstPage.state,
    reload: firstPage.reload,
    items,
    hasMore,
    loadingMore: current?.loading ?? false,
    loadMoreFailed: current?.failed ?? false,
    loadMore,
  }
}
