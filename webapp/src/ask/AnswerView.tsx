import type { AnswerBar, AnswerCard, AnswerLink, Currency } from '../api/types'
import { formatMoney } from '../format'
import { strings } from '../strings'
import { Button } from '../ui/Button'
import { MicIcon } from '../ui/icons'
import { ScreenHeader } from '../ui/ScreenHeader'
import './ask.css'

const FULL_PERCENT = 100

export function AnswerView({ card, currency, language, onAsk, onRecord, onOpenLink, onClose }: {
  card: AnswerCard
  currency: Currency
  language: string
  onAsk: (question: string) => void
  onRecord: () => void
  onOpenLink: (link: AnswerLink) => void
  onClose: () => void
}) {
  const link = card.link
  return (
    <>
      <ScreenHeader title={strings.answerTitle} onBack={onClose} />
      <article className={`card answer-card ${card.understood ? '' : 'not-understood'}`} aria-live="polite">
        <h2 className="answer-title">{card.title}</h2>
        {card.headline && <p className="amount answer-headline">{card.headline}</p>}
        {card.lines.map((line) => <p key={line} className="answer-line">{line}</p>)}
        {card.bars.length > 0 && <AnswerBars bars={card.bars} currency={currency} language={language} />}
        {card.rows.length > 0 && (
          <ul className="answer-rows">
            {card.rows.map((row) => (
              <li key={`${row.label}-${row.value}`} className="answer-row">
                <span className="answer-row-icon" aria-hidden="true">{row.icon}</span>
                <span className="answer-row-label">{row.label}</span>
                <span className="answer-row-value">{row.value}</span>
                {row.note && <span className="caption answer-row-note">{row.note}</span>}
              </li>
            ))}
          </ul>
        )}
      </article>
      {card.followUps.length > 0 && (
        <section className="answer-follow" aria-label={strings.answerFollowUps}>
          {card.followUps.map((question) => (
            <button key={question} type="button" className="chip ask-chip pressable" onClick={() => onAsk(question)}>{question}</button>
          ))}
        </section>
      )}
      <div className="answer-actions">
        {link && <Button variant="secondary" onClick={() => onOpenLink(link)}>{strings.answerLinks[link.screen]}</Button>}
        <Button onClick={onRecord}><MicIcon />{strings.askAgain}</Button>
      </div>
    </>
  )
}

function AnswerBars({ bars, currency, language }: { bars: AnswerBar[]; currency: Currency; language: string }) {
  const max = Math.max(...bars.map((bar) => Number(bar.value)), 0)
  return (
    <ul className="answer-bars">
      {bars.map((bar) => (
        <li key={bar.label} className={`answer-bar ${bar.current ? 'current' : ''}`}>
          <span className="answer-bar-label">{bar.label}</span>
          <span className="answer-bar-track" aria-hidden="true">
            <span className="answer-bar-fill" style={{ width: `${max > 0 ? (Number(bar.value) / max) * FULL_PERCENT : 0}%` }} />
          </span>
          <span className="answer-bar-value">{formatMoney(bar.value, currency, language)}</span>
        </li>
      ))}
    </ul>
  )
}
