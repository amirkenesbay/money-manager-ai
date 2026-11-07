import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import { strings } from '../strings'
import { me, reminder } from '../test/fixtures'
import { createReminder, fetchReminders, setReminderActive } from './remindersApi'
import { RemindersScreen } from './RemindersScreen'

vi.mock('./remindersApi', () => ({
  fetchReminders: vi.fn(),
  createReminder: vi.fn(),
  replaceReminder: vi.fn(),
  setReminderActive: vi.fn(),
  deleteReminder: vi.fn(),
}))
vi.mock('../telegram/telegramEnv', () => ({ openExternalLink: vi.fn(), rawInitData: () => undefined }))

const TIMEZONE = 'Asia/Almaty'
const notify = vi.fn()
const evening = reminder()
const weekly = reminder({ id: 'rem-2', name: 'Итоги недели', isActive: false, frequencyType: 'WEEKLY', dayOfWeek: 'SUNDAY', hour: 20 })
const renderScreen = (timezone: string | null = TIMEZONE) =>
  render(<RemindersScreen me={me({ timezone })} notify={notify} onBack={vi.fn()} />)

describe('RemindersScreen', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchReminders).mockResolvedValue([evening, weekly])
  })

  it('lists reminders with their schedule and the active counter', async () => {
    renderScreen()

    expect(await screen.findByText('каждый день · 21:00')).toBeInTheDocument()
    expect(screen.getByText('каждую неделю · вс · 20:00')).toBeInTheDocument()
    expect(screen.getByText(strings.limitCounter(1, 3))).toBeInTheDocument()
    expect(screen.getByRole('switch', { name: strings.reminderToggleLabel('Итоги недели') })).not.toBeChecked()
  })

  it('turns a reminder on in place', async () => {
    const user = userEvent.setup()
    vi.mocked(setReminderActive).mockResolvedValue({ ...weekly, isActive: true })
    renderScreen()

    await user.click(await screen.findByRole('switch', { name: strings.reminderToggleLabel('Итоги недели') }))

    expect(setReminderActive).toHaveBeenCalledWith('rem-2', true)
    expect(await screen.findByText(strings.limitCounter(2, 3))).toBeInTheDocument()
  })

  it('offers premium when the server refuses to turn on one more', async () => {
    const user = userEvent.setup()
    vi.mocked(setReminderActive).mockRejectedValue(new ApiRequestError('limit', 422, { code: 'NOTIFICATION_LIMIT_REACHED', message: 'limit', details: { limit: 3 } }))
    renderScreen()

    await user.click(await screen.findByRole('switch', { name: strings.reminderToggleLabel('Итоги недели') }))

    expect(await screen.findByText(strings.reminderLimitTitle)).toBeInTheDocument()
  })

  it('creates a monthly reminder on the chosen day', async () => {
    const user = userEvent.setup()
    vi.mocked(createReminder).mockResolvedValue(reminder({ id: 'rem-3', name: 'Аренда' }))
    renderScreen()

    await user.click(await screen.findByRole('button', { name: strings.addReminder }))
    await user.type(screen.getByRole('textbox', { name: strings.nameLabel }), 'Аренда')
    await user.click(screen.getByRole('radio', { name: strings.repeatOption('monthly') }))
    await user.selectOptions(screen.getByRole('combobox', { name: strings.dayOfMonthLabel }), '10')
    await user.click(screen.getByRole('button', { name: strings.save }))

    expect(createReminder).toHaveBeenCalledWith(expect.objectContaining({ name: 'Аренда', frequencyType: 'MONTHLY', dayOfMonth: 10, dayOfWeek: null }))
    expect(notify).toHaveBeenCalledWith(expect.objectContaining({ title: strings.reminderCreatedToast }))
  })
})
