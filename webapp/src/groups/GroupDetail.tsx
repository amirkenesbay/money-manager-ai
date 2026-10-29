import { useCallback, useState } from 'react'
import type { Group, Me, Member } from '../api/types'
import { useAsync } from '../api/useAsync'
import { initialOf } from '../format'
import { strings } from '../strings'
import { openExternalLink } from '../telegram/telegramEnv'
import { Moment } from '../three/Moment'
import { MAX_GROUP_BALLS } from '../three/moments'
import { Button } from '../ui/Button'
import { ConfirmSheet } from '../ui/ConfirmSheet'
import { CoinIcon, CopyIcon, EditIcon, SendIcon, TrashIcon } from '../ui/icons'
import { LoadError } from '../ui/LoadError'
import { ScreenHeader } from '../ui/ScreenHeader'
import { SettingsRow } from '../ui/SettingsRow'
import { SkeletonRows } from '../ui/Skeleton'
import type { Notify } from '../ui/useToast'
import { currencyBadge } from './currencies'
import { CurrencySheet } from './CurrencySheet'
import { groupMeta } from './groupMeta'
import { deleteGroup, fetchInvite, fetchMembers } from './groupsApi'
import { RenameSheet } from './RenameSheet'

const SHARE_URL = 'https://t.me/share/url'
const MEMBER_SKELETON_ROWS = 2

type SheetKind = 'rename' | 'currency' | 'delete'

export function GroupDetail({ me, group, onBack, onActivate, onUpdated, onDeleted, notify }: {
  me: Me
  group: Group
  onBack: () => void
  onActivate: (group: Group) => void
  onUpdated: (group: Group) => void
  onDeleted: (group: Group) => void
  notify: Notify
}) {
  const [sheet, setSheet] = useState<SheetKind | null>(null)
  const shared = group.type === 'SHARED'

  const updated = (next: Group, title: string) => {
    setSheet(null)
    onUpdated(next)
    notify({ title, detail: next.name })
  }

  return (
    <>
      <ScreenHeader title={group.name} onBack={onBack} />
      <p className="caption">{groupMeta(group)}</p>
      {group.isActive
        ? <p className="hint-box caption">{strings.activeGroupNote}</p>
        : <Button variant="secondary" onClick={() => onActivate(group)}>{strings.makeActive}</Button>}
      {shared ? <SharedGroupPanels me={me} group={group} notify={notify} /> : <p className="hint-box caption">{strings.personalGroupNote}</p>}
      {group.isOwner
        ? (
          <ul className="settings-list">
            <li><SettingsRow icon={<EditIcon />} label={strings.renameGroup} onClick={() => setSheet('rename')} /></li>
            <li><SettingsRow icon={<CoinIcon />} label={strings.currencyLabel} value={currencyBadge(group.currency)} onClick={() => setSheet('currency')} /></li>
            {shared && <li><SettingsRow icon={<TrashIcon />} label={strings.deleteGroup} danger onClick={() => setSheet('delete')} /></li>}
          </ul>
        )
        : <p className="caption centered">{strings.ownerOnlyHint}</p>}
      {sheet === 'rename' && <RenameSheet group={group} onSaved={(next) => updated(next, strings.groupRenamedToast)} onClose={() => setSheet(null)} />}
      {sheet === 'currency' && <CurrencySheet group={group} onSaved={(next) => updated(next, strings.currencyChangedToast)} onClose={() => setSheet(null)} />}
      {sheet === 'delete' && (
        <ConfirmSheet
          title={strings.deleteGroupQuestion(group.name)}
          confirmLabel={strings.deleteGroup}
          onConfirm={() => deleteGroup(group.id)}
          onDone={() => onDeleted(group)}
          onClose={() => setSheet(null)}
        >
          <p className="hint">{strings.deleteGroupWarning(group.memberCount)}</p>
          <p className="hint negative">{strings.irreversible}</p>
        </ConfirmSheet>
      )}
    </>
  )
}

function SharedGroupPanels({ me, group, notify }: { me: Me; group: Group; notify: Notify }) {
  const loadMembers = useCallback((signal: AbortSignal) => fetchMembers(group.id, signal), [group.id])
  const loadInvite = useCallback((signal: AbortSignal) => fetchInvite(group.id, signal), [group.id])
  const members = useAsync(loadMembers)
  const invite = useAsync(loadInvite)

  return (
    <>
      <section className="well group-panel">
        <h2 className="section-title">{strings.membersTitle}</h2>
        {members.state.status === 'loading' && <SkeletonRows count={MEMBER_SKELETON_ROWS} />}
        {members.state.status === 'error' && <LoadError message={members.state.message} onRetry={members.reload} />}
        {members.state.status === 'ready' && <MembersMoment members={members.state.data} userId={me.userId} />}
        {members.state.status === 'ready' && (
          <ul className="member-list">
            {members.state.data.map((member) => (
              <li key={member.userId} className="member-item">
                <span className="avatar" aria-hidden="true">{initialOf(member.name)}</span>
                <span className="row-title">{member.name}</span>
                {member.userId === me.userId && <span className="caption">{strings.youMark}</span>}
                {member.isOwner && <span className="badge">{strings.ownerMark}</span>}
              </li>
            ))}
          </ul>
        )}
      </section>
      <section className="well group-panel">
        <h2 className="section-title">{strings.inviteTitle(group.name)}</h2>
        {invite.state.status === 'loading' && <SkeletonRows count={1} />}
        {invite.state.status === 'error' && <LoadError message={invite.state.message} onRetry={invite.reload} />}
        {invite.state.status === 'ready' && <InviteActions url={invite.state.data.url} groupName={group.name} notify={notify} />}
      </section>
    </>
  )
}

function MembersMoment({ members, userId }: { members: Member[]; userId: number }) {
  const shown = members.slice(0, MAX_GROUP_BALLS)
  const you = Math.max(shown.findIndex((member) => member.userId === userId), 0)
  return (
    <Moment
      key={shown.map((member) => member.userId).join()}
      kind="group"
      input={{ initials: shown.map((member) => initialOf(member.name)), you }}
      className="group-moment"
      fallback={null}
    />
  )
}

function InviteActions({ url, groupName, notify }: { url: string; groupName: string; notify: Notify }) {
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(url)
      notify({ title: strings.linkCopiedToast })
    } catch {
      notify({ title: strings.copyFailed })
    }
  }

  const share = () => openExternalLink(`${SHARE_URL}?${new URLSearchParams({ url, text: strings.shareText(groupName) })}`)

  return (
    <>
      <p className="invite-link">{url}</p>
      <div className="invite-actions">
        <Button variant="secondary" size="small" onClick={() => void copy()}><CopyIcon />{strings.copyLink}</Button>
        <Button size="small" onClick={share}><SendIcon />{strings.shareLink}</Button>
      </div>
    </>
  )
}
