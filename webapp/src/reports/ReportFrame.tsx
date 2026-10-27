import type { ReactNode } from 'react'
import type { AsyncState } from '../api/useAsync'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { LoadError } from '../ui/LoadError'
import { PremiumOffer } from '../ui/PremiumOffer'
import { ReportSkeleton } from './ReportSkeleton'
import type { ReportContext } from './ReportsScreen'

export function ReportFrame<T>({ report, context, children }: {
  report: { state: AsyncState<T>; reload: () => void }
  context: ReportContext
  children: (data: T) => ReactNode
}) {
  const { state } = report
  if (state.status === 'loading') return <ReportSkeleton />
  if (state.status === 'ready') return children(state.data)
  if (state.code !== 'PREMIUM_REQUIRED') return <LoadError message={state.message} onRetry={report.reload} />
  const { limits, subscribeUrl } = context.me.subscription
  return (
    <div className="report-premium">
      <PremiumOffer historyDaysBack={limits.historyDaysBack} subscribeUrl={subscribeUrl} />
      <Button variant="secondary" size="small" onClick={context.onBackToCurrentMonth}>{strings.backToCurrentMonth}</Button>
    </div>
  )
}
