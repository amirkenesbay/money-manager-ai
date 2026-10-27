import type { ReactNode } from 'react'
import { strings } from '../strings'
import { ChartIcon, GridIcon, HomeIcon, ListIcon } from './icons'
import { useSoonNotice } from './useSoonNotice'

export type Tab = 'home' | 'operations' | 'reports'

const TABS: { tab: Tab; label: string; icon: ReactNode }[] = [
  { tab: 'home', label: strings.tabHome, icon: <HomeIcon /> },
  { tab: 'operations', label: strings.operationsTitle, icon: <ListIcon /> },
  { tab: 'reports', label: strings.reportsTitle, icon: <ChartIcon /> },
]

export function TabBar({ active, onSelect }: { active: Tab; onSelect: (tab: Tab) => void }) {
  const soon = useSoonNotice()
  return (
    <div className="tab-bar-dock">
      <p className="notice" role="status" hidden={!soon.visible}>{strings.comingSoon}</p>
      <nav className="tab-bar" aria-label={strings.tabBarLabel}>
        {TABS.map(({ tab, label, icon }) => (
          <TabButton key={tab} label={label} icon={icon} active={tab === active} onClick={() => onSelect(tab)} />
        ))}
        <TabButton label={strings.tabMore} icon={<GridIcon />} active={false} onClick={soon.show} />
      </nav>
    </div>
  )
}

function TabButton({ label, icon, active, onClick }: { label: string; icon: ReactNode; active: boolean; onClick: () => void }) {
  return (
    <button type="button" className="tab" aria-label={label} aria-current={active ? 'page' : undefined} onClick={onClick}>
      {icon}
      {active && <span aria-hidden="true">{label}</span>}
    </button>
  )
}
