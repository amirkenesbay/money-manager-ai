import { strings } from '../strings'
import { useBackButton } from '../telegram/useBackButton'

export function ScreenHeader({ title, onBack }: { title: string; onBack: () => void }) {
  const telegramBack = useBackButton(onBack)
  return (
    <header className="screen-header">
      {!telegramBack && (
        <button type="button" className="link-button" onClick={onBack}>{strings.back}</button>
      )}
      <h1>{title}</h1>
    </header>
  )
}
