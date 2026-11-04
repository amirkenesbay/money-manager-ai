import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { strings } from '../strings'
import { type HomeScreenState, useHomeScreen } from '../telegram/homeScreen'
import { HomeScreenOffer } from './HomeScreenOffer'

vi.mock('../telegram/homeScreen', () => ({ useHomeScreen: vi.fn() }))

const add = vi.fn()

const renderOffer = (state: HomeScreenState = 'available') => {
  vi.mocked(useHomeScreen).mockReturnValue({ state, add })
  return render(<HomeScreenOffer />)
}

describe('HomeScreenOffer', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('asks Telegram to add the shortcut', async () => {
    renderOffer()

    await userEvent.click(screen.getByRole('button', { name: strings.homeScreenAdd }))

    expect(add).toHaveBeenCalled()
  })

  it('hides until the next launch when postponed', async () => {
    renderOffer()

    await userEvent.click(screen.getByRole('button', { name: strings.homeScreenLater }))

    expect(screen.queryByText(strings.homeScreenOfferTitle)).not.toBeInTheDocument()
    expect(add).not.toHaveBeenCalled()
  })

  it('stays hidden once added or where Telegram cannot add it', () => {
    renderOffer('added').unmount()
    renderOffer('unsupported')

    expect(screen.queryByText(strings.homeScreenOfferTitle)).not.toBeInTheDocument()
  })
})
