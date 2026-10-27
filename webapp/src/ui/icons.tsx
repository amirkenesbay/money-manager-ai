import type { ReactNode } from 'react'

function Icon({ size, strokeWidth = 2, children }: { size: number; strokeWidth?: number; children: ReactNode }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth}
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {children}
    </svg>
  )
}

export const ChevronDownIcon = () => <Icon size={14}><path d="M6 9l6 6 6-6" /></Icon>

export const ChevronRightIcon = () => <Icon size={16} strokeWidth={2.2}><path d="M9 18l6-6-6-6" /></Icon>

export const PlusIcon = () => <Icon size={20} strokeWidth={2.2}><path d="M12 5v14M5 12h14" /></Icon>

export const CalendarIcon = () => <Icon size={14}><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M3 10h18M8 3v4M16 3v4" /></Icon>

export const AlertIcon = () => <Icon size={14} strokeWidth={2.2}><path d="M12 3l9.5 17H2.5zM12 10v4M12 17h.01" /></Icon>

export const LockIcon = () => <Icon size={20} strokeWidth={1.9}><rect x="5" y="11" width="14" height="10" rx="2" /><path d="M8 11V7a4 4 0 0 1 8 0v4" /></Icon>

export const MicIcon = () => (
  <Icon size={48} strokeWidth={1.8}><rect x="9" y="3" width="6" height="11" rx="3" /><path d="M5 11a7 7 0 0 0 14 0M12 18v3" /></Icon>
)

export const KeyboardIcon = () => (
  <Icon size={22} strokeWidth={1.8}><rect x="2" y="6" width="20" height="12" rx="2" /><path d="M6 10h.01M10 10h.01M14 10h.01M18 10h.01M7 14h10" /></Icon>
)
