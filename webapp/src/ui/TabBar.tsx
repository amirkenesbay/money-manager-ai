import { type CSSProperties, type ReactNode, useLayoutEffect, useRef, useState } from 'react'
import { strings } from '../strings'
import { selectionTick, tapImpact } from '../telegram/haptics'
import { ChartIcon, GridIcon, HomeIcon, ListIcon, MicIcon } from './icons'
import { type Tab, TABS } from './tabs'
import { useLongPress } from './useLongPress'
import { tourTarget } from '../onboarding/tourTarget'

const TAB_ICONS: Record<Tab, ReactNode> = {
  home: <HomeIcon />,
  operations: <ListIcon />,
  reports: <ChartIcon />,
  more: <GridIcon />,
}

const tabLabel = (tab: Tab): string => {
  const labels: Record<Tab, string> = {
    home: strings.tabHome,
    operations: strings.operationsTitle,
    reports: strings.reportsTitle,
    more: strings.tabMore,
  }
  return labels[tab]
}

const MIC_SLOT = 2

export function TabBar({ active, onSelect, onRecordVoice, onShowQuestions }: {
  active: Tab
  onSelect: (tab: Tab) => void
  onRecordVoice: () => void
  onShowQuestions: () => void
}) {
  const nav = useRef<HTMLElement>(null)
  const [indicatorX, setIndicatorX] = useState<number | null>(null)

  useLayoutEffect(() => {
    const current = nav.current?.querySelector<HTMLElement>('.tab[aria-current="page"]')
    setIndicatorX(current ? current.offsetLeft : null)
  }, [active])

  const micPress = useLongPress(onRecordVoice, () => {
    tapImpact()
    onShowQuestions()
  })
  const tabButton = (tab: Tab) => {
    const current = tab === active
    return (
      <button key={tab} type="button" className="tab" {...tourTarget(`tab-${tab}`)} aria-label={tabLabel(tab)} aria-current={current ? 'page' : undefined} onClick={() => { selectionTick(); onSelect(tab) }}>
        {TAB_ICONS[tab]}
      </button>
    )
  }
  return (
    <div className="tab-bar-dock">
      <nav ref={nav} className="tab-bar" aria-label={strings.tabBarLabel}>
        {indicatorX !== null && <span className="tab-indicator" style={{ '--indicator-x': `${indicatorX}px` } as CSSProperties} aria-hidden="true" />}
        {TABS.slice(0, MIC_SLOT).map(tabButton)}
        <span className="tab-mic-slot">
          <button type="button" className="tab-mic pressable" {...tourTarget('voice')} aria-label={strings.recordVoice} {...micPress}><MicIcon /></button>
        </span>
        {TABS.slice(MIC_SLOT).map(tabButton)}
      </nav>
    </div>
  )
}
