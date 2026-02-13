package ai.moneymanager.service

import ai.moneymanager.domain.model.UserInfo
import org.telegram.telegrambots.meta.api.objects.User

interface UserInfoService {
    /**
     * Получить информацию о юзере
     */
    fun getUserInfo(telegramUserInfo: User): UserInfo

    /**
     * Получить юзера по телеграм id
     */
    fun getUserInfoByTelegramId(telegramUserId: Long): UserInfo?
}