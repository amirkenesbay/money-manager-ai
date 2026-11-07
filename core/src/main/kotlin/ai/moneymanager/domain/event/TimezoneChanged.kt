package ai.moneymanager.domain.event

data class TimezoneChanged(val telegramUserId: Long, val timezone: String)
