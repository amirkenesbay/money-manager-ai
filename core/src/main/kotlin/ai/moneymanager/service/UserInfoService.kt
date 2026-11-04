package ai.moneymanager.service

import ai.moneymanager.domain.event.NewUserRegistered
import ai.moneymanager.domain.model.AppHint
import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.GroupNoticeLevel
import org.bson.types.ObjectId
import ai.moneymanager.domain.model.formatUserDisplayName
import ai.moneymanager.domain.model.SubscriptionTier
import ai.moneymanager.domain.model.TelegramProfile
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.UserInfoEntity
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserInfoService(
    private val userRepository: UserInfoRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    fun getOrCreate(profile: TelegramProfile): UserInfo {
        val existing = userRepository.findFirstByTelegramUserIdOrderByIdAsc(profile.telegramUserId)
        if (existing != null) return mapEntity(existing)

        val saved = try {
            userRepository.save(mapUserModel(profile))
        } catch (exception: DuplicateKeyException) {
            return mapEntity(checkNotNull(userRepository.findFirstByTelegramUserIdOrderByIdAsc(profile.telegramUserId)))
        }
        val newUser = mapEntity(saved)
        eventPublisher.publishEvent(NewUserRegistered(newUser))
        return newUser
    }

    fun getUserInfoByTelegramId(telegramUserId: Long): UserInfo? {
        val entity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(telegramUserId)
        return entity?.let { mapEntity(it) }
    }

    fun displayNames(telegramUserIds: Set<Long>): Map<Long, String> =
        telegramUserIds.associateWith { id -> formatUserDisplayName(getUserInfoByTelegramId(id), id) }

    fun updateTimezone(telegramUserId: Long, timezone: String): UserInfo? {
        val entity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(telegramUserId) ?: return null
        val updated = userRepository.save(entity.copy(timezone = timezone))
        return mapEntity(updated)
    }

    fun updateLanguage(telegramUserId: Long, language: String): UserInfo? {
        val entity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(telegramUserId) ?: return null
        val updated = userRepository.save(entity.copy(language = language))
        return mapEntity(updated)
    }

    fun markOnboardingCompleted(telegramUserId: Long): UserInfo? {
        val entity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(telegramUserId) ?: return null
        if (entity.onboardingCompleted) return mapEntity(entity)
        val updated = userRepository.save(entity.copy(onboardingCompleted = true))
        return mapEntity(updated)
    }

    fun markMiniAppIntroduced(telegramUserId: Long) {
        val entity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(telegramUserId) ?: return
        if (entity.miniAppIntroduced) return
        userRepository.save(entity.copy(miniAppIntroduced = true))
    }

    fun markHintSeen(telegramUserId: Long, hint: AppHint): UserInfo? {
        val entity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(telegramUserId) ?: return null
        if (hint in entity.seenHints) return mapEntity(entity)
        return mapEntity(userRepository.save(entity.copy(seenHints = entity.seenHints + hint)))
    }

    fun setDigestMuted(telegramUserId: Long, kind: DigestKind, muted: Boolean): UserInfo? {
        val entity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(telegramUserId) ?: return null
        val mutedDigests = if (muted) entity.mutedDigests + kind else entity.mutedDigests - kind
        if (mutedDigests == entity.mutedDigests) return mapEntity(entity)
        return mapEntity(userRepository.save(entity.copy(mutedDigests = mutedDigests)))
    }

    fun setGroupNoticeLevel(telegramUserId: Long, groupId: ObjectId, level: GroupNoticeLevel): UserInfo? {
        val entity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(telegramUserId) ?: return null
        return mapEntity(userRepository.save(entity.copy(groupNotices = entity.groupNotices + (groupId.toHexString() to level))))
    }

    fun digestRecipients(): List<UserInfo> =
        userRepository.findByActiveGroupIdIsNotNullAndTelegramUserIdIsNotNull().map { mapEntity(it) }

    fun findUserByUsernameOrTelegramId(identifier: String): UserInfo? {
        val entity = identifier.toLongOrNull()
            ?.let { userRepository.findFirstByTelegramUserIdOrderByIdAsc(it) }
            ?: userRepository.findFirstByUsernameOrderByIdAsc(identifier.removePrefix("@"))
        return entity?.let { mapEntity(it) }
    }

    fun activateSubscription(telegramUserId: Long, days: Long): UserInfo? {
        val entity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(telegramUserId) ?: return null
        val currentExpiry = entity.subscriptionExpiresAt?.takeIf { it.isAfter(LocalDateTime.now()) }
        val newExpiry = (currentExpiry ?: LocalDateTime.now()).plusDays(days)
        val updated = userRepository.save(
            entity.copy(subscriptionTier = SubscriptionTier.PAID, subscriptionExpiresAt = newExpiry)
        )
        return mapEntity(updated)
    }

    private fun mapUserModel(profile: TelegramProfile): UserInfoEntity {
        return UserInfoEntity(
            username = profile.username,
            firstName = profile.firstName,
            lastName = profile.lastName,
            telegramUserId = profile.telegramUserId,
            languageCode = profile.languageCode
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
            language = it.language,
            activeGroupId = it.activeGroupId,
            groupIds = it.groupIds,
            timezone = it.timezone,
            onboardingCompleted = it.onboardingCompleted,
            miniAppIntroduced = it.miniAppIntroduced,
            seenHints = it.seenHints,
            mutedDigests = it.mutedDigests,
            groupNotices = it.groupNotices,
            subscriptionTier = it.subscriptionTier,
            subscriptionExpiresAt = it.subscriptionExpiresAt
        )
    }
}
