import { strings } from '../strings'

const LEGEND_WIDTHS = ['6.875rem', '5rem', '8.125rem', '5.625rem', '4.375rem']

export function ReportSkeleton() {
  return (
    <div className="report-skeleton" aria-busy="true" aria-label={strings.reportLoading}>
      <span className="donut donut-skeleton" />
      {LEGEND_WIDTHS.map((width) => (
        <span key={width} className="legend-row">
          <span className="legend-swatch skeleton-bar" />
          <span className="skeleton-bar" style={{ width }} />
          <span className="legend-spacer" />
          <span className="skeleton-bar" style={{ width: '5rem' }} />
        </span>
      ))}
      <p className="caption">{strings.reportLoading}</p>
    </div>
  )
}
