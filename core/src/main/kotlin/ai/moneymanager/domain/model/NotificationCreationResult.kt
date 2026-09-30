package ai.moneymanager.domain.model

import ai.moneymanager.repository.entity.NotificationEntity

sealed class NotificationCreationResult {
    data class Created(val notification: NotificationEntity) : NotificationCreationResult()
    data class LimitReached(val limit: Int) : NotificationCreationResult()
}
