import type { Limits, Me, Price } from '../api/types'
import { formatDate, formatMoney } from '../format'
import { FULL_PERCENT } from '../percent'
import { strings } from '../strings'
import { openExternalLink } from '../telegram/telegramEnv'
import { Moment } from '../three/Moment'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { CrownIcon } from '../ui/icons'
import { ScreenHeader } from '../ui/ScreenHeader'
import './premium.css'

const DAYS_IN_MONTH = 30
const MONTHS_IN_YEAR = 12
const CRYSTAL_INPUT = {}

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

const monthlyOf = (prices: Price[]): Price | undefined => prices.find((price) => price.period === 'MONTH')

function yearlySaving(yearly: Price, monthly: Price | undefined): number | null {
  if (!monthly || yearly.period !== 'YEAR') return null
  const saving = Math.round((1 - Number(yearly.amount) / (Number(monthly.amount) * MONTHS_IN_YEAR)) * FULL_PERCENT)
  return saving > 0 ? saving : null
}

function PricePlans({ prices, language }: { prices: Price[]; language: string }) {
  const monthly = monthlyOf(prices)
  return (
    <ul className="premium-plans">
      {prices.map((price) => {
        const saving = yearlySaving(price, monthly)
        return (
          <li key={price.period} className="premium-plan">
            <span className="caption">{strings.planPeriod[price.period]}</span>
            <span className="amount premium-price-value">{formatMoney(price.amount, price.currency, language)}</span>
            {price.period === 'YEAR' && <span className="caption">{strings.planPerMonth(formatMoney((Number(price.amount) / MONTHS_IN_YEAR).toFixed(2), price.currency, language))}</span>}
            {saving !== null && <span className="premium-plan-saving">{strings.planSaving(saving)}</span>}
          </li>
        )
      })}
    </ul>
  )
}

export function PremiumScreen({ me, onBack }: { me: Me; onBack: () => void }) {
  const { subscription, language } = me
  const paid = subscription.tier === 'PAID'
  const forever = paid && subscription.expiresAt === null
  const monthly = monthlyOf(subscription.prices)
  const perDay = monthly && formatMoney((Number(monthly.amount) / DAYS_IN_MONTH).toFixed(2), monthly.currency, language)

  return (
    <>
      <ScreenHeader title={strings.premiumTitle} onBack={onBack} />
      <section className="premium-hero">
        <Moment
          kind="crystal"
          input={CRYSTAL_INPUT}
          className="premium-crystal"
          fallback={<span className="premium-crown" aria-hidden="true"><span><CrownIcon /></span></span>}
        />
        <h2 className="premium-title">{paid ? strings.premiumActiveTitle : strings.premiumTitle}</h2>
        <p className="hint centered">
          {forever
            ? strings.premiumForeverBody
            : paid && subscription.expiresAt ? strings.premiumActiveBody(formatDate(subscription.expiresAt, language)) : strings.premiumTagline}
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
          <PricePlans prices={subscription.prices} language={language} />
          {perDay && <p className="caption">{strings.perDay(perDay)}</p>}
        </section>
      )}
      {!forever && (
        <>
          <p className="caption centered">{strings.tributeNote}</p>
          <BottomBar>
            <Button onClick={() => openExternalLink(subscription.subscribeUrl)}>{paid ? strings.extendPremium : strings.subscribePremium}</Button>
          </BottomBar>
        </>
      )}
    </>
  )
}
