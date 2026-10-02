import { strings } from '../strings'

export function LoadError({ message, onRetry }: { message: string | null; onRetry: () => void }) {
  return (
    <section className="card" role="alert">
      <h2>{strings.loadFailedTitle}</h2>
      <p className="hint">{message ?? strings.loadFailedFallback}</p>
      <button type="button" onClick={onRetry}>{strings.retry}</button>
    </section>
  )
}
