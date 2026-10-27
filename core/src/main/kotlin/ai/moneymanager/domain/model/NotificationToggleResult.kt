package ai.moneymanager.domain.model

import ai.moneymanager.repository.entity.NotificationEntity

sealed class NotificationToggleResult {
    data class Updated(val notification: NotificationEntity) : NotificationToggleResult()
    data class LimitReached(val limit: Int) : NotificationToggleResult()
    object NotFound : NotificationToggleResult()
}
