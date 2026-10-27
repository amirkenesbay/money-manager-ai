import { useCallback, useState } from 'react'
import { serverMessage } from '../api/client'
import type { Group, Me, Operation } from '../api/types'
import { useAsync } from '../api/useAsync'
import { Dashboard } from '../dashboard/Dashboard'
import { activateGroup, fetchGroups } from '../dashboard/dashboardApi'
import { OperationDetail } from '../operation/OperationDetail'
import { OperationForm, type SaveResult } from '../operation/OperationForm'
import { operationSummary } from '../operation/operationSummary'
import { deleteOperation } from '../operations/operationsApi'
import { OperationsScreen } from '../operations/OperationsScreen'
import { ReportsScreen } from '../reports/ReportsScreen'
import { strings } from '../strings'
import { notifyOutcome } from '../telegram/haptics'
import { LoadError } from '../ui/LoadError'
import { SkeletonCard } from '../ui/Skeleton'
import { type Tab, TabBar } from '../ui/TabBar'
import { Toast } from '../ui/Toast'
import { useToast } from '../ui/useToast'

type Overlay = { kind: 'form'; operation: Operation | null } | { kind: 'detail'; operation: Operation }

interface Navigation {
  addOperation: () => void
  selectOperation: (operation: Operation) => void
}

export function MainScreen({ me, activeGroupId }: { me: Me; activeGroupId: string }) {
  const groups = useAsync(fetchGroups)
  const [groupId, setGroupId] = useState(activeGroupId)
  const [switchError, setSwitchError] = useState<string | null>(null)

  const switchGroup = useCallback(async (nextGroupId: string) => {
    setSwitchError(null)
    try {
      await activateGroup(nextGroupId)
      setGroupId(nextGroupId)
    } catch (error) {
      setSwitchError(serverMessage(error) ?? strings.groupSwitchFailed)
    }
  }, [])

  if (groups.state.status === 'loading') return <main className="screen" aria-busy="true" aria-label={strings.loading}><SkeletonCard /></main>
  if (groups.state.status === 'error') {
    return <main className="screen"><LoadError message={groups.state.message} onRetry={groups.reload} /></main>
  }
  return <GroupScreens me={me} groups={groups.state.data} groupId={groupId} switchError={switchError} onSwitchGroup={switchGroup} />
}

function GroupScreens({ me, groups, groupId, switchError, onSwitchGroup }: {
  me: Me
  groups: Group[]
  groupId: string
  switchError: string | null
  onSwitchGroup: (groupId: string) => void
}) {
  const [tab, setTab] = useState<Tab>('home')
  const [overlay, setOverlay] = useState<Overlay | null>(null)
  const [version, setVersion] = useState(0)
  const toast = useToast()
  const group = groups.find((candidate) => candidate.id === groupId)
  const navigation: Navigation = {
    addOperation: () => setOverlay({ kind: 'form', operation: null }),
    selectOperation: (operation) => setOverlay({ kind: 'detail', operation }),
  }
  const summary = (operation: Operation) => group ? operationSummary(operation, group.currency, me.language) : undefined
  const refresh = () => setVersion((value) => value + 1)

  const undo = async (operation: Operation) => {
    toast.hide()
    try {
      await deleteOperation(groupId, operation.id)
      notifyOutcome('success')
      refresh()
      toast.show({ title: strings.undoneToast, detail: summary(operation) })
    } catch {
      notifyOutcome('error')
      toast.show({ title: strings.undoFailed })
    }
  }

  const saved = ({ operation, created }: SaveResult) => {
    refresh()
    if (created) {
      setOverlay(null)
      setTab('home')
      toast.show({ title: strings.savedToast, detail: summary(operation), action: { label: strings.undo, onClick: () => void undo(operation) } })
      return
    }
    setOverlay({ kind: 'detail', operation })
    toast.show({ title: strings.changedToast, detail: summary(operation) })
  }

  const deleted = (operation: Operation) => {
    refresh()
    setOverlay(null)
    toast.show({ title: strings.deletedToast, detail: summary(operation) })
  }

  return (
    <>
      <main className={overlay ? 'screen' : 'screen with-tab-bar'}>
        {overlay && group && (
          <OverlayScreen
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
          {tab === 'home' || !group
            ? (
              <Dashboard
                key={`${groupId}-${version}`}
                name={displayName(me)}
                groups={groups}
                groupId={groupId}
                language={me.language}
                switchError={switchError}
                onSwitchGroup={onSwitchGroup}
                onOpenOperations={() => setTab('operations')}
                onOpenReports={() => setTab('reports')}
                onAddOperation={navigation.addOperation}
                onSelectOperation={navigation.selectOperation}
              />
            )
            : <TabContent key={`${tab}-${groupId}-${version}`} tab={tab} me={me} group={group} navigation={navigation} />}
          <TabBar active={tab} onSelect={setTab} />
        </div>
      </main>
      {toast.current && <Toast toast={toast.current} onDone={toast.hide} />}
    </>
  )
}

function OverlayScreen({ overlay, group, language, onClose, onEdit, onOpenDetail, onSaved, onDeleted }: {
  overlay: Overlay
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

function TabContent({ tab, me, group, navigation }: { tab: Exclude<Tab, 'home'>; me: Me; group: Group; navigation: Navigation }) {
  if (tab === 'operations') {
    return <OperationsScreen me={me} group={group} onAddOperation={navigation.addOperation} onSelectOperation={navigation.selectOperation} />
  }
  return <ReportsScreen me={me} group={group} onAddOperation={navigation.addOperation} />
}

function displayName(me: Me): string {
  return me.firstName ?? me.username ?? strings.defaultName
}
