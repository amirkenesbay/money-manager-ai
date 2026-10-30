import { useState } from 'react'
import type { Group, Me } from '../api/types'
import { formatMonthName, formatMonthTitle, shiftMonth, toIsoMonth } from '../format'
import { strings } from '../strings'
import type { ReportTab } from './reportTabs'
import { ChevronLeftIcon, ChevronRightIcon } from '../ui/icons'
import { Segmented } from '../ui/Segmented'
import { AnalyticsView, CategoryView, ComparisonView, MembersView } from './reportViews'
import './ReportsScreen.css'
import { Hint } from '../onboarding/Hint'

const REPORT_TABS: ReportTab[] = ['analytics', 'comparison', 'members', 'category']

export interface ReportContext {
  me: Me
  group: Group
  month: string
  onAddOperation: () => void
  onBackToCurrentMonth: () => void
}

export function ReportsScreen({ me, group, onAddOperation }: { me: Me; group: Group; onAddOperation: () => void }) {
  const currentMonth = toIsoMonth(new Date())
  const [tab, setTab] = useState<ReportTab>('analytics')
  const [month, setMonth] = useState(currentMonth)
  const context: ReportContext = { me, group, month, onAddOperation, onBackToCurrentMonth: () => setMonth(currentMonth) }

  return (
    <>
      <h1 className="title">{strings.reportsTitle}</h1>
      <Segmented label={strings.reportTabsLabel} options={REPORT_TABS} value={tab} optionKey={(option) => option} optionLabel={strings.reportTab} onChange={setTab} />
      {tab === 'category'
        ? <p className="month-nav-title">{strings.categoryPeriod}</p>
        : (
          <>
            <MonthNav tab={tab} month={month} currentMonth={currentMonth} language={me.language} onChange={setMonth} />
            <Hint hint="REPORTS" title={strings.hintReportsTitle} body={strings.hintReportsBody} />
          </>
        )}
      <ReportBody key={`${group.id}-${tab}-${month}`} tab={tab} context={context} />
    </>
  )
}

function MonthNav({ tab, month, currentMonth, language, onChange }: {
  tab: ReportTab
  month: string
  currentMonth: string
  language: string
  onChange: (month: string) => void
}) {
  const title = tab === 'comparison'
    ? strings.comparisonPeriod(formatMonthName(shiftMonth(month, -1), language), formatMonthName(month, language))
    : formatMonthTitle(month, language)
  return (
    <div className="month-nav">
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
