import { type PointerEvent, type ReactNode, useState } from 'react'
import type { Group } from '../api/types'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { MicIcon } from '../ui/icons'
import { Sheet } from '../ui/Sheet'
import './onboarding.css'

const SWIPE_DISTANCE = 40
const JOIN_MARK = '+'

interface WelcomeCard {
  art: ReactNode
  title: string
  body: ReactNode
}

export function WelcomeSheet({ group, onFinish }: { group: Group; onFinish: (tryVoice: boolean) => void }) {
  const [step, setStep] = useState(0)
  const [swipeStart, setSwipeStart] = useState<number | null>(null)
  const cards = welcomeCards(group)
  const card = cards[step]
  const last = step === cards.length - 1
  const goTo = (next: number) => setStep(Math.min(cards.length - 1, Math.max(0, next)))

  const endSwipe = (event: PointerEvent) => {
    if (swipeStart === null) return
    const distance = event.clientX - swipeStart
    setSwipeStart(null)
    if (Math.abs(distance) < SWIPE_DISTANCE) return
    goTo(distance < 0 ? step + 1 : step - 1)
  }

  return (
    <Sheet label={strings.welcomeLabel} onClose={() => onFinish(false)}>
      <div className="welcome" onPointerDown={(event) => setSwipeStart(event.clientX)} onPointerUp={endSwipe}>
        <div key={step} className="welcome-card">
          <div className="welcome-art" aria-hidden="true">{card.art}</div>
          <h2 className="sheet-title">{card.title}</h2>
          {card.body}
        </div>
        <div className="welcome-dots" role="img" aria-label={strings.welcomeStep(step + 1, cards.length)}>
          {cards.map((item, index) => <span key={item.title} className={index === step ? 'welcome-dot current' : 'welcome-dot'} />)}
        </div>
        <div className="welcome-actions">
          {!last && <button type="button" className="welcome-skip" onClick={() => onFinish(false)}>{strings.welcomeSkip}</button>}
          {last
            ? <Button onClick={() => onFinish(true)}>{strings.welcomeTryVoice}</Button>
            : <Button onClick={() => goTo(step + 1)}>{strings.welcomeNext}</Button>}
        </div>
      </div>
    </Sheet>
  )
}

function welcomeCards(group: Group): WelcomeCard[] {
  const invited = group.type === 'SHARED' && !group.isOwner
  return [
    {
      art: <span className="welcome-ring"><span className="welcome-mic"><MicIcon /></span></span>,
      title: strings.welcomeVoiceTitle,
      body: (
        <>
          <p className="welcome-example">{strings.welcomeVoiceExample}</p>
          <p className="hint">{strings.welcomeVoiceBody}</p>
        </>
      ),
    },
    {
      art: <span className="welcome-donut" />,
      title: strings.welcomeBalanceTitle,
      body: <p className="hint">{strings.welcomeBalanceBody}</p>,
    },
    invited
      ? {
        art: <Avatars first={group.name.charAt(0)} second={`${JOIN_MARK}${group.memberCount - 1}`} />,
        title: strings.welcomeInvitedTitle(group.name),
        body: <p className="hint">{strings.welcomeInvitedBody(group.memberCount)}</p>,
      }
      : {
        art: <Avatars first={group.name.charAt(0)} second={JOIN_MARK} />,
        title: strings.welcomeTogetherTitle,
        body: <p className="hint">{strings.welcomeTogetherBody}</p>,
      },
  ]
}

function Avatars({ first, second }: { first: string; second: string }) {
  return (
    <span className="welcome-avatars">
      <span className="welcome-avatar current">{first}</span>
      <span className="welcome-avatar">{second}</span>
    </span>
  )
}
