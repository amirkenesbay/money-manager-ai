package ai.moneymanager.service

import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.UserInfoEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.objects.User

@Service
class UserInfoService(
    private val userRepository: UserInfoRepository,
    private val groupService: GroupService,
    @Value("\${chat-machinist.bot.token}")
    private val botToken: String
) : DefaultAbsSender(
    DefaultBotOptions(), botToken
) {

    fun getUserInfo(telegramUserInfo: User): UserInfo {
        val userInfo = findUser(telegramUserInfo)

        if (userInfo == null) {
            val entity = userRepository.save(mapUserModel(telegramUserInfo))

            groupService.createPersonalGroup(
                userId = telegramUserInfo.id,
                userName = telegramUserInfo.userName
            )

            val updatedEntity = userRepository.findUserInfoEntityByTelegramUserId(telegramUserInfo.id)
            return mapEntity(updatedEntity ?: entity)
        }

        return mapEntity(userInfo)
    }

    fun getUserInfoByTelegramId(telegramUserId: Long): UserInfo? {
        val entity = userRepository.findUserInfoEntityByTelegramUserId(telegramUserId)
        return entity?.let { mapEntity(it) }
    }

    fun updateTimezone(telegramUserId: Long, timezone: String): UserInfo? {
        val entity = userRepository.findUserInfoEntityByTelegramUserId(telegramUserId) ?: return null
        val updated = userRepository.save(entity.copy(timezone = timezone))
        return mapEntity(updated)
    }

    private fun findUser(user: User): UserInfoEntity? {
        if (user.userName != null) {
            return userRepository.findUserInfoEntityByUsername(user.userName)
        }
        return userRepository.findUserInfoEntityByTelegramUserId(user.id)
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
            groupIds = it.groupIds,
            timezone = it.timezone
        )
    }
}
