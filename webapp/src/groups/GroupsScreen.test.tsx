import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/client'
import type { Group } from '../api/types'
import { strings } from '../strings'
import { FAMILY_GROUP_ID, group, me } from '../test/fixtures'
import { createGroup, deleteGroup, fetchInvite, fetchMembers } from './groupsApi'
import { type GroupActions, GroupsScreen } from './GroupsScreen'
import { confirmBySlide } from '../test/slide'

vi.mock('./groupsApi', () => ({
  createGroup: vi.fn(),
  updateGroup: vi.fn(),
  deleteGroup: vi.fn(),
  fetchMembers: vi.fn(),
  fetchInvite: vi.fn(),
}))
vi.mock('../telegram/telegramEnv', () => ({ openExternalLink: vi.fn(), rawInitData: () => undefined }))

const INVITE_URL = 'https://t.me/moneyManagerAIbot?start=join_ABC'
const personal = group()
const family = group({ id: FAMILY_GROUP_ID, name: 'Семья', type: 'SHARED', isOwner: false, isActive: false, memberCount: 2 })
const notify = vi.fn()
const actions: GroupActions = { created: vi.fn(), updated: vi.fn(), deleted: vi.fn(), activate: vi.fn() }
const renderScreen = (groups: Group[] = [personal, family]) => render(<GroupsScreen me={me()} groups={groups} actions={actions} notify={notify} onBack={vi.fn()} />)

describe('GroupsScreen', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(fetchMembers).mockResolvedValue([
      { userId: 7, name: 'Айгерим', isOwner: true },
      { userId: 42, name: 'Амир', isOwner: false },
    ])
    vi.mocked(fetchInvite).mockResolvedValue({ inviteToken: 'ABC', url: INVITE_URL })
  })

  it('lists groups with the active mark and the shared counter', () => {
    renderScreen()

    expect(screen.getByText('Личное')).toBeInTheDocument()
    expect(screen.getByText('Семья')).toBeInTheDocument()
    expect(screen.getByText(strings.activeBadge)).toBeInTheDocument()
    expect(screen.getByText(strings.groupsFreeCounter(0, 3))).toBeInTheDocument()
  })

  it('creates a group with the chosen currency and opens it', async () => {
    const user = userEvent.setup()
    const created = group({ id: 'g-new', name: 'Квартира', type: 'SHARED', currency: 'EUR', isActive: true })
    vi.mocked(createGroup).mockResolvedValue(created)
    renderScreen()

    await user.click(screen.getByRole('button', { name: strings.createGroup }))
    await user.click(screen.getByRole('button', { name: strings.groupTemplates[1] }))
    await user.click(screen.getByRole('radio', { name: `${strings.currencyName('EUR')} EUR` }))
    await user.click(screen.getByRole('button', { name: strings.createGroup }))

    expect(createGroup).toHaveBeenCalledWith(strings.groupTemplates[1], 'EUR')
    expect(actions.created).toHaveBeenCalledWith(created)
    expect(notify).toHaveBeenCalledWith({ title: strings.groupCreatedToast('Квартира') })
  })

  it('offers premium at the shared groups limit', async () => {
    const user = userEvent.setup()
    const owned = [1, 2, 3].map((index) => group({ id: `g-${index}`, name: `Группа ${index}`, type: 'SHARED', isActive: false }))
    renderScreen([personal, ...owned])

    await user.click(screen.getByRole('button', { name: strings.createGroup }))

    expect(screen.getByText(strings.groupLimitTitle)).toBeInTheDocument()
  })

  it('shows members and the invite link but hides owner actions from a member', async () => {
    const user = userEvent.setup()
    renderScreen()

    await user.click(screen.getByRole('button', { name: /Семья/ }))

    expect(await screen.findByText('Айгерим')).toBeInTheDocument()
    expect(screen.getByText(strings.ownerMark)).toBeInTheDocument()
    expect(await screen.findByText(INVITE_URL)).toBeInTheDocument()
    expect(screen.getByText(strings.ownerOnlyHint)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: strings.deleteGroup })).not.toBeInTheDocument()
  })

  it('makes a group active from its card', async () => {
    const user = userEvent.setup()
    vi.mocked(actions.activate).mockResolvedValue(true)
    renderScreen()

    await user.click(screen.getByRole('button', { name: /Семья/ }))
    await user.click(screen.getByRole('button', { name: strings.makeActive }))

    expect(actions.activate).toHaveBeenCalledWith(family)
    expect(notify).toHaveBeenCalledWith({ title: strings.groupActivatedToast('Семья') })
  })

  it('lets the owner delete a shared group after confirmation', async () => {
    const user = userEvent.setup()
    const owned = { ...family, isOwner: true }
    vi.mocked(deleteGroup).mockResolvedValue(undefined)
    renderScreen([personal, owned])

    await user.click(screen.getByRole('button', { name: /Семья/ }))
    await user.click(screen.getByRole('button', { name: strings.deleteGroup }))
    await confirmBySlide(screen.getByRole('dialog'), strings.deleteGroup)

    expect(deleteGroup).toHaveBeenCalledWith(FAMILY_GROUP_ID)
    expect(actions.deleted).toHaveBeenCalledWith(owned)
  })

  it('never offers to delete the personal group', async () => {
    const user = userEvent.setup()
    renderScreen()

    await user.click(screen.getByRole('button', { name: /Личное/ }))

    expect(screen.getByText(strings.personalGroupNote)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: strings.deleteGroup })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: strings.renameGroup })).toBeInTheDocument()
  })

  it('shows a duplicate name from the server under the field', async () => {
    const user = userEvent.setup()
    const message = 'Группа «Семья» у вас уже есть.'
    vi.mocked(createGroup).mockRejectedValue(new ApiRequestError(message, 409, { code: 'GROUP_DUPLICATE', message, details: {} }))
    renderScreen()

    await user.click(screen.getByRole('button', { name: strings.createGroup }))
    await user.type(screen.getByRole('textbox', { name: strings.nameLabel }), 'Семья')
    await user.click(screen.getByRole('button', { name: strings.createGroup }))

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
  })
})
