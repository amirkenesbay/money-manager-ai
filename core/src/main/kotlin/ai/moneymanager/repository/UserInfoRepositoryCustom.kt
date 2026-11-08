package ai.moneymanager.repository

import ai.moneymanager.domain.model.AppHint
import ai.moneymanager.repository.entity.UserInfoEntity
import java.time.LocalDateTime

interface UserInfoRepositoryCustom {
    fun addSeenHint(telegramUserId: Long, hint: AppHint): UserInfoEntity?
    fun setBotBlockedAt(telegramUserId: Long, blockedAt: LocalDateTime?): Boolean
}
