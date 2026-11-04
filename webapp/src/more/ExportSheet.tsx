import { useState } from 'react'
import type { Group, Me } from '../api/types'
import { type Period, periodRange } from '../operations/periods'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { PremiumOffer } from '../ui/PremiumOffer'
import { Segmented } from '../ui/Segmented'
import { Sheet } from '../ui/Sheet'
import { useSubmit } from '../ui/useSubmit'
import { type ExportFormat, exportOperations } from './exportApi'

const EXPORT_PERIODS: Period[] = ['thisMonth', 'lastMonth', 'last3Months', 'allTime']
const EXPORT_FORMATS: ExportFormat[] = ['XLSX', 'CSV']

export function ExportSheet({ me, group, onSent, onClose }: {
  me: Me
  group: Group
  onSent: (count: number) => void
  onClose: () => void
}) {
  const [period, setPeriod] = useState<Period>('thisMonth')
  const [format, setFormat] = useState<ExportFormat>('XLSX')
  const [empty, setEmpty] = useState(false)
  const submit = useSubmit()
  const premiumNeeded = submit.error?.code === 'PREMIUM_REQUIRED'

  const send = async () => {
    setEmpty(false)
    const result = await submit.run(() => exportOperations(group.id, periodRange(period, new Date(), group.monthStartDay), format), strings.exportFailed)
    if (!result.ok) return
    if (result.value.count === 0) setEmpty(true)
    else onSent(result.value.count)
  }

  return (
    <Sheet label={strings.exportTitle} onClose={onClose}>
      <h2 className="sheet-title">{strings.exportTitle}</h2>
      <p className="hint">{strings.exportHint}</p>
      <Segmented variant="choices" label={strings.periodFilterLabel} options={EXPORT_PERIODS} value={period} optionKey={(option) => option} optionLabel={strings.period} onChange={setPeriod} />
      <Segmented label={strings.exportFormatLabel} options={EXPORT_FORMATS} value={format} optionKey={(option) => option} optionLabel={(option) => strings.exportFormats[option]} onChange={setFormat} />
      {empty && <p className="hint warning" role="status">{strings.exportEmpty}</p>}
      {premiumNeeded && <PremiumOffer historyDaysBack={me.subscription.limits.historyDaysBack} subscribeUrl={me.subscription.subscribeUrl} />}
      {submit.error && !premiumNeeded && <p className="error-text" role="alert">{submit.error.message}</p>}
      <Button disabled={submit.pending} onClick={send}>{submit.pending ? strings.exportSending : strings.exportSend}</Button>
    </Sheet>
  )
}
