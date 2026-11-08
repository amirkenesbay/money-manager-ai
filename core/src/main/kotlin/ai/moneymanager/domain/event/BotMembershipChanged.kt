package ai.moneymanager.domain.event

data class BotMembershipChanged(val telegramUserId: Long, val blocked: Boolean)
