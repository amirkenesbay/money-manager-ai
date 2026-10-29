import { useState } from 'react'
import { serverMessage } from '../api/client'
import type { Group, Me, Operation } from '../api/types'
import { useAsync } from '../api/useAsync'
import { CategoriesScreen } from '../categories/CategoriesScreen'
import { Dashboard } from '../dashboard/Dashboard'
import { activateGroup, fetchGroups } from '../groups/groupsApi'
import { type GroupActions, GroupsScreen } from '../groups/GroupsScreen'
import { MoreScreen, type Section } from '../more/MoreScreen'
import { OperationDetail } from '../operation/OperationDetail'
import { OperationForm, type SaveResult } from '../operation/OperationForm'
import { operationSummary } from '../operation/operationSummary'
import { deleteOperation } from '../operations/operationsApi'
import { OperationsScreen } from '../operations/OperationsScreen'
import { PremiumScreen } from '../premium/PremiumScreen'
import { RemindersScreen } from '../reminders/RemindersScreen'
import { ReportsScreen } from '../reports/ReportsScreen'
import { VoiceFlow } from '../voice/VoiceFlow'
import { type LaunchScreen, launchScreen } from './launchScreen'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { LoadError } from '../ui/LoadError'
import { SkeletonCard } from '../ui/Skeleton'
import { type Tab, TabBar } from '../ui/TabBar'
import { Toast } from '../ui/Toast'
import { type Notify, useToast } from '../ui/useToast'

type Overlay =
  | { kind: 'form'; operation: Operation | null }
  | { kind: 'detail'; operation: Operation }
  | { kind: 'section'; section: Section }
  | { kind: 'voice' }

const LAUNCH_OVERLAYS: Record<LaunchScreen, Overlay> = {
  add: { kind: 'form', operation: null },
  categories: { kind: 'section', section: 'categories' },
}

interface Navigation {
  addOperation: () => void
  selectOperation: (operation: Operation) => void
  openSection: (section: Section) => void
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
  return <GroupScreens me={me} initialGroups={groups.state.data} initialGroupId={activeGroupId} onMeChanged={onMeChanged} />
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
  const [tab, setTab] = useState<Tab>('home')
  const [overlay, setOverlay] = useState<Overlay | null>(launchOverlay)
  const [version, setVersion] = useState(0)
  const toast = useToast()
  const group = groups.find((candidate) => candidate.id === groupId)
  const refresh = () => setVersion((value) => value + 1)
  const summary = (operation: Operation) => group ? operationSummary(operation, group.currency, me.language) : undefined

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
    addOperation: () => setOverlay({ kind: 'form', operation: null }),
    selectOperation: (operation) => setOverlay({ kind: 'detail', operation }),
    openSection: (section) => setOverlay({ kind: 'section', section }),
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

  const recorded = (operations: Operation[]) => {
    refresh()
    setOverlay(null)
    setTab('home')
    const detail = operations.length === 1 ? summary(operations[0]) : strings.savedCount(operations.length)
    toast.show({ title: strings.savedToast, detail, celebrate: true, action: { label: strings.undo, onClick: () => void undo(operations) } })
  }

  const saved = ({ operation, created }: SaveResult) => {
    if (created) {
      recorded([operation])
      return
    }
    refresh()
    setOverlay({ kind: 'detail', operation })
    toast.show({ title: strings.changedToast, detail: summary(operation) })
  }

  const deleted = (operation: Operation) => {
    refresh()
    setOverlay(null)
    toast.show({ title: strings.deletedToast, detail: summary(operation) })
  }

  const closeSection = () => {
    setOverlay(null)
    refresh()
  }

  const workspace: Workspace | null = group ? { me, group, groups, actions, notify: toast.show, onMeChanged } : null

  return (
    <>
      <main className={overlay ? 'screen' : 'screen with-tab-bar'}>
        {overlay?.kind === 'section' && workspace && <SectionScreen key={overlay.section} section={overlay.section} workspace={workspace} onBack={closeSection} />}
        {overlay?.kind === 'voice' && group && (
          <VoiceFlow
            me={me}
            group={group}
            onClose={() => {
              setOverlay(null)
              refresh()
            }}
            onManual={navigation.addOperation}
            onSaved={recorded}
          />
        )}
        {overlay && (overlay.kind === 'form' || overlay.kind === 'detail') && group && (
          <OperationOverlay
            overlay={overlay}
            group={group}
            language={me.language}
            onClose={() => setOverlay(null)}
            onEdit={(operation) => setOverlay({ kind: 'form', operation })}
            onOpenDetail={navigation.selectOperation}
            onSaved={saved}
            onDeleted={deleted}
          />
        )}
        <div className="screen-section" style={overlay ? { display: 'none' } : undefined}>
          {tab === 'home' || !workspace
            ? (
              <Dashboard
                key={`${groupId}-${version}`}
                name={displayName(me)}
                groups={groups}
                groupId={groupId}
                language={me.language}
                switchError={switchError}
                onSwitchGroup={(nextGroupId) => {
                  const target = groups.find((candidate) => candidate.id === nextGroupId)
                  if (target) void activate(target)
                }}
                onOpenOperations={() => setTab('operations')}
                onOpenReports={() => setTab('reports')}
                onAddOperation={navigation.addOperation}
                onRecordVoice={() => setOverlay({ kind: 'voice' })}
                onSelectOperation={navigation.selectOperation}
              />
            )
            : <TabContent key={`${tab}-${groupId}-${version}`} tab={tab} workspace={workspace} navigation={navigation} />}
          <TabBar active={tab} onSelect={setTab} />
        </div>
      </main>
      {toast.current && <Toast toast={toast.current} onDone={toast.hide} />}
    </>
  )
}

function OperationOverlay({ overlay, group, language, onClose, onEdit, onOpenDetail, onSaved, onDeleted }: {
  overlay: Extract<Overlay, { kind: 'form' | 'detail' }>
  group: Group
  language: string
  onClose: () => void
  onEdit: (operation: Operation) => void
  onOpenDetail: (operation: Operation) => void
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
      onBack={editing ? () => onOpenDetail(editing) : onClose}
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
    default:
      return <PremiumScreen me={me} onBack={onBack} />
  }
}

function TabContent({ tab, workspace, navigation }: { tab: Exclude<Tab, 'home'>; workspace: Workspace; navigation: Navigation }) {
  const { me, group, groups, actions, notify, onMeChanged } = workspace
  switch (tab) {
    case 'operations':
      return <OperationsScreen me={me} group={group} onAddOperation={navigation.addOperation} onSelectOperation={navigation.selectOperation} />
    case 'reports':
      return <ReportsScreen me={me} group={group} onAddOperation={navigation.addOperation} />
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
          notify={notify}
        />
      )
  }
}

function launchOverlay(): Overlay | null {
  const screen = launchScreen()
  return screen ? LAUNCH_OVERLAYS[screen] : null
}

function displayName(me: Me): string {
  return me.firstName ?? me.username ?? strings.defaultName
}
