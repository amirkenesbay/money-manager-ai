import type { Group } from '../api/types'
import { strings } from '../strings'
import { currencyBadge } from './currencies'

export const groupMeta = (group: Group): string =>
  group.type === 'PERSONAL'
    ? strings.personalGroupMeta(currencyBadge(group.currency))
    : strings.sharedGroupMeta(group.memberCount, currencyBadge(group.currency))
