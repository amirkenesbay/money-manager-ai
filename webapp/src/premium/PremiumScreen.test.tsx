import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { formatDate } from '../format'
import { strings } from '../strings'
import { me } from '../test/fixtures'
import { PremiumScreen } from './PremiumScreen'

const LANGUAGE = 'ru'
const base = me()
const withSubscription = (changes: Partial<typeof base.subscription>) => me({ subscription: { ...base.subscription, ...changes } })

describe('PremiumScreen', () => {
  it('offers a month and a year and shows what the year saves', () => {
    render(<PremiumScreen me={base} onBack={vi.fn()} />)

    expect(screen.getByText(strings.planPeriod.MONTH)).toBeInTheDocument()
    expect(screen.getByText(strings.planPeriod.YEAR)).toBeInTheDocument()
    expect(screen.getByText(strings.planSaving(6))).toBeInTheDocument()
    expect(screen.getByRole('button', { name: strings.subscribePremium })).toBeInTheDocument()
  })

  it('shows until when a paid premium works and offers to extend it', () => {
    const expiresAt = '2027-11-07T10:00:00Z'
    render(<PremiumScreen me={withSubscription({ tier: 'PAID', expiresAt })} onBack={vi.fn()} />)

    expect(screen.getByText(strings.premiumActiveBody(formatDate(expiresAt, LANGUAGE)))).toBeInTheDocument()
    expect(screen.getByRole('button', { name: strings.extendPremium })).toBeInTheDocument()
    expect(screen.queryByText(strings.planPeriod.YEAR)).not.toBeInTheDocument()
  })

  it('calls a whitelisted premium forever and asks for nothing', () => {
    render(<PremiumScreen me={withSubscription({ tier: 'PAID', expiresAt: null })} onBack={vi.fn()} />)

    expect(screen.getByText(strings.premiumForeverBody)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: strings.extendPremium })).not.toBeInTheDocument()
  })
})
