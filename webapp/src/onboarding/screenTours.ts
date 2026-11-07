import type { AppHint } from '../api/types'
import type { Strings } from '../i18n/ru'
import { strings } from '../strings'
import type { TourStep } from './SpotlightTour'
import type { TourTarget } from './tourTarget'

type TourTextKey = keyof Strings['tourTexts']

export type ScreenHint = Exclude<AppHint, 'WELCOME' | 'MANUAL_ENTRY'>

const SCREEN_TOURS: Record<ScreenHint, [TourTarget, TourTextKey][]> = {
  OPERATIONS: [['operations-search', 'operationsSearch'], ['operations-filters', 'operationsFilters'], ['operations-feed', 'operationsFeed']],
  REPORTS: [['reports-tabs', 'reportsTabs'], ['reports-month', 'reportsMonth'], ['reports-body', 'reportsBody']],
  MORE: [['more-manage', 'moreManage'], ['more-preferences', 'morePreferences'], ['more-help', 'moreHelp']],
  OPERATION_FORM: [['form-type', 'formType'], ['form-categories', 'formCategories'], ['form-date', 'formDate'], ['form-repeat', 'formRepeat']],
  CATEGORIES: [['categories-type', 'categoriesType'], ['categories-list', 'categoriesList'], ['categories-add', 'categoriesAdd']],
  GROUPS: [['groups-list', 'groupsList'], ['groups-create', 'groupsCreate']],
  GROUP: [['group-invite', 'groupInvite']],
  RECURRING: [['recurring-add', 'recurringAdd']],
  REMINDERS: [['reminders-add', 'remindersAdd']],
  DEBTS: [['debts-tabs', 'debtsTabs'], ['debts-add', 'debtsAdd']],
  IMPORT: [['import-pick', 'importPick'], ['import-help', 'importHelp']],
  ASK: [['voice', 'ask']],
}

export const screenTourSteps = (hint: ScreenHint): TourStep[] =>
  SCREEN_TOURS[hint].map(([target, text]) => ({ target, ...strings.tourTexts[text] }))
