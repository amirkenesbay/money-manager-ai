import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchBalance } from '../dashboard/dashboardApi'
import { formatMoney } from '../format'
import { fetchRate } from '../operations/operationsApi'
import { strings } from '../strings'
import { balance, group } from '../test/fixtures'
import { asRendered } from '../test/text'
import { CurrencySheet } from './CurrencySheet'
import { updateGroup } from './groupsApi'

vi.mock('./groupsApi', () => ({ updateGroup: vi.fn() }))
vi.mock('../dashboard/dashboardApi', () => ({ fetchBalance: vi.fn() }))
vi.mock('../operations/operationsApi', () => ({ fetchRate: vi.fn() }))

const renderSheet = (onSaved = vi.fn()) =>
  render(<CurrencySheet group={group()} language="ru" onSaved={onSaved} onClose={vi.fn()} />)

const pickDollar = async () => {
  await userEvent.click(screen.getByRole('radio', { name: `${strings.currencyName('USD')} USD` }))
  await userEvent.click(screen.getByRole('button', { name: strings.done }))
}

describe('CurrencySheet', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchBalance).mockResolvedValue(balance({ total: '9686' }))
    vi.mocked(fetchRate).mockResolvedValue({ from: 'KZT', to: 'USD', rate: '0.002065', date: '2026-11-10' })
  })

  it('shows what the balance becomes before switching', async () => {
    renderSheet()
    await pickDollar()

    expect(await screen.findByText(asRendered(formatMoney('20', 'USD', 'ru')))).toBeInTheDocument()
    expect(screen.getByText(asRendered('1 $ = 484,26 ₸'))).toBeInTheDocument()
    expect(updateGroup).not.toHaveBeenCalled()
  })

  it('switches and offers to go back', async () => {
    const switched = group({ currency: 'USD' })
    vi.mocked(updateGroup).mockResolvedValueOnce(switched).mockResolvedValueOnce(group())
    const onSaved = vi.fn()
    renderSheet(onSaved)
    await pickDollar()

    await userEvent.click(await screen.findByRole('button', { name: strings.currencySwitchTo('$') }))

    expect(updateGroup).toHaveBeenCalledWith(group().id, { currency: 'USD' })
    const undo = onSaved.mock.calls[0][1]
    expect(undo.label).toBe(strings.currencyUndo('₸'))
    undo.onClick()
    expect(updateGroup).toHaveBeenLastCalledWith(switched.id, { currency: 'KZT' })
  })

  it('keeps the currency when the owner changes their mind', async () => {
    renderSheet()
    await pickDollar()

    await userEvent.click(await screen.findByRole('button', { name: strings.currencyKeep('₸') }))

    expect(screen.getByRole('radio', { name: `${strings.currencyName('USD')} USD` })).toBeInTheDocument()
    expect(updateGroup).not.toHaveBeenCalled()
  })
})
