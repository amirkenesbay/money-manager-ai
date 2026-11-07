import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AppHint } from '../api/types'
import { markHintSeen } from '../me/meApi'
import { strings } from '../strings'
import { group, me } from '../test/fixtures'
import { HintScope, HintsProvider } from './Hint'
import { ScreenTour } from './ScreenTour'
import { SpotlightTour } from './SpotlightTour'
import { tourTarget } from './tourTarget'
import { tourSteps } from './tourSteps'

vi.mock('../me/meApi', () => ({ markHintSeen: vi.fn() }))

const reportsScreen = (
  <>
    <div {...tourTarget('reports-tabs')}>tabs</div>
    <div {...tourTarget('reports-month')}>month</div>
    <div {...tourTarget('reports-body')}>body</div>
  </>
)

const renderReportsTour = ({ seen = ['WELCOME'] as AppHint[], visible = true } = {}) => {
  const onMeChanged = vi.fn()
  render(
    <HintsProvider me={me({ seenHints: seen })} onMeChanged={onMeChanged}>
      {reportsScreen}
      <HintScope visible={visible}>
        <ScreenTour hint="REPORTS" />
      </HintScope>
    </HintsProvider>,
  )
  return onMeChanged
}

describe('ScreenTour', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(markHintSeen).mockResolvedValue(me({ seenHints: ['WELCOME', 'REPORTS'] }))
  })

  it('walks the screen step by step and remembers it on the server', async () => {
    const onMeChanged = renderReportsTour()

    expect(await screen.findByRole('dialog', { name: strings.tourTexts.reportsTabs.title })).toBeInTheDocument()
    expect(screen.getByText(strings.welcomeStep(1, 3))).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.welcomeNext }))
    expect(screen.getByRole('dialog', { name: strings.tourTexts.reportsMonth.title })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: strings.welcomeNext }))
    await userEvent.click(screen.getByRole('button', { name: strings.tourDone }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(markHintSeen).toHaveBeenCalledWith('REPORTS')
    expect(onMeChanged).toHaveBeenCalled()
  })

  it('can be skipped at any step', async () => {
    renderReportsTour()

    await userEvent.click(await screen.findByRole('button', { name: strings.welcomeSkip }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(markHintSeen).toHaveBeenCalledWith('REPORTS')
  })

  it('stays away when already seen', () => {
    renderReportsTour({ seen: ['WELCOME', 'REPORTS'] })

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('waits for the welcome tour and for its screen to be shown', () => {
    renderReportsTour({ seen: [] })
    renderReportsTour({ visible: false })

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(markHintSeen).not.toHaveBeenCalled()
  })
})

describe('SpotlightTour', () => {
  it('skips steps whose element is not on the screen', async () => {
    render(
      <>
        <div {...tourTarget('summary')}>summary</div>
        <div {...tourTarget('voice')}>voice</div>
        <SpotlightTour steps={tourSteps(group())} finishLabel={strings.welcomeTryVoice} laterLabel={strings.homeScreenLater} onFinish={vi.fn()} />
      </>,
    )

    expect(await screen.findByText(strings.welcomeStep(1, 2))).toBeInTheDocument()
  })

  it('ends the welcome tour with voice', async () => {
    const onFinish = vi.fn()
    render(
      <>
        <div {...tourTarget('summary')}>summary</div>
        <div {...tourTarget('voice')}>voice</div>
        <SpotlightTour steps={tourSteps(group())} finishLabel={strings.welcomeTryVoice} laterLabel={strings.homeScreenLater} onFinish={onFinish} />
      </>,
    )

    await userEvent.click(await screen.findByRole('button', { name: strings.welcomeNext }))
    await userEvent.click(screen.getByRole('button', { name: strings.welcomeTryVoice }))

    expect(onFinish).toHaveBeenCalledWith(true)
  })

  it('greets an invited member with their group', async () => {
    render(
      <>
        <div {...tourTarget('summary')}>summary</div>
        <SpotlightTour steps={tourSteps(group({ name: 'Семья', type: 'SHARED', isOwner: false, memberCount: 3 }))} finishLabel={strings.welcomeTryVoice} onFinish={vi.fn()} />
      </>,
    )

    expect(await screen.findByRole('dialog', { name: strings.welcomeInvitedTitle('Семья') })).toBeInTheDocument()
    expect(screen.getByText(strings.welcomeInvitedBody(3))).toBeInTheDocument()
  })
})
