import { type ReactNode, useState } from 'react'
import { serverMessage } from '../api/client'
import type { AnswerLink, CategoryType, Group, Me, Operation } from '../api/types'
import { AskTopicsSheet } from '../ask/AskTopicsSheet'
import { useAsync } from '../api/useAsync'
import { CategoriesScreen } from '../categories/CategoriesScreen'
import { Dashboard } from '../dashboard/Dashboard'
import { activateGroup, fetchGroups } from '../groups/groupsApi'
import { type GroupActions, GroupsScreen } from '../groups/GroupsScreen'
import { MoreScreen, type Section } from '../more/MoreScreen'
import { HintScope, HintsProvider } from '../onboarding/Hint'
import { useHints } from '../onboarding/hintsContext'
import { WelcomeSheet } from '../onboarding/WelcomeSheet'
import { OperationDetail } from '../operation/OperationDetail'
import { OperationForm, type SaveResult } from '../operation/OperationForm'
import { operationSummary } from '../operation/operationSummary'
import { deleteOperation } from '../operations/operationsApi'
import { OperationsScreen } from '../operations/OperationsScreen'
import { PremiumScreen } from '../premium/PremiumScreen'
import { RemindersScreen } from '../reminders/RemindersScreen'
import { RecurringScreen } from '../recurring/RecurringScreen'
import { ReportsScreen } from '../reports/ReportsScreen'
import { VoiceFlow, type VoiceStart } from '../voice/VoiceFlow'
import { type LaunchScreen, launchMonth, launchScreen } from './launchScreen'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { LoadError } from '../ui/LoadError'
import { withMotion } from '../ui/motion'
import { SkeletonCard } from '../ui/Skeleton'
import { ManualFab } from '../ui/ManualFab'
import { TabBar } from '../ui/TabBar'
import { type Tab, TABS } from '../ui/tabs'
import { Toast } from '../ui/Toast'
import { type Notify, useToast } from '../ui/useToast'

type SecondaryTab = Exclude<Tab, 'home'>

const SECONDARY_TABS = TABS.filter((candidate): candidate is SecondaryTab => candidate !== 'home')

type Overlay =
  | { kind: 'form'; operation: Operation | null; type?: CategoryType }
  | { kind: 'detail'; operation: Operation }
  | { kind: 'section'; section: Section }
  | { kind: 'voice'; start?: VoiceStart }

const LAUNCH_TABS: Partial<Record<LaunchScreen, Tab>> = {
  reports: 'reports',
  operations: 'operations',
}

const LAUNCH_OVERLAYS: Partial<Record<LaunchScreen, Overlay>> = {
  add: { kind: 'form', operation: null },
  categories: { kind: 'section', section: 'categories' },
  groups: { kind: 'section', section: 'groups' },
}

interface Navigation {
  addOperation: (type?: CategoryType) => void
  selectOperation: (operation: Operation) => void
  openSection: (section: Section) => void
  showWelcome: () => void
  ask: (question: string) => void
}

interface Workspace {
  me: Me
  group: Group
  groups: Group[]
  actions: GroupActions
  notify: Notify
  onMeChanged: (me: Me) => void
}

export function MainScreen({ me, activeGroupId, onMeChanged }: { me: Me; activeGroupId: string; onMeChanged: (me: Me) => void }) {
  const groups = useAsync(fetchGroups)

  if (groups.state.status === 'loading') return <main className="screen" aria-busy="true" aria-label={strings.loading}><SkeletonCard /></main>
  if (groups.state.status === 'error') {
    return <main className="screen"><LoadError message={groups.state.message} onRetry={groups.reload} /></main>
  }
  return (
    <HintsProvider me={me} onMeChanged={onMeChanged}>
      <GroupScreens me={me} initialGroups={groups.state.data} initialGroupId={activeGroupId} onMeChanged={onMeChanged} />
    </HintsProvider>
  )
}

function GroupScreens({ me, initialGroups, initialGroupId, onMeChanged }: {
  me: Me
  initialGroups: Group[]
  initialGroupId: string
  onMeChanged: (me: Me) => void
}) {
  const [groups, setGroups] = useState(initialGroups)
  const [groupId, setGroupId] = useState(initialGroupId)
  const [switchError, setSwitchError] = useState<string | null>(null)
  const [tab, setTab] = useState<Tab>(launchTab)
  const [overlay, setOverlay] = useState<Overlay | null>(launchOverlay)
  const [version, setVersion] = useState(0)
  const [questionsOpen, setQuestionsOpen] = useState(() => launchScreen() === 'ask')
  const [reportsMonth, setReportsMonth] = useState<string | null>(launchMonth)
  const [operationsSearch, setOperationsSearch] = useState<string | null>(null)
  const hints = useHints()
  const [welcomeOpen, setWelcomeOpen] = useState(() => !hints.isSeen('WELCOME') && launchScreen() === null)
  const toast = useToast()
  const group = groups.find((candidate) => candidate.id === groupId)
  const refresh = () => setVersion((value) => value + 1)
  const summary = (operation: Operation) => group ? operationSummary(operation, group.currency, me.language) : undefined
  const open = (next: Overlay) => withMotion('forward', () => setOverlay(next))
  const goBack = (next: Overlay | null, after?: () => void) => withMotion('back', () => {
    setOverlay(next)
    after?.()
  })
  const switchTab = (next: Tab) => withMotion('fade', () => setTab(next))

  const activate = async (target: Group): Promise<boolean> => {
    setSwitchError(null)
    try {
      await activateGroup(target.id)
      setGroupId(target.id)
      setGroups((list) => list.map((candidate) => ({ ...candidate, isActive: candidate.id === target.id })))
      return true
    } catch (error) {
      setSwitchError(serverMessage(error) ?? strings.groupSwitchFailed)
      return false
    }
  }

  const actions: GroupActions = {
    created: (created) => {
      setGroups((list) => [...list.map((candidate) => ({ ...candidate, isActive: false })), { ...created, isActive: true }])
      setGroupId(created.id)
      refresh()
    },
    updated: (updated) => {
      setGroups((list) => list.map((candidate) => (candidate.id === updated.id ? { ...updated, isActive: candidate.isActive } : candidate)))
      refresh()
    },
    deleted: (deleted) => {
      const remaining = groups.filter((candidate) => candidate.id !== deleted.id)
      setGroups(remaining)
      const fallback = remaining.find((candidate) => candidate.type === 'PERSONAL') ?? remaining[0]
      if (deleted.id === groupId && fallback) void activate(fallback)
      refresh()
    },
    activate,
  }

  const navigation: Navigation = {
    addOperation: (type) => open({ kind: 'form', operation: null, type }),
    selectOperation: (operation) => open({ kind: 'detail', operation }),
    openSection: (section) => open({ kind: 'section', section }),
    showWelcome: () => setWelcomeOpen(true),
    ask: (question) => {
      setQuestionsOpen(false)
      open({ kind: 'voice', start: { kind: 'question', text: question } })
    },
  }

  const recordVoice = () => {
    setQuestionsOpen(false)
    open({ kind: 'voice' })
  }

  const openLink = (link: AnswerLink) => {
    if (link.screen === 'CATEGORIES' || link.screen === 'RECURRING') {
      open({ kind: 'section', section: link.screen === 'CATEGORIES' ? 'categories' : 'recurring' })
      return
    }
    goBack(null, () => {
      if (link.screen === 'REPORTS') setReportsMonth(link.month)
      else setOperationsSearch(link.search)
      refresh()
      setTab(link.screen === 'REPORTS' ? 'reports' : 'operations')
    })
  }

  const finishWelcome = (tryVoice: boolean) => {
    setWelcomeOpen(false)
    if (!hints.isSeen('WELCOME')) hints.dismiss('WELCOME')
    if (tryVoice) open({ kind: 'voice' })
  }

  const undo = async (operations: Operation[]) => {
    toast.hide()
    try {
      await Promise.all(operations.map((operation) => deleteOperation(groupId, operation.id)))
      notifyOutcome('success')
      refresh()
      toast.show({ title: strings.undoneToast, detail: operations.length === 1 ? summary(operations[0]) : strings.savedCount(operations.length) })
    } catch {
      notifyOutcome('error')
      toast.show({ title: strings.undoFailed })
    }
  }

  const recorded = (operations: Operation[], note?: string) => {
    goBack(null, () => {
      refresh()
      setTab('home')
    })
    const detail = note ?? (operations.length === 1 ? summary(operations[0]) : strings.savedCount(operations.length))
    toast.show({ title: strings.savedToast, detail, celebrate: true, action: { label: strings.undo, onClick: () => void undo(operations) } })
  }

  const saved = ({ operation, created, note }: SaveResult) => {
    if (created) {
      recorded([operation], note)
      return
    }
    goBack({ kind: 'detail', operation }, refresh)
    toast.show({ title: strings.changedToast, detail: summary(operation) })
  }

  const deleted = (operation: Operation) => {
    goBack(null, refresh)
    toast.show({ title: strings.deletedToast, detail: summary(operation) })
  }

  const closeSection = () => goBack(null, refresh)

  const workspace: Workspace | null = group ? { me, group, groups, actions, notify: toast.show, onMeChanged } : null

  return (
    <>
      <main className={overlay ? 'screen' : 'screen with-tab-bar'}>
        {overlay?.kind === 'section' && workspace && <SectionScreen key={overlay.section} section={overlay.section} workspace={workspace} onBack={closeSection} />}
        {overlay?.kind === 'voice' && group && (
          <VoiceFlow
            me={me}
            group={group}
            onClose={closeSection}
            onManual={() => navigation.addOperation()}
            onSaved={recorded}
            onOpenLink={openLink}
          />
        )}
        {overlay && (overlay.kind === 'form' || overlay.kind === 'detail') && group && (
          <OperationOverlay
            overlay={overlay}
            group={group}
            language={me.language}
            onClose={() => goBack(null)}
            onEdit={(operation) => open({ kind: 'form', operation })}
            onBackToDetail={(operation) => goBack({ kind: 'detail', operation })}
            onSaved={saved}
            onDeleted={deleted}
          />
        )}
        <div className="screen-section" style={overlay ? { display: 'none' } : undefined}>
          <TabPanel active={tab === 'home' || !workspace} hintsVisible={!welcomeOpen}>
            <Dashboard
              key={`${groupId}-${version}`}
              groups={groups}
              groupId={groupId}
              language={me.language}
              switchError={switchError}
              onSwitchGroup={(nextGroupId) => {
                const target = groups.find((candidate) => candidate.id === nextGroupId)
                if (target) void activate(target)
              }}
              onOpenOperations={() => switchTab('operations')}
              onOpenBudgets={() => navigation.openSection('categories')}
              onAddIncome={() => navigation.addOperation('INCOME')}
              onSelectOperation={navigation.selectOperation}
              onAsk={navigation.ask}
            />
          </TabPanel>
          {workspace && SECONDARY_TABS.map((candidate) => (
            <TabPanel key={`${candidate}-${groupId}-${version}`} active={tab === candidate} hintsVisible={!overlay && !welcomeOpen}>
              <TabContent tab={candidate} workspace={workspace} navigation={navigation} reportsMonth={reportsMonth} operationsSearch={operationsSearch} />
            </TabPanel>
          ))}
          <TabBar active={tab} onSelect={switchTab} onRecordVoice={recordVoice} onShowQuestions={() => setQuestionsOpen(true)} />
          {!welcomeOpen && <ManualFab onOpen={() => navigation.addOperation()} />}
        </div>
      </main>
      {welcomeOpen && group && !overlay && <WelcomeSheet group={group} onFinish={finishWelcome} />}
      {questionsOpen && !welcomeOpen && !overlay && (
        <AskTopicsSheet groupId={groupId} onAsk={navigation.ask} onRecord={recordVoice} onClose={() => setQuestionsOpen(false)} />
      )}
      {toast.current && <Toast toast={toast.current} onDone={toast.hide} />}
    </>
  )
}

function OperationOverlay({ overlay, group, language, onClose, onEdit, onBackToDetail, onSaved, onDeleted }: {
  overlay: Extract<Overlay, { kind: 'form' | 'detail' }>
  group: Group
  language: string
  onClose: () => void
  onEdit: (operation: Operation) => void
  onBackToDetail: (operation: Operation) => void
  onSaved: (result: SaveResult) => void
  onDeleted: (operation: Operation) => void
}) {
  if (overlay.kind === 'detail') {
    return <OperationDetail group={group} language={language} operation={overlay.operation} onBack={onClose} onEdit={() => onEdit(overlay.operation)} onDeleted={onDeleted} />
  }
  const editing = overlay.operation
  return (
    <OperationForm
      key={editing?.id ?? 'new'}
      group={group}
      language={language}
      operation={editing}
      initialType={overlay.type}
      onBack={editing ? () => onBackToDetail(editing) : onClose}
      onSaved={onSaved}
      onDeleted={onDeleted}
    />
  )
}

function SectionScreen({ section, workspace, onBack }: { section: Section; workspace: Workspace; onBack: () => void }) {
  const { me, group, groups, actions, notify, onMeChanged } = workspace
  switch (section) {
    case 'categories':
      return <CategoriesScreen me={me} group={group} notify={notify} onBack={onBack} />
    case 'groups':
      return <GroupsScreen me={me} groups={groups} actions={actions} notify={notify} onBack={onBack} />
    case 'reminders':
      return <RemindersScreen me={me} onMeChanged={onMeChanged} notify={notify} onBack={onBack} />
    case 'recurring':
      return <RecurringScreen me={me} group={group} onMeChanged={onMeChanged} notify={notify} onBack={onBack} />
    default:
      return <PremiumScreen me={me} onBack={onBack} />
  }
}

function TabPanel({ active, hintsVisible, children }: { active: boolean; hintsVisible: boolean; children: ReactNode }) {
  return (
    <div className="tab-panel" hidden={!active}>
      <HintScope visible={active && hintsVisible}>{children}</HintScope>
    </div>
  )
}

function TabContent({ tab, workspace, navigation, reportsMonth, operationsSearch }: {
  tab: SecondaryTab
  workspace: Workspace
  navigation: Navigation
  reportsMonth: string | null
  operationsSearch: string | null
}) {
  const { me, group, groups, actions, notify, onMeChanged } = workspace
  switch (tab) {
    case 'operations':
      return <OperationsScreen me={me} group={group} initialSearch={operationsSearch} onSelectOperation={navigation.selectOperation} />
    case 'reports':
      return <ReportsScreen me={me} group={group} initialMonth={reportsMonth} onAddOperation={() => navigation.addOperation()} onAsk={navigation.ask} />
    default:
      return (
        <MoreScreen
          me={me}
          group={group}
          groups={groups}
          name={displayName(me)}
          onMeChanged={onMeChanged}
          onGroupUpdated={actions.updated}
          onOpenSection={navigation.openSection}
          onShowWelcome={navigation.showWelcome}
          notify={notify}
        />
      )
  }
}

function launchOverlay(): Overlay | null {
  const screen = launchScreen()
  return (screen && LAUNCH_OVERLAYS[screen]) ?? null
}

function launchTab(): Tab {
  const screen = launchScreen()
  return (screen && LAUNCH_TABS[screen]) ?? 'home'
}

function displayName(me: Me): string {
  return me.firstName ?? me.username ?? strings.defaultName
}
