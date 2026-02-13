package ai.moneymanager.service.impl

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.UserInfoEntity
import ai.moneymanager.service.UserInfoService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.objects.User

@Service
class UserInfoServiceImpl(
    private val userRepository: UserInfoRepository,
    @Value("\${chat-machinist.bot.token}")
    private val botToken: String
) : UserInfoService, DefaultAbsSender(
    DefaultBotOptions(), botToken
) {

    /**
     * Получить информацию о юзере
     */
    override fun getUserInfo(telegramUserInfo: User): UserInfo {
        val userInfo = findUser(telegramUserInfo)

        if (userInfo == null) {
            val entity = userRepository.save(mapUserModel(telegramUserInfo))
            return mapEntity(entity)
        }

        return mapEntity(userInfo)
    }

    /**
     * Получить юзера по телеграм id
     */
    override fun getUserInfoByTelegramId(telegramUserId: Long): UserInfo? {
        val entity = userRepository.findUserInfoEntityByTelegramUserId(telegramUserId)
        return entity?.let { mapEntity(it) }
    }

    private fun findUser(user: User): UserInfoEntity? {
        if (user.userName != null) {
            getByUsername(user.userName)
            return userRepository.findUserInfoEntityByUsername(user.userName)
        }

        return userRepository.findUserInfoEntityByTelegramUserId(user.id)
    }

    private fun getByUsername(username: String): UserInfoEntity? {
        return userRepository.findUserInfoEntityByUsername(username)
    }

    private fun mapUserModel(telegramUserInfo: User): UserInfoEntity {
        return UserInfoEntity(
            username = telegramUserInfo.userName,
            firstName = telegramUserInfo.firstName,
            lastName = telegramUserInfo.lastName,
            telegramUserId = telegramUserInfo.id,
            languageCode = telegramUserInfo.languageCode
        )
    }

    private fun mapEntity(it: UserInfoEntity): UserInfo {
        return UserInfo(
            id = it.id!!,
            username = it.username,
            firstName = it.firstName,
            lastName = it.lastName,
            telegramUserId = it.telegramUserId,
            languageCode = it.languageCode,
            activeGroupId = it.activeGroupId,
            groupIds = it.groupIds
        )
    }
}