import { strings } from '../strings'
import { openExternalLink } from '../telegram/telegramEnv'
import { ChevronRightIcon, LockIcon } from './icons'

export function PremiumOffer({ historyDaysBack, subscribeUrl }: { historyDaysBack: number | null; subscribeUrl: string }) {
  return (
    <button type="button" className="premium-offer pressable" onClick={() => openExternalLink(subscribeUrl)}>
      <span className="income"><LockIcon /></span>
      <span className="row-main">
        <span className="row-title">{strings.premiumHistoryTitle(historyDaysBack)}</span>
        <span className="caption">{strings.premiumHistoryHint}</span>
      </span>
      <ChevronRightIcon />
    </button>
  )
}
