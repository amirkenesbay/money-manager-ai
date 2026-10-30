import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { markHintSeen } from '../me/meApi'
import { strings } from '../strings'
import { group, me } from '../test/fixtures'
import { Hint, HintScope, HintsProvider } from './Hint'
import { WelcomeSheet } from './WelcomeSheet'

vi.mock('../me/meApi', () => ({ markHintSeen: vi.fn() }))

const renderHint = ({ seen = false, visible = true } = {}) => {
  const onMeChanged = vi.fn()
  render(
    <HintsProvider me={me({ seenHints: seen ? ['REPORTS'] : [] })} onMeChanged={onMeChanged}>
      <HintScope visible={visible}>
        <Hint hint="REPORTS" title={strings.hintReportsTitle} body={strings.hintReportsBody} />
      </HintScope>
    </HintsProvider>,
  )
  return onMeChanged
}

describe('Hint', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(markHintSeen).mockResolvedValue(me({ seenHints: ['REPORTS'] }))
  })

  it('explains the screen once and remembers it on the server', async () => {
    const onMeChanged = renderHint()

    await userEvent.click(screen.getByRole('button', { name: strings.hintOk }))

    expect(screen.queryByRole('note')).not.toBeInTheDocument()
    expect(markHintSeen).toHaveBeenCalledWith('REPORTS')
    expect(onMeChanged).toHaveBeenCalled()
  })

  it('closes on a tap anywhere', async () => {
    renderHint()

    await userEvent.click(document.body)

    expect(screen.queryByRole('note')).not.toBeInTheDocument()
  })

  it('stays away when already seen', () => {
    renderHint({ seen: true })

    expect(screen.queryByRole('note')).not.toBeInTheDocument()
  })

  it('waits while its screen is hidden', () => {
    renderHint({ visible: false })

    expect(screen.queryByRole('note')).not.toBeInTheDocument()
    expect(markHintSeen).not.toHaveBeenCalled()
  })
})

describe('WelcomeSheet', () => {
  it('walks through the cards and ends with voice', async () => {
    const onFinish = vi.fn()
    render(<WelcomeSheet group={group()} onFinish={onFinish} />)

    expect(screen.getByRole('heading', { name: strings.welcomeVoiceTitle })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.welcomeNext }))
    expect(screen.getByRole('heading', { name: strings.welcomeBalanceTitle })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.welcomeNext }))
    expect(screen.getByRole('heading', { name: strings.welcomeTogetherTitle })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: strings.welcomeSkip })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: strings.welcomeTryVoice }))

    expect(onFinish).toHaveBeenCalledWith(true)
  })

  it('can be skipped from the first card', async () => {
    const onFinish = vi.fn()
    render(<WelcomeSheet group={group()} onFinish={onFinish} />)

    await userEvent.click(screen.getByRole('button', { name: strings.welcomeSkip }))

    expect(onFinish).toHaveBeenCalledWith(false)
  })

  it('greets an invited member with their group instead of asking to invite', async () => {
    render(<WelcomeSheet group={group({ name: 'Семья', type: 'SHARED', isOwner: false, memberCount: 3 })} onFinish={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: strings.welcomeNext }))
    await userEvent.click(screen.getByRole('button', { name: strings.welcomeNext }))

    expect(screen.getByRole('heading', { name: strings.welcomeInvitedTitle('Семья') })).toBeInTheDocument()
    expect(screen.getByText(strings.welcomeInvitedBody(3))).toBeInTheDocument()
  })
})
