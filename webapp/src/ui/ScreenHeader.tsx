import type { ReactNode } from 'react'
import { strings } from '../strings'
import { useBackButton } from '../telegram/useBackButton'
import { useScrolled } from './useScrolled'

export function ScreenHeader({ title, onBack, action }: { title: string; onBack: () => void; action?: ReactNode }) {
  const telegramBack = useBackButton(onBack)
  const scrolled = useScrolled()
  return (
    <header className={`screen-header ${scrolled ? 'compact' : ''}`}>
      {!telegramBack && <button type="button" className="link-button" onClick={onBack}>{strings.back}</button>}
      <div className="screen-header-row">
        <h1 className="title">{title}</h1>
        {action}
      </div>
    </header>
  )
}
