import { useState } from 'react'
import { tourTarget } from '../onboarding/tourTarget'
import { ScreenTour } from '../onboarding/ScreenTour'
import type { Group, Me } from '../api/types'
import { useAsync } from '../api/useAsync'
import { useCategories } from '../categories/useCategories'
import { formatDate } from '../format'
import { currencyBadge } from '../groups/currencies'
import { CurrencySheet, type UndoAction } from '../groups/CurrencySheet'
import { MonthStartSheet } from '../groups/MonthStartSheet'
import { isLanguage, LANGUAGE_NAMES } from '../i18n/languages'
import { fetchReminders } from '../reminders/remindersApi'
import { strings } from '../strings'
import { useHomeScreen } from '../telegram/homeScreen'
import { deviceTimezone, describeTimezone } from '../me/timezone'
import { syncDeviceTimezone } from '../me/useDeviceTimezone'
import { Button } from '../ui/Button'
import { BellIcon, CalendarIcon, ClockIcon, ChartIcon, DownloadIcon, CoinIcon, GlobeIcon, GridIcon, HelpIcon, PhoneIcon, RepeatIcon, UsersIcon, WalletIcon, ConvertIcon, SwapIcon, UploadIcon } from '../ui/icons'
import { SettingsRow } from '../ui/SettingsRow'
import { DIGEST_KINDS } from './digestKinds'
import { DigestsSheet } from './DigestsSheet'
import { ExportSheet } from './ExportSheet'
import { BalanceSheet } from './BalanceSheet'
import { LanguageSheet } from './LanguageSheet'
import type { Notify } from '../ui/useToast'
import './more.css'

export type Section = 'categories' | 'groups' | 'reminders' | 'recurring' | 'premium' | 'debts' | 'import' | 'converter'

type SheetKind = 'language' | 'currency' | 'balance' | 'monthStart' | 'digests' | 'export'

export function MoreScreen({ me, group, groups, name, onMeChanged, onGroupUpdated, onOpenSection, onShowWelcome, notify }: {
  me: Me
  group: Group
  groups: Group[]
  name: string
  onMeChanged: (me: Me) => void
  onGroupUpdated: (group: Group) => void
  onOpenSection: (section: Section) => void
  onShowWelcome: () => void
  notify: Notify
}) {
  const [sheet, setSheet] = useState<SheetKind | null>(null)
  const homeScreen = useHomeScreen()

  const syncTimezone = () => {
    const syncing = syncDeviceTimezone(me)
    if (!syncing) {
      notify({ title: strings.timezoneLabel, detail: describeTimezone(deviceTimezone()) })
      return
    }
    syncing
      .then((next) => {
        onMeChanged(next)
        notify({ title: strings.timezoneSyncedToast, detail: describeTimezone(deviceTimezone()) })
      })
      .catch(() => notify({ title: strings.saveFailed }))
  }
  const categories = useCategories(group.id)
  const reminders = useAsync(fetchReminders)
  const { subscription, language } = me
  const paid = subscription.tier === 'PAID'
  const categoryCount = categories.state.status === 'ready' ? String(categories.state.data.length) : undefined
  const activeReminders = reminders.state.status === 'ready' ? strings.remindersOn(reminders.state.data.filter((reminder) => reminder.isActive).length) : undefined

  const groupSaved = (next: Group, title: string, action?: UndoAction) => {
    setSheet(null)
    onGroupUpdated(next)
    notify({ title, detail: next.name, action })
  }

  return (
    <>
      <h1 className="title">{strings.moreTitle}</h1>
      <ScreenTour hint="MORE" />
      <section className="card profile-card">
        <span className="avatar profile-avatar" aria-hidden="true">{name.charAt(0)}</span>
        <span className="row-main">
          <span className="row-title">{name}</span>
          <span className="caption">{!paid ? strings.tierFree : subscription.expiresAt ? strings.tierPaid(formatDate(subscription.expiresAt, language)) : strings.tierForever}</span>
        </span>
        <Button size="small" onClick={() => onOpenSection('premium')}>{strings.premiumButton}</Button>
      </section>

      <h2 className="field-label">{strings.manageSection}</h2>
      <ul className="settings-list" {...tourTarget('more-manage')}>
        <li><SettingsRow icon={<UsersIcon />} label={strings.groupsTitle} value={String(groups.length)} onClick={() => onOpenSection('groups')} /></li>
        <li><SettingsRow icon={<GridIcon />} label={strings.categoriesTitle} value={categoryCount} onClick={() => onOpenSection('categories')} /></li>
        <li><SettingsRow icon={<BellIcon />} label={strings.remindersTitle} value={activeReminders} onClick={() => onOpenSection('reminders')} /></li>
        <li><SettingsRow icon={<RepeatIcon />} label={strings.recurringTitle} onClick={() => onOpenSection('recurring')} /></li>
        <li><SettingsRow icon={<SwapIcon />} label={strings.debtsTitle} onClick={() => onOpenSection('debts')} /></li>
        <li><SettingsRow icon={<ConvertIcon />} label={strings.converterTitle} onClick={() => onOpenSection('converter')} /></li>
        <li><SettingsRow icon={<DownloadIcon />} label={strings.exportTitle} onClick={() => setSheet('export')} /></li>
        <li><SettingsRow icon={<UploadIcon />} label={strings.importRow} onClick={() => onOpenSection('import')} /></li>
        <li><SettingsRow icon={<ChartIcon />} label={strings.digestsTitle} value={strings.digestsOn(DIGEST_KINDS.length - me.mutedDigests.length)} onClick={() => setSheet('digests')} /></li>
      </ul>

      <h2 className="field-label">{strings.preferencesSection}</h2>
      <ul className="settings-list" {...tourTarget('more-preferences')}>
        <li><SettingsRow icon={<GlobeIcon />} label={strings.languageLabel} value={isLanguage(language) ? LANGUAGE_NAMES[language] : language} onClick={() => setSheet('language')} /></li>
        <li><SettingsRow icon={<CoinIcon />} label={strings.currencyLabel} value={currencyBadge(group.currency)} onClick={() => setSheet('currency')} /></li>
        <li><SettingsRow icon={<ClockIcon />} label={strings.timezoneLabel} value={describeTimezone(me.timezone ?? deviceTimezone())} onClick={syncTimezone} /></li>
        <li><SettingsRow icon={<CalendarIcon />} label={strings.monthStartLabel} value={strings.monthStartValue(group.monthStartDay)} onClick={() => setSheet('monthStart')} /></li>
        <li><SettingsRow icon={<WalletIcon />} label={strings.balanceNowTitle} onClick={() => setSheet('balance')} /></li>
        {homeScreen.state === 'available' && <li><SettingsRow icon={<PhoneIcon />} label={strings.homeScreenRow} onClick={homeScreen.add} /></li>}
        <li {...tourTarget('more-help')}><SettingsRow icon={<HelpIcon />} label={strings.howToUse} onClick={onShowWelcome} /></li>
      </ul>

      {sheet === 'language' && (
        <LanguageSheet
          me={me}
          onChanged={(next) => {
            setSheet(null)
            onMeChanged(next)
            notify({ title: strings.languageChangedToast })
          }}
          onClose={() => setSheet(null)}
        />
      )}
      {sheet === 'export' && (
        <ExportSheet
          me={me}
          group={group}
          onSent={(count) => {
            setSheet(null)
            notify({ title: strings.exportSentToast, detail: strings.exportSentDetail(count) })
          }}
          onClose={() => setSheet(null)}
        />
      )}
      {sheet === 'digests' && (
        <DigestsSheet me={me} onChanged={onMeChanged} onFailed={() => notify({ title: strings.toggleFailed })} onClose={() => setSheet(null)} />
      )}
      {sheet === 'currency' && <CurrencySheet group={group} language={me.language} onSaved={(next, undo) => groupSaved(next, strings.currencyChangedToast, undo)} onClose={() => setSheet(null)} />}
      {sheet === 'monthStart' && <MonthStartSheet group={group} onSaved={(next) => groupSaved(next, strings.monthStartChangedToast)} onClose={() => setSheet(null)} />}
      {sheet === 'balance' && <BalanceSheet group={group} onSaved={(next) => groupSaved(next, strings.balanceSavedToast)} onClose={() => setSheet(null)} />}
    </>
  )
}
