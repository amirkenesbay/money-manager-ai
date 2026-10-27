import { strings } from '../strings'

const ROW_WIDTHS = ['7.5rem', '5.5rem', '8.75rem', '6.25rem', '7rem', '5rem']

export function SkeletonRows({ count, label = strings.loading }: { count: number; label?: string }) {
  return (
    <div className="skeleton" aria-busy="true" aria-label={label}>
      {ROW_WIDTHS.slice(0, count).map((width) => (
        <span key={width} className="skeleton-row row">
          <span className="icon-circle" />
          <span className="row-main"><span className="skeleton-bar" style={{ width }} /></span>
          <span className="skeleton-bar" style={{ width: '4rem' }} />
        </span>
      ))}
    </div>
  )
}

export function SkeletonCard() {
  return (
    <span className="skeleton-card" aria-hidden="true">
      <span className="skeleton-bar" style={{ width: '4.5rem' }} />
      <span className="skeleton-bar" style={{ width: '12rem', height: '1.75rem' }} />
      <span className="skeleton-bar" style={{ width: '70%' }} />
    </span>
  )
}
