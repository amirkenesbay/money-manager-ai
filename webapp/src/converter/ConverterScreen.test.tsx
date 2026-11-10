import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Currency } from '../api/types'
import { formatMoney } from '../format'
import { fetchRate } from '../operations/operationsApi'
import { strings } from '../strings'
import { group } from '../test/fixtures'
import { asRendered } from '../test/text'
import { ConverterScreen } from './ConverterScreen'

vi.mock('../operations/operationsApi', () => ({ fetchRate: vi.fn() }))

const KZT_PER: Record<string, number> = { KZT: 1, USD: 484.3, EUR: 521.6, RUB: 4.95, TRY: 15.12 }

const renderConverter = (onRecord = vi.fn()) =>
  render(<ConverterScreen group={group()} language="ru" onBack={vi.fn()} onRecord={onRecord} />)

describe('ConverterScreen', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    localStorage.clear()
    vi.mocked(fetchRate).mockImplementation(async (from: Currency, to: Currency, date: string) => ({ from, to, date, rate: String(KZT_PER[from] / KZT_PER[to]) }))
  })

  it('shows a hundred dollars in tenge, euros and rubles', async () => {
    renderConverter()

    expect(await screen.findByText(asRendered(formatMoney('48430', 'KZT', 'ru')))).toBeInTheDocument()
    expect(screen.getByText(asRendered(formatMoney('92.85', 'EUR', 'ru')))).toBeInTheDocument()
    expect(screen.getByText(asRendered(formatMoney('9783.84', 'RUB', 'ru')))).toBeInTheDocument()
  })

  it('makes a tapped currency the one to convert from', async () => {
    renderConverter()
    await userEvent.click(await screen.findByRole('button', { name: new RegExp(strings.currencyName('KZT')) }))

    expect(screen.getByRole('button', { name: `${strings.currencyFieldLabel}: KZT` })).toBeInTheDocument()
    expect(screen.getByLabelText(strings.amountLabel)).toHaveTextContent(asRendered('48 430'))
  })

  it('adds a currency to the list', async () => {
    renderConverter()
    await userEvent.click(screen.getByRole('button', { name: strings.converterAdd }))
    await userEvent.click(within(screen.getByRole('dialog', { name: strings.currencyFieldLabel })).getByRole('radio', { name: `${strings.currencyName('TRY')} TRY` }))

    expect(await screen.findByText(asRendered(formatMoney('3203.04', 'TRY', 'ru')))).toBeInTheDocument()
  })

  it('records the amount as an expense in that currency', async () => {
    const onRecord = vi.fn()
    renderConverter(onRecord)

    await userEvent.click(screen.getByRole('button', { name: strings.recordAmount(formatMoney('100', 'USD', 'ru')) }))

    expect(onRecord).toHaveBeenCalledWith('USD', '100')
  })
})
