import type { Limits, Me } from '../api/types'
import { formatDate, formatMoney } from '../format'
import { strings } from '../strings'
import { openExternalLink } from '../telegram/telegramEnv'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { CrownIcon } from '../ui/icons'
import { ScreenHeader } from '../ui/ScreenHeader'
import './premium.css'

const DAYS_IN_MONTH = 30

interface LimitRow {
  label: () => string
  value: (limits: Limits, language: string) => string
}

const countOrUnlimited = (count: number | null, language: string): string =>
  count === null ? strings.unlimited : new Intl.NumberFormat(language).format(count)

const LIMIT_ROWS: LimitRow[] = [
  { label: () => strings.premiumAiRow, value: (limits, language) => countOrUnlimited(limits.aiRequestsPerDay, language) },
  { label: () => strings.premiumHistoryRow, value: (limits) => (limits.historyDaysBack === null ? strings.wholeHistory : strings.historyDays(limits.historyDaysBack)) },
  { label: () => strings.premiumCategoriesRow, value: (limits, language) => countOrUnlimited(limits.categoriesPerType, language) },
  { label: () => strings.premiumGroupsRow, value: (limits, language) => countOrUnlimited(limits.ownedSharedGroups, language) },
  { label: () => strings.premiumRemindersRow, value: (limits, language) => countOrUnlimited(limits.activeNotifications, language) },
]

export function PremiumScreen({ me, onBack }: { me: Me; onBack: () => void }) {
  const { subscription, language } = me
  const paid = subscription.tier === 'PAID'
  const price = formatMoney(subscription.price.amount, subscription.price.currency, language)
  const perDay = formatMoney(String(Math.round(Number(subscription.price.amount) / DAYS_IN_MONTH)), subscription.price.currency, language)

  return (
    <>
      <ScreenHeader title={strings.premiumTitle} onBack={onBack} />
      <section className="premium-hero">
        <span className="premium-crown" aria-hidden="true"><span><CrownIcon /></span></span>
        <h2 className="premium-title">{paid ? strings.premiumActiveTitle : strings.premiumTitle}</h2>
        <p className="hint centered">
          {paid && subscription.expiresAt ? strings.premiumActiveBody(formatDate(subscription.expiresAt, language)) : strings.premiumTagline}
        </p>
      </section>
      <table className="premium-table">
        <thead>
          <tr>
            <th scope="col"><span className="visually-hidden">{strings.premiumTitle}</span></th>
            <th scope="col">{strings.freeColumn}</th>
            <th scope="col" className="premium-column">{strings.premiumColumn}</th>
          </tr>
        </thead>
        <tbody>
          {LIMIT_ROWS.map((row) => (
            <tr key={row.label()}>
              <th scope="row">{row.label()}</th>
              <td>{row.value(subscription.freeLimits, language)}</td>
              <td className="premium-column">{row.value(subscription.paidLimits, language)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!paid && (
        <section className="premium-price">
          <p><span className="amount premium-price-value">{price}</span> <span className="hint">{strings.perMonth}</span></p>
          <p className="caption">{strings.perDay(perDay)}</p>
        </section>
      )}
      <p className="caption centered">{strings.tributeNote}</p>
      <BottomBar>
        <Button onClick={() => openExternalLink(subscription.subscribeUrl)}>{paid ? strings.extendPremium : strings.subscribeFor(price)}</Button>
      </BottomBar>
    </>
  )
}
