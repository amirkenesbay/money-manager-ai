import { useState } from 'react'
import { tourTarget } from '../onboarding/tourTarget'
import { ScreenTour } from '../onboarding/ScreenTour'
import type { Group, Me } from '../api/types'
import { strings } from '../strings'
import { BottomBar } from '../ui/BottomBar'
import { Button } from '../ui/Button'
import { ChevronRightIcon, UserIcon, UsersIcon } from '../ui/icons'
import { LimitScreen } from '../ui/LimitScreen'
import { ScreenHeader } from '../ui/ScreenHeader'
import type { Notify } from '../ui/useToast'
import { GroupDetail } from './GroupDetail'
import { GroupForm } from './GroupForm'
import { groupMeta } from './groupMeta'
import './groups.css'

type Mode = { kind: 'list' } | { kind: 'create' } | { kind: 'detail'; groupId: string } | { kind: 'limit' }

export interface GroupActions {
  created: (group: Group) => void
  updated: (group: Group) => void
  deleted: (group: Group) => void
  activate: (group: Group) => Promise<boolean>
}

export function GroupsScreen({ me, groups, actions, notify, onBack }: {
  me: Me
  groups: Group[]
  actions: GroupActions
  notify: Notify
  onBack: () => void
}) {
  const [mode, setMode] = useState<Mode>({ kind: 'list' })
  const { subscription } = me
  const limit = subscription.limits.ownedSharedGroups
  const ownedShared = groups.filter((group) => group.type === 'SHARED' && group.isOwner).length
  const atLimit = limit !== null && ownedShared >= limit
  const toList = () => setMode({ kind: 'list' })
  const selected = mode.kind === 'detail' ? groups.find((group) => group.id === mode.groupId) : undefined

  if (mode.kind === 'create') {
    return (
      <GroupForm
        defaultCurrency={groups.find((group) => group.isActive)?.currency ?? 'KZT'}
        onBack={toList}
        onCreated={(group) => {
          actions.created(group)
          notify({ title: strings.groupCreatedToast(group.name) })
          setMode({ kind: 'detail', groupId: group.id })
        }}
        onLimit={() => setMode({ kind: 'limit' })}
      />
    )
  }
  if (mode.kind === 'limit' && limit !== null) {
    return (
      <LimitScreen
        header={strings.groupsTitle}
        title={strings.groupLimitTitle}
        body={strings.groupLimitBody(limit)}
        meterLabel={strings.groupLimitMeter}
        used={ownedShared}
        limit={limit}
        subscribeUrl={subscription.subscribeUrl}
        backLabel={strings.backToGroups}
        onBack={toList}
      />
    )
  }
  if (selected) {
    return (
      <GroupDetail
        me={me}
        group={selected}
        onBack={toList}
        onActivate={async (group) => {
          const activated = await actions.activate(group)
          notify({ title: activated ? strings.groupActivatedToast(group.name) : strings.groupSwitchFailed })
        }}
        onUpdated={actions.updated}
        onDeleted={(group) => {
          actions.deleted(group)
          notify({ title: strings.groupDeletedToast, detail: group.name })
          toList()
        }}
        notify={notify}
      />
    )
  }

  return (
    <>
      <ScreenHeader
        title={strings.groupsTitle}
        onBack={onBack}
        action={limit !== null ? <span className="counter-chip">{strings.groupsFreeCounter(ownedShared, limit)}</span> : undefined}
      />
      <ScreenTour hint="GROUPS" />
      <ul className="rows" {...tourTarget('groups-list')}>
        {groups.map((group) => (
          <li key={group.id}>
            <button type="button" className={`row row-button pressable group-row ${group.isActive ? 'group-row-active' : ''}`} onClick={() => setMode({ kind: 'detail', groupId: group.id })}>
              <span className="icon-circle" aria-hidden="true">{group.type === 'SHARED' ? <UsersIcon /> : <UserIcon />}</span>
              <span className="row-main">
                <span className="row-title">{group.name}</span>
                <span className="caption">{groupMeta(group)}</span>
              </span>
              {group.isActive && <span className="badge">{strings.activeBadge}</span>}
              <ChevronRightIcon />
            </button>
          </li>
        ))}
      </ul>
      {!groups.some((group) => group.type === 'SHARED') && <p className="hint-box caption">{strings.noSharedGroupsHint}</p>}
      <BottomBar>
        <Button {...tourTarget('groups-create')} onClick={() => setMode(atLimit ? { kind: 'limit' } : { kind: 'create' })}>{strings.createGroup}</Button>
      </BottomBar>
    </>
  )
}
