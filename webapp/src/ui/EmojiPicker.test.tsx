import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { strings } from '../strings'
import { EmojiPicker } from './EmojiPicker'

const PRESETS = ['🛒', '🍔', '📦']

describe('EmojiPicker', () => {
  it('puts the icon suggested by the name first', () => {
    render(<EmojiPicker value="☕" presets={PRESETS} suggestion="☕" onChange={vi.fn()} />)

    expect(screen.getAllByRole('button', { pressed: true })[0]).toHaveAccessibleName('☕')
  })

  it('finds an icon in the library by a word', async () => {
    const onChange = vi.fn()
    render(<EmojiPicker value="🛒" presets={PRESETS} onChange={onChange} />)

    await userEvent.click(screen.getByRole('button', { name: strings.iconMore }))
    await userEvent.type(screen.getByRole('searchbox', { name: strings.iconSearchLabel }), 'такси')
    await userEvent.click(screen.getByRole('button', { name: '🚕' }))

    expect(onChange).toHaveBeenCalledWith('🚕')
  })

  it('says when the search finds nothing', async () => {
    render(<EmojiPicker value="🛒" presets={PRESETS} onChange={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: strings.iconMore }))
    await userEvent.type(screen.getByRole('searchbox', { name: strings.iconSearchLabel }), 'zzzz')

    expect(screen.getByText(strings.iconSearchEmpty)).toBeInTheDocument()
  })
})
