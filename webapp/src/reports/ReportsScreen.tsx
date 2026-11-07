import { AskChip } from '../ask/AskChip'
import { tourTarget } from '../onboarding/tourTarget'
import { ScreenTour } from '../onboarding/ScreenTour'
import { useHorizontalSwipe } from '../ui/useHorizontalSwipe'
import { useState } from 'react'
import type { Group, Me } from '../api/types'
import { formatMonthName, formatMonthTitle, formatPeriod, shiftMonth } from '../format'
import { strings } from '../strings'
import type { ReportTab } from './reportTabs'
import { ChevronLeftIcon, ChevronRightIcon } from '../ui/icons'
import { Segmented } from '../ui/Segmented'
import { AnalyticsView, CategoryView, ComparisonView, MembersView } from './reportViews'
import './ReportsScreen.css'
import { currentPeriodMonth, isCalendarMonth, monthRange } from '../operations/periods'

const REPORT_TABS: ReportTab[] = ['analytics', 'comparison', 'members', 'category']

export interface ReportContext {
  me: Me
  group: Group
  month: string
  onAddOperation: () => void
  onBackToCurrentMonth: () => void
}

export function ReportsScreen({ me, group, initialMonth = null, onAddOperation, onAsk }: {
  me: Me
  group: Group
  initialMonth?: string | null
  onAddOperation: () => void
  onAsk: (question: string) => void
}) {
  const currentMonth = currentPeriodMonth(new Date(), group.monthStartDay)
  const [tab, setTab] = useState<ReportTab>('analytics')
  const [month, setMonth] = useState(() => (initialMonth !== null && initialMonth <= currentMonth ? initialMonth : currentMonth))
  const context: ReportContext = { me, group, month, onAddOperation, onBackToCurrentMonth: () => setMonth(currentMonth) }
  const swipe = useHorizontalSwipe(
    () => tab !== 'category' && month < currentMonth && setMonth(shiftMonth(month, 1)),
    () => tab !== 'category' && setMonth(shiftMonth(month, -1)),
  )

  return (
    <>
      <h1 className="title">{strings.reportsTitle}</h1>
      <div {...tourTarget('reports-tabs')}>
        <Segmented label={strings.reportTabsLabel} options={REPORT_TABS} value={tab} optionKey={(option) => option} optionLabel={strings.reportTab} onChange={setTab} />
      </div>
      <ScreenTour hint="REPORTS" />
      {tab === 'category'
        ? <p className="month-nav-title">{strings.categoryPeriod}</p>
        : (
          <>
            <MonthNav tab={tab} month={month} currentMonth={currentMonth} startDay={group.monthStartDay} language={me.language} onChange={setMonth} />
          </>
        )}
      <div className="report-swipe" {...tourTarget('reports-body')} {...swipe}>
        <ReportBody key={`${group.id}-${tab}-${month}`} tab={tab} context={context} />
      </div>
      {tab !== 'category' && <AskChip place="reports" question={strings.askReportsChip} onAsk={onAsk} />}
    </>
  )
}

function MonthNav({ tab, month, currentMonth, startDay, language, onChange }: {
  tab: ReportTab
  month: string
  currentMonth: string
  startDay: number
  language: string
  onChange: (month: string) => void
}) {
  const title = tab === 'comparison'
    ? strings.comparisonPeriod(formatMonthName(shiftMonth(month, -1), language), formatMonthName(month, language))
    : monthTitle(month, startDay, language)
  return (
    <div className="month-nav" {...tourTarget('reports-month')}>
      <button type="button" className="round-button pressable month-nav-arrow" aria-label={strings.previousMonth} onClick={() => onChange(shiftMonth(month, -1))}>
        <ChevronLeftIcon />
      </button>
      <span className="month-nav-title" aria-live="polite">{title}</span>
      <button type="button" className="round-button pressable month-nav-arrow" aria-label={strings.nextMonth} disabled={month >= currentMonth} onClick={() => onChange(shiftMonth(month, 1))}>
        <ChevronRightIcon />
      </button>
    </div>
  )
}

function monthTitle(month: string, startDay: number, language: string): string {
  const range = monthRange(month, startDay)
  return isCalendarMonth(range) ? formatMonthTitle(month, language) : formatPeriod(range.from, range.to, language)
}

function ReportBody({ tab, context }: { tab: ReportTab; context: ReportContext }) {
  switch (tab) {
    case 'analytics':
      return <AnalyticsView context={context} />
    case 'comparison':
      return <ComparisonView context={context} />
    case 'members':
      return <MembersView context={context} />
    case 'category':
      return <CategoryView context={context} />
  }
}
