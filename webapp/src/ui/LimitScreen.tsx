import { percentOf } from '../percent'
import { strings } from '../strings'
import { openExternalLink } from '../telegram/telegramEnv'
import { Button } from './Button'
import { Meter } from './Meter'
import { ScreenHeader } from './ScreenHeader'
import { StateView } from './StateView'

const LIMIT_ICON = '💎'

export function LimitScreen({ header, title, body, meterLabel, used, limit, subscribeUrl, backLabel, onBack }: {
  header: string
  title: string
  body: string
  meterLabel: string
  used: number
  limit: number
  subscribeUrl: string
  backLabel: string
  onBack: () => void
}) {
  return (
    <>
      <ScreenHeader title={header} onBack={onBack} />
      <StateView icon={LIMIT_ICON} title={title} body={body}>
        <div className="limit-meter">
          <span className="limit-meter-head">
            <span className="caption">{meterLabel}</span>
            <span className="amount">{strings.limitCounter(used, limit)}</span>
          </span>
          <Meter percent={percentOf(used, limit)} />
        </div>
        <Button onClick={() => openExternalLink(subscribeUrl)}>{strings.getPremium}</Button>
        <Button variant="secondary" onClick={onBack}>{backLabel}</Button>
      </StateView>
    </>
  )
}
