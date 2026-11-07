import type { Group } from '../api/types'
import { strings } from '../strings'
import type { TourStep } from './SpotlightTour'

export function tourSteps(group: Group): TourStep[] {
  const invited = group.type === 'SHARED' && !group.isOwner
  return [
    { target: 'summary', title: invited ? strings.welcomeInvitedTitle(group.name) : strings.tourSummaryTitle, body: invited ? strings.welcomeInvitedBody(group.memberCount) : strings.tourSummaryBody },
    { target: 'group', title: strings.tourGroupTitle, body: strings.tourGroupBody },
    { target: 'manual', title: strings.tourManualTitle, body: strings.tourManualBody },
    { target: 'tab-operations', title: strings.operationsTitle, body: strings.tourOperationsBody },
    { target: 'tab-reports', title: strings.reportsTitle, body: strings.tourReportsBody },
    { target: 'tab-more', title: strings.tabMore, body: strings.tourMoreBody },
    { target: 'voice', title: strings.tourVoiceTitle, body: strings.tourVoiceBody },
  ]
}
