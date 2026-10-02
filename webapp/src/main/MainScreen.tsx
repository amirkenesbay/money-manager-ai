import { useCallback, useState } from 'react'
import { serverMessage } from '../api/client'
import type { Me } from '../api/types'
import { useAsync } from '../api/useAsync'
import { Dashboard } from '../dashboard/Dashboard'
import { activateGroup, fetchGroups } from '../dashboard/dashboardApi'
import { OperationsScreen } from '../operations/OperationsScreen'
import { strings } from '../strings'
import { LoadError } from '../ui/LoadError'

type Screen = 'dashboard' | 'operations'

export function MainScreen({ me, activeGroupId }: { me: Me; activeGroupId: string }) {
  const groups = useAsync(fetchGroups)
  const [groupId, setGroupId] = useState(activeGroupId)
  const [screen, setScreen] = useState<Screen>('dashboard')
  const [switchError, setSwitchError] = useState<string | null>(null)
  const openDashboard = useCallback(() => setScreen('dashboard'), [])

  const switchGroup = useCallback(async (nextGroupId: string) => {
    setSwitchError(null)
    try {
      await activateGroup(nextGroupId)
      setGroupId(nextGroupId)
    } catch (error) {
      setSwitchError(serverMessage(error) ?? strings.groupSwitchFailed)
    }
  }, [])

  if (groups.state.status === 'loading') return <main className="screen"><p className="hint">{strings.loading}</p></main>
  if (groups.state.status === 'error') {
    return <main className="screen"><LoadError message={groups.state.message} onRetry={groups.reload} /></main>
  }
  const group = groups.state.data.find((candidate) => candidate.id === groupId)
  if (screen === 'operations' && group) return <OperationsScreen me={me} group={group} onBack={openDashboard} />
  return (
    <Dashboard
      groups={groups.state.data}
      groupId={groupId}
      language={me.language}
      switchError={switchError}
      onSwitchGroup={switchGroup}
      onOpenOperations={() => setScreen('operations')}
    />
  )
}
