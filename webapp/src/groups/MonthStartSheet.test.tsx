import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { strings } from '../strings'
import { group } from '../test/fixtures'
import { updateGroup } from './groupsApi'
import { MonthStartSheet } from './MonthStartSheet'

vi.mock('./groupsApi', () => ({ updateGroup: vi.fn() }))

const SALARY_DAY = 25

describe('MonthStartSheet', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('lets the owner start the month on payday', async () => {
    const saved = group({ monthStartDay: SALARY_DAY })
    vi.mocked(updateGroup).mockResolvedValue(saved)
    const onSaved = vi.fn()
    render(<MonthStartSheet group={group()} onSaved={onSaved} onClose={vi.fn()} />)

    await userEvent.click(screen.getByRole('radio', { name: String(SALARY_DAY) }))
    await userEvent.click(screen.getByRole('button', { name: strings.done }))

    expect(updateGroup).toHaveBeenCalledWith(group().id, { monthStartDay: SALARY_DAY })
    expect(onSaved).toHaveBeenCalledWith(saved)
  })

  it('explains to a member that only the owner can change it', async () => {
    render(<MonthStartSheet group={group({ isOwner: false })} onSaved={vi.fn()} onClose={vi.fn()} />)

    await userEvent.click(screen.getByRole('radio', { name: String(SALARY_DAY) }))

    expect(screen.getByRole('radio', { name: '1' })).toHaveAttribute('aria-checked', 'true')
    expect(screen.getByText(strings.monthStartNonOwnerHint)).toBeInTheDocument()
    expect(updateGroup).not.toHaveBeenCalled()
  })
})
