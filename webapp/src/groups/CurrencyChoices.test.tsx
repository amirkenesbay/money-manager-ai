import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { CURRENCIES } from '../currencies'
import { strings } from '../strings'
import { CurrencyChoices } from './CurrencyChoices'

describe('CurrencyChoices', () => {
  it('lists every currency and marks the current one', () => {
    render(<CurrencyChoices value="KZT" onChange={vi.fn()} />)

    expect(screen.getAllByRole('radio')).toHaveLength(CURRENCIES.length)
    expect(screen.getByRole('radio', { checked: true })).toHaveAccessibleName(`${strings.currencyName('KZT')} KZT`)
  })

  it('finds a currency by its name or code', async () => {
    const onChange = vi.fn()
    render(<CurrencyChoices value="KZT" onChange={onChange} />)

    await userEvent.type(screen.getByRole('searchbox', { name: strings.currencySearchLabel }), 'белорус')
    await userEvent.click(screen.getByRole('radio'))
    expect(onChange).toHaveBeenCalledWith('BYN')

    await userEvent.clear(screen.getByRole('searchbox', { name: strings.currencySearchLabel }))
    await userEvent.type(screen.getByRole('searchbox', { name: strings.currencySearchLabel }), 'gbp')
    expect(screen.getByRole('radio')).toHaveAccessibleName(`${strings.currencyName('GBP')} GBP`)
  })

  it('says when nothing matches', async () => {
    render(<CurrencyChoices value="KZT" onChange={vi.fn()} />)

    await userEvent.type(screen.getByRole('searchbox', { name: strings.currencySearchLabel }), 'zzzz')

    expect(screen.getByText(strings.iconSearchEmpty)).toBeInTheDocument()
  })
})
