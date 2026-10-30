import type { ReactNode } from 'react'
import { strings } from '../strings'
import { ChartIcon, GridIcon, HomeIcon, ListIcon } from './icons'
import { type Tab, TABS } from './tabs'

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

export function TabBar({ active, onSelect }: { active: Tab; onSelect: (tab: Tab) => void }) {
  return (
    <div className="tab-bar-dock">
      <nav className="tab-bar" aria-label={strings.tabBarLabel}>
        {TABS.map((tab) => {
          const current = tab === active
          return (
            <button key={tab} type="button" className="tab" aria-label={tabLabel(tab)} aria-current={current ? 'page' : undefined} onClick={() => onSelect(tab)}>
              {TAB_ICONS[tab]}
              {current && <span aria-hidden="true">{tabLabel(tab)}</span>}
            </button>
          )
        })}
      </nav>
    </div>
  )
}
