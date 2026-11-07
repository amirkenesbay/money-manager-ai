package ai.moneymanager.repository

import ai.moneymanager.domain.model.AppHint
import ai.moneymanager.repository.entity.UserInfoEntity

interface UserInfoRepositoryCustom {
    fun addSeenHint(telegramUserId: Long, hint: AppHint): UserInfoEntity?
}
