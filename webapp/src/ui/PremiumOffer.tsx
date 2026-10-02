import { strings } from '../strings'
import { openExternalLink } from '../telegram/telegramEnv'

export function PremiumOffer({ historyDaysBack, subscribeUrl }: { historyDaysBack: number | null; subscribeUrl: string }) {
  return (
    <section className="card premium-offer">
      <h2>{strings.premiumTitle}</h2>
      <p className="hint">{strings.premiumHistoryLimit(historyDaysBack)}</p>
      <button type="button" onClick={() => openExternalLink(subscribeUrl)}>{strings.premiumSubscribe}</button>
    </section>
  )
}
