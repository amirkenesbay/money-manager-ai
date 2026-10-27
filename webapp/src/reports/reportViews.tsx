import { useCallback, useState } from 'react'
import type { AnalyticsReport, CategoryMonth, Currency, MemberAmount, MonthPair } from '../api/types'
import { useAsync } from '../api/useAsync'
import { useCategories } from '../operations/useCategories'
import { formatCompactMoney, formatDayLabel, formatMoney, formatMonthShort, formatMonthTitle } from '../format'
import { strings } from '../strings'
import { ChipSelect } from '../ui/ChipSelect'
import { StateView } from '../ui/StateView'
import { Button } from '../ui/Button'
import { chartColor, percentOf } from './chartMath'
import { Donut, Meter, MonthBars } from './charts'
import { ReportFrame } from './ReportFrame'
import type { ReportContext } from './ReportsScreen'
import { fetchAnalytics, fetchCategoryReport, fetchComparison, fetchMembers } from './reportsApi'

const EMPTY_REPORT_ICON = '📊'

function useMonthReport<T>(fetcher: (groupId: string, month: string, signal: AbortSignal) => Promise<T>, context: ReportContext) {
  const { group, month } = context
  const load = useCallback((signal: AbortSignal) => fetcher(group.id, month, signal), [fetcher, group.id, month])
  return useAsync(load)
}

export function AnalyticsView({ context }: { context: ReportContext }) {
  const report = useMonthReport(fetchAnalytics, context)
  return <ReportFrame report={report} context={context}>{(data) => <Analytics report={data} context={context} />}</ReportFrame>
}

function Analytics({ report, context }: { report: AnalyticsReport; context: ReportContext }) {
  const { language } = context.me
  const money = (amount: string | number) => formatMoney(String(amount), report.currency, language)
  if (report.operationCount === 0) return <EmptyMonth context={context} />
  const expense = Number(report.expense)
  const topTotal = report.topExpenses.reduce((sum, category) => sum + Number(category.total), 0)
  const slices = [
    ...report.topExpenses.map((category) => ({ name: `${category.icon} ${category.name}`, value: Number(category.total) })),
    ...(expense - topTotal > 0 ? [{ name: strings.otherCategories, value: expense - topTotal }] : []),
  ]
  const monthTitle = formatMonthTitle(report.month, language)
  return (
    <section className="report">
      <Donut values={slices.map((slice) => slice.value)} label={strings.expensesByCategory(monthTitle)}>
        <span className="caption">{strings.expensesLabel}</span>
        <span className="amount donut-total">{money(expense)}</span>
      </Donut>
      <ul className="legend">
        {slices.map((slice, index) => (
          <li key={slice.name} className="legend-row">
            <span className="legend-swatch" style={{ background: chartColor(index) }} aria-hidden="true" />
            <span className="legend-name">{slice.name}</span>
            <span className="caption legend-percent">{Math.round(percentOf(slice.value, expense))}%</span>
            <span className="amount">{money(slice.value)}</span>
          </li>
        ))}
      </ul>
      <dl className="facts well">
        {report.maxExpense && (
          <Fact label={strings.maxExpense} value={`${report.maxExpense.icon} ${money(report.maxExpense.amount)} · ${formatDayLabel(report.maxExpense.date, new Date(), language)}`} />
        )}
        {report.mostExpensiveDay && (
          <Fact label={strings.mostExpensiveDay} value={`${formatDayLabel(report.mostExpensiveDay.date, new Date(), language)} · ${money(report.mostExpensiveDay.total)}`} />
        )}
      </dl>
    </section>
  )
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div className="fact">
      <dt className="caption">{label}</dt>
      <dd className="row-title">{value}</dd>
    </div>
  )
}

function EmptyMonth({ context }: { context: ReportContext }) {
  return (
    <StateView icon={EMPTY_REPORT_ICON} title={strings.reportEmpty(formatMonthTitle(context.month, context.me.language))} body={strings.reportEmptyHint}>
      <Button variant="secondary" size="small" onClick={context.onAddOperation}>{strings.addFirstOperation}</Button>
    </StateView>
  )
}

export function ComparisonView({ context }: { context: ReportContext }) {
  const report = useMonthReport(fetchComparison, context)
  return (
    <ReportFrame report={report} context={context}>
      {(data) => {
        const months = { previous: data.previousMonth, current: data.month }
        const empty = [data.expense, data.income].every((pair) => Number(pair.previous) === 0 && Number(pair.current) === 0)
        if (empty) return <EmptyMonth context={context} />
        return (
          <section className="report">
            <ComparisonCard title={strings.comparisonExpense} pair={data.expense} months={months} currency={data.currency} language={context.me.language} lowerIsBetter />
            <ComparisonCard title={strings.comparisonIncome} pair={data.income} months={months} currency={data.currency} language={context.me.language} />
          </section>
        )
      }}
    </ReportFrame>
  )
}

function ComparisonCard({ title, pair, months, currency, language, lowerIsBetter = false }: {
  title: string
  pair: MonthPair
  months: { previous: string; current: string }
  currency: Currency
  language: string
  lowerIsBetter?: boolean
}) {
  const previous = Number(pair.previous)
  const current = Number(pair.current)
  const max = Math.max(previous, current)
  const change = previous > 0 ? Math.round(((current - previous) / previous) * 100) : null
  const better = change !== null && change !== 0 && (change < 0) === lowerIsBetter
  const tone = change === null || change === 0 ? 'caption' : better ? 'income' : 'negative'
  return (
    <article className="card comparison">
      <header className="comparison-head">
        <h2 className="section-title">{title}</h2>
        {change !== null && <span className={`row-title ${tone}`}>{`${change > 0 ? '+' : change < 0 ? '−' : ''}${Math.abs(change)}%`}</span>}
      </header>
      {[
        { month: months.previous, value: previous, color: 'var(--accent-soft)' },
        { month: months.current, value: current, color: 'var(--accent)' },
      ].map((bar) => (
        <div key={bar.month} className="comparison-bar">
          <span className="comparison-bar-head">
            <span className="caption">{formatMonthTitle(bar.month, language)}</span>
            <span className="amount">{formatMoney(String(bar.value), currency, language)}</span>
          </span>
          <Meter percent={percentOf(bar.value, max)} color={bar.color} />
        </div>
      ))}
    </article>
  )
}

export function MembersView({ context }: { context: ReportContext }) {
  const report = useMonthReport(fetchMembers, context)
  const { group, me } = context
  return (
    <ReportFrame report={report} context={context}>
      {(data) => (
        <section className="report">
          <p className="caption">{strings.membersCaption(group.name)}</p>
          {group.type === 'PERSONAL' && <p className="hint-box caption">{strings.membersSolo}</p>}
          {data.expenseByMember.length === 0
            ? <p className="hint-box caption">{strings.membersEmpty}</p>
            : <MemberCards members={data.expenseByMember} currency={data.currency} language={me.language} />}
        </section>
      )}
    </ReportFrame>
  )
}

function MemberCards({ members, currency, language }: { members: MemberAmount[]; currency: Currency; language: string }) {
  const max = Math.max(...members.map((member) => Number(member.total)))
  return (
    <ul className="rows">
      {members.map((member) => (
        <li key={member.name} className="card member">
          <span className="member-head">
            <span className="icon-circle member-initial" aria-hidden="true">{member.name.charAt(0)}</span>
            <span className="row-title member-name">{member.name}</span>
            <span className="amount">{formatMoney(member.total, currency, language)}</span>
          </span>
          <Meter percent={percentOf(Number(member.total), max)} />
        </li>
      ))}
    </ul>
  )
}

export function CategoryView({ context }: { context: ReportContext }) {
  const { group } = context
  const categories = useCategories(group.id)
  const [selected, setSelected] = useState<string | null>(null)

  if (categories.state.status !== 'ready') {
    return <ReportFrame report={categories} context={context}>{() => null}</ReportFrame>
  }
  const expenseCategories = categories.state.data.filter((category) => category.type === 'EXPENSE')
  if (expenseCategories.length === 0) return <p className="hint-box caption">{strings.noExpenseCategories}</p>
  const categoryId = selected ?? expenseCategories[0].id
  return (
    <section className="report">
      <ChipSelect
        label={strings.categoryFilterLabel}
        value={categoryId}
        options={expenseCategories.map((category) => ({ value: category.id, label: `${category.icon} ${category.name}` }))}
        onChange={setSelected}
      />
      <CategoryHistory key={categoryId} categoryId={categoryId} context={context} />
    </section>
  )
}

function CategoryHistory({ categoryId, context }: { categoryId: string; context: ReportContext }) {
  const { group, me } = context
  const load = useCallback((signal: AbortSignal) => fetchCategoryReport(group.id, categoryId, signal), [group.id, categoryId])
  const report = useAsync(load)
  return (
    <ReportFrame report={report} context={context}>
      {(data) => <CategoryMonths months={data.months} title={`${data.icon} ${data.name}`} currency={data.currency} language={me.language} />}
    </ReportFrame>
  )
}

function CategoryMonths({ months, title, currency, language }: { months: CategoryMonth[]; title: string; currency: Currency; language: string }) {
  const average = months.reduce((sum, month) => sum + Number(month.total), 0) / Math.max(months.length, 1)
  return (
    <>
      <MonthBars
        label={strings.categoryChart(title)}
        bars={months.map((month) => ({
          key: month.month,
          name: formatMonthShort(month.month, language),
          value: Number(month.total),
          caption: formatCompactMoney(Number(month.total), language),
        }))}
      />
      <p className="average">
        <span className="caption">{strings.categoryAverage}</span>
        <span className="amount">{formatMoney(String(Math.round(average)), currency, language)}</span>
      </p>
    </>
  )
}
