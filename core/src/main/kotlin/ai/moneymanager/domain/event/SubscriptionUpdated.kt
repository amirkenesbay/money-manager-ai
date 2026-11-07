package ai.moneymanager.domain.event

import ai.moneymanager.domain.model.PaidSubscription
import ai.moneymanager.domain.model.UserInfo

data class SubscriptionUpdated(val user: UserInfo, val subscription: PaidSubscription)
