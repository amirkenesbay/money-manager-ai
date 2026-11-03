import { type ReactNode, useEffect, useRef, useState } from 'react'
import type { Balance, MonthSummary } from '../api/types'
import { formatMoney } from '../format'
import { FULL_PERCENT } from '../percent'
import { strings } from '../strings'
import { PlusIcon } from '../ui/icons'
import { prefersLessMotion } from '../ui/motion'
import { useCountUp } from '../ui/useCountUp'
import { type HeroSlide, HERO_SLIDES, loadHeroSlide, saveHeroSlide } from './heroSlide'
import { dayBars, monthPace, safeToSpend } from './homeMath'

const SCROLL_SETTLE_MS = 80

export function HomeHero({ balance, summary, language, onAddIncome }: {
  balance: Balance
  summary: MonthSummary
  language: string
  onAddIncome: () => void
}) {
  const track = useRef<HTMLDivElement>(null)
  const [current, setCurrent] = useState<HeroSlide>(loadHeroSlide)
  const today = new Date()

  useEffect(() => {
    const element = track.current
    if (element) element.scrollLeft = HERO_SLIDES.indexOf(loadHeroSlide()) * element.clientWidth
  }, [])

  useEffect(() => {
    const element = track.current
    if (!element) return undefined
    let timer = 0
    const settle = () => {
      window.clearTimeout(timer)
      timer = window.setTimeout(() => {
        const slide = HERO_SLIDES[Math.round(element.scrollLeft / element.clientWidth)] ?? HERO_SLIDES[0]
        setCurrent(slide)
        saveHeroSlide(slide)
      }, SCROLL_SETTLE_MS)
    }
    element.addEventListener('scroll', settle, { passive: true })
    return () => {
      window.clearTimeout(timer)
      element.removeEventListener('scroll', settle)
    }
  }, [])

  const show = (slide: HeroSlide) => {
    const element = track.current
    if (!element) return
    element.scrollTo({ left: HERO_SLIDES.indexOf(slide) * element.clientWidth, behavior: prefersLessMotion() ? 'auto' : 'smooth' })
    setCurrent(slide)
    saveHeroSlide(slide)
  }
  const showNext = () => show(HERO_SLIDES[(HERO_SLIDES.indexOf(current) + 1) % HERO_SLIDES.length])

  const slides: Record<HeroSlide, ReactNode> = {
    spent: <SpentSlide summary={summary} language={language} today={today} />,
    safe: <SafeSlide summary={summary} language={language} today={today} onAddIncome={onAddIncome} />,
    balance: <BalanceSlide balance={balance} summary={summary} language={language} />,
  }

  return (
    <section className="home-hero" aria-roledescription="carousel" aria-label={strings.homeSummaryLabel}>
      <div className="home-hero-track" ref={track}>
        {HERO_SLIDES.map((slide, index) => (
          <div
            key={slide}
            className="home-hero-slide"
            role="group"
            aria-roledescription="slide"
            aria-label={strings.homeSlideNames[index]}
            onClick={showNext}
          >
            {slides[slide]}
          </div>
        ))}
      </div>
      <div className="home-hero-dots">
        {HERO_SLIDES.map((slide, index) => (
          <button
            key={slide}
            type="button"
            className={`home-hero-dot ${slide === current ? 'current' : ''}`}
            aria-label={strings.homeSlideNames[index]}
            aria-current={slide === current}
            onClick={() => show(slide)}
          />
        ))}
      </div>
    </section>
  )
}

function HeroAmount({ amount, summary, language }: { amount: number; summary: MonthSummary; language: string }) {
  const shown = useCountUp(amount)
  const value = shown === amount ? amount : Math.round(shown)
  return <p className="home-hero-amount amount">{formatMoney(String(value), summary.currency, language)}</p>
}

function SpentSlide({ summary, language, today }: { summary: MonthSummary; language: string; today: Date }) {
  const money = (amount: string) => formatMoney(amount, summary.currency, language)
  const hasIncome = Number(summary.income) > 0
  return (
    <>
      <p className="home-hero-kicker">{strings.homeSpentTitle}</p>
      <HeroAmount amount={Number(summary.expense)} summary={summary} language={language} />
      <p className="home-hero-meta">{hasIncome ? strings.homeSpentMeta(money(summary.income), money(summary.net)) : strings.homeNoIncome}</p>
      <DayBars summary={summary} today={today} />
    </>
  )
}

function DayBars({ summary, today }: { summary: MonthSummary; today: Date }) {
  const bars = dayBars(summary, today)
  const highest = Math.max(1, ...bars.map((bar) => bar.total))
  return (
    <div className="home-bars-wrap" role="img" aria-label={strings.homeChartLabel}>
      <div className="home-bars">
        {bars.map((bar) => (
          <span
            key={bar.date}
            className={`home-bar ${bar.isToday ? 'today' : ''} ${bar.isFuture ? 'future' : ''}`}
            style={{ height: bar.isFuture ? undefined : `${(bar.total / highest) * FULL_PERCENT}%` }}
          />
        ))}
      </div>
      <span className="home-bars-axis caption">
        <span>{Number(summary.from.slice(-2))}</span>
        <span>{Number(summary.to.slice(-2))}</span>
      </span>
    </div>
  )
}

function SafeSlide({ summary, language, today, onAddIncome }: {
  summary: MonthSummary
  language: string
  today: Date
  onAddIncome: () => void
}) {
  const safe = safeToSpend(summary, today)
  if (!safe) {
    return (
      <>
        <p className="home-hero-kicker">{strings.homeSafeTitle}</p>
        <p className="home-hero-empty">{strings.homeSafeEmpty}</p>
        <button
          type="button"
          className="home-hero-cta pressable"
          onClick={(event) => {
            event.stopPropagation()
            onAddIncome()
          }}
        >
          <PlusIcon />{strings.homeAddIncome}
        </button>
      </>
    )
  }
  const money = (amount: number) => formatMoney(String(amount), summary.currency, language)
  const pace = monthPace(summary, today)
  return (
    <>
      <p className="home-hero-kicker">{strings.homeSafeTitle}</p>
      <HeroAmount amount={safe.perDay} summary={summary} language={language} />
      {safe.left < 0
        ? <p className="home-hero-meta negative">{strings.monthOverspent(money(-safe.left))}</p>
        : <p className="home-hero-meta">{strings.homeSafeMeta(money(safe.left), safe.daysLeft)}</p>}
      <span className="home-pace" aria-hidden="true">
        <span className={`home-pace-fill ${pace.spentPercent > pace.elapsedPercent ? 'ahead' : ''}`} style={{ width: `${Math.min(FULL_PERCENT, pace.spentPercent)}%` }} />
        <span className="home-pace-mark" style={{ left: `${pace.elapsedPercent}%` }} />
      </span>
      <span className="home-pace-labels caption">
        <span>{strings.homeSpentShare(pace.spentPercent)}</span>
        <span>{strings.homeElapsedShare(pace.elapsedPercent)}</span>
      </span>
    </>
  )
}

function BalanceSlide({ balance, summary, language }: { balance: Balance; summary: MonthSummary; language: string }) {
  const money = (amount: string) => formatMoney(amount, summary.currency, language)
  return (
    <>
      <p className="home-hero-kicker">{strings.balanceTitle}</p>
      <HeroAmount amount={Number(balance.total)} summary={summary} language={language} />
      <dl className="home-hero-split">
        <div>
          <dt className="caption">{strings.homeMonthIncome}</dt>
          <dd className="amount income">{money(summary.income)}</dd>
        </div>
        <div>
          <dt className="caption">{strings.homeMonthExpense}</dt>
          <dd className="amount">{money(summary.expense)}</dd>
        </div>
      </dl>
    </>
  )
}
