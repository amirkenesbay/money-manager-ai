package ai.moneymanager.service

import ai.moneymanager.domain.model.UserInfo
import org.springframework.stereotype.Service

const val PERSONAL_GROUP_NAME_KEY = "group.default.personal_name"

@Service
class OnboardingService(
    private val userInfoService: UserInfoService,
    private val groupService: GroupService,
    private val localizationService: LocalizationService,
) {
    fun selectLanguage(telegramUserId: Long, language: String): UserInfo? {
        userInfoService.updateLanguage(telegramUserId, language) ?: return null
        ensurePersonalGroup(telegramUserId, language)
        return userInfoService.getUserInfoByTelegramId(telegramUserId)
    }

    fun ensurePersonalGroup(telegramUserId: Long, language: String) {
        if (groupService.getUserGroups(telegramUserId).isNotEmpty()) return
        val personalName = localizationService.t(PERSONAL_GROUP_NAME_KEY, language)
        groupService.createPersonalGroup(telegramUserId, personalName, language)
    }
}
