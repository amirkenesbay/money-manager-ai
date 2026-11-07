import type { Tab } from '../ui/tabs'

const TOUR_TARGET_ATTRIBUTE = 'data-tour'

export type TourTarget =
  | 'summary' | 'group' | 'manual' | 'voice' | `tab-${Tab}`
  | 'operations-search' | 'operations-filters' | 'operations-feed'
  | 'reports-tabs' | 'reports-month' | 'reports-body'
  | 'more-manage' | 'more-preferences' | 'more-help'
  | 'form-type' | 'form-categories' | 'form-date' | 'form-repeat'
  | 'categories-type' | 'categories-list' | 'categories-add'
  | 'groups-list' | 'groups-create' | 'group-invite'
  | 'recurring-add' | 'reminders-add'
  | 'debts-tabs' | 'debts-add'
  | 'import-pick' | 'import-help'

export const tourTarget = (target: TourTarget) => ({ [TOUR_TARGET_ATTRIBUTE]: target })

export const tourTargetSelector = (target: TourTarget): string => `[${TOUR_TARGET_ATTRIBUTE}="${target}"]`
