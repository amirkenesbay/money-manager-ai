package ai.moneymanager.service

import ai.moneymanager.domain.event.GroupDeleted
import ai.moneymanager.domain.event.GroupSettingChange
import ai.moneymanager.domain.event.GroupSettingChanged
import ai.moneymanager.domain.event.MemberJoined
import org.springframework.context.ApplicationEventPublisher
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.MONTH_START_DAYS
import ai.moneymanager.domain.model.GroupCreationResult
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.MoneyGroupEntity
import ai.moneymanager.repository.entity.UserInfoEntity
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.security.SecureRandom

@Service
class GroupService(
    private val groupRepository: MoneyGroupRepository,
    private val userRepository: UserInfoRepository,
    private val categoryService: CategoryService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(GroupService::class.java)

    companion object {
        private const val INVITE_TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private const val INVITE_TOKEN_LENGTH = 9
        private const val MAX_TOKEN_RETRIES = 10
    }

    fun createGroup(ownerId: Long, name: String, currency: Currency = Currency.DEFAULT, maxOwnedSharedGroups: Int? = null): GroupCreationResult {
        if (groupRepository.findByOwnerIdAndName(ownerId, name) != null) {
            return GroupCreationResult.Duplicate
        }

        if (maxOwnedSharedGroups != null) {
            val ownedCount = groupRepository.countByOwnerIdAndType(ownerId, GroupType.SHARED)
            if (ownedCount >= maxOwnedSharedGroups) {
                return GroupCreationResult.LimitReached(maxOwnedSharedGroups)
            }
        }

        val group = saveGroupAndUpdateUser(name, ownerId, GroupType.SHARED, currency) { existing, newId ->
            existing + newId
        }
        seedDefaultCategories(group, languageOf(ownerId))
        return GroupCreationResult.Created(group)
    }

    fun createPersonalGroup(userId: Long, name: String, language: String, currency: Currency = Currency.DEFAULT): MoneyGroup {
        val group = saveGroupAndUpdateUser(name, userId, GroupType.PERSONAL, currency) { _, newId ->
            setOf(newId)
        }
        seedDefaultCategories(group, language)
        return group
    }

    fun joinGroup(userId: Long, inviteToken: String): MoneyGroup? {
        val groupEntity = groupRepository.findByInviteToken(inviteToken) ?: return null

        if (groupEntity.memberIds.contains(userId)) {
            return mapToModel(groupEntity)
        }

        val updatedGroup = groupEntity.copy(memberIds = groupEntity.memberIds + userId)
        val savedGroup = groupRepository.save(updatedGroup)
        eventPublisher.publishEvent(MemberJoined(checkNotNull(savedGroup.id), userId))

        updateUserGroupInfo(userId) { entity ->
            entity.copy(
                groupIds = entity.groupIds + savedGroup.id!!,
                activeGroupId = savedGroup.id
            )
        }

        return mapToModel(savedGroup)
    }

    fun getGroup(groupId: ObjectId): MoneyGroup? {
        return groupRepository.findById(groupId)
            .map { mapToModel(it) }
            .orElse(null)
    }

    fun getGroupByToken(inviteToken: String): MoneyGroup? {
        return groupRepository.findByInviteToken(inviteToken)?.let { mapToModel(it) }
    }

    fun getUserGroups(userId: Long): List<MoneyGroup> {
        return groupRepository.findByMemberIdsContaining(userId)
            .map { mapToModel(it) }
    }

    fun switchActiveGroup(userId: Long, groupId: ObjectId) {
        val userEntity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(userId)
        if (userEntity != null && userEntity.groupIds.contains(groupId)) {
            userRepository.save(userEntity.copy(activeGroupId = groupId))
        }
    }

    fun getActiveGroup(userId: Long): MoneyGroup? {
        val userEntity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(userId) ?: return null
        val activeGroupId = userEntity.activeGroupId ?: return null
        return getGroup(activeGroupId)
    }

    fun setInitialBalance(actorId: Long, groupId: ObjectId, amount: BigDecimal): MoneyGroup? {
        val groupEntity = groupRepository.findById(groupId).orElse(null) ?: return null
        val saved = groupRepository.save(groupEntity.copy(initialBalance = amount))
        log.info("Initial balance set: groupId=$groupId, amount=$amount")
        if (groupEntity.initialBalance.compareTo(amount) != 0) {
            publishSetting(groupId, actorId, GroupSettingChange.InitialBalanceChanged(groupEntity.initialBalance, amount))
        }
        return mapToModel(saved)
    }

    fun updateGroupName(userId: Long, groupId: ObjectId, newName: String): MoneyGroup? {
        val groupEntity = groupRepository.findById(groupId).orElse(null) ?: return null

        if (groupEntity.ownerId != userId) {
            return null
        }

        val existing = groupRepository.findByOwnerIdAndName(userId, newName)
        if (existing != null && existing.id != groupId) {
            return null
        }

        val savedGroup = groupRepository.save(groupEntity.copy(name = newName))
        if (groupEntity.name != newName) publishSetting(groupId, userId, GroupSettingChange.Renamed(groupEntity.name, newName))
        return mapToModel(savedGroup)
    }

    fun updateCurrency(userId: Long, groupId: ObjectId, currency: Currency): MoneyGroup? {
        val groupEntity = groupRepository.findById(groupId).orElse(null) ?: return null

        if (groupEntity.ownerId != userId) {
            return null
        }

        val savedGroup = groupRepository.save(groupEntity.copy(currency = currency))
        log.info("Currency updated: groupId=$groupId, currency=$currency")
        if (groupEntity.currency != currency) publishSetting(groupId, userId, GroupSettingChange.CurrencyChanged(groupEntity.currency, currency))
        return mapToModel(savedGroup)
    }

    fun updateMonthStartDay(userId: Long, groupId: ObjectId, day: Int): MoneyGroup? {
        require(day in MONTH_START_DAYS) { "Month start day must be in $MONTH_START_DAYS" }
        val groupEntity = groupRepository.findById(groupId).orElse(null) ?: return null
        if (groupEntity.ownerId != userId) return null
        val saved = groupRepository.save(groupEntity.copy(monthStartDay = day))
        if (groupEntity.monthStartDay != day) publishSetting(groupId, userId, GroupSettingChange.MonthStartChanged(groupEntity.monthStartDay, day))
        return mapToModel(saved)
    }

    fun updateSplitExpenses(userId: Long, groupId: ObjectId, enabled: Boolean): MoneyGroup? {
        val groupEntity = groupRepository.findById(groupId).orElse(null) ?: return null
        if (groupEntity.ownerId != userId || groupEntity.type != GroupType.SHARED) return null
        return mapToModel(groupRepository.save(groupEntity.copy(splitExpenses = enabled)))
    }

    fun deleteGroup(userId: Long, groupId: ObjectId): Boolean {
        val groupEntity = groupRepository.findById(groupId).orElse(null) ?: return false

        if (groupEntity.ownerId != userId) {
            return false
        }

        val deletedCategoriesCount = categoryService.deleteAllCategoriesForGroup(groupId)
        log.info("Deleted $deletedCategoriesCount categories for group $groupId")

        groupRepository.delete(groupEntity)
        eventPublisher.publishEvent(GroupDeleted(groupId, userId, groupEntity.name, groupEntity.memberIds))

        groupEntity.memberIds.forEach { memberId ->
            updateUserGroupInfo(memberId) { entity ->
                val updatedGroupIds = entity.groupIds - groupId
                entity.copy(
                    groupIds = updatedGroupIds,
                    activeGroupId = if (entity.activeGroupId == groupId) {
                        updatedGroupIds.firstOrNull()
                    } else {
                        entity.activeGroupId
                    }
                )
            }
        }

        return true
    }

    private fun seedDefaultCategories(group: MoneyGroup, language: String) {
        categoryService.createDefaultCategories(checkNotNull(group.id), language)
        log.info("Created default categories for group: ${group.id}")
    }

    private fun languageOf(userId: Long): String =
        userRepository.findFirstByTelegramUserIdOrderByIdAsc(userId)?.language ?: LocalizationService.FALLBACK_LANGUAGE

    private fun saveGroupAndUpdateUser(
        name: String,
        ownerId: Long,
        type: GroupType,
        currency: Currency,
        buildGroupIds: (Set<ObjectId>, ObjectId) -> Set<ObjectId>
    ): MoneyGroup {
        val groupEntity = MoneyGroupEntity(
            name = name,
            inviteToken = generateInviteToken(),
            ownerId = ownerId,
            memberIds = setOf(ownerId),
            type = type,
            currency = currency
        )
        val savedGroup = groupRepository.save(groupEntity)
        log.info("Group created: id=${savedGroup.id}, name=$name, type=$type, ownerId=$ownerId")

        updateUserGroupInfo(ownerId) { entity ->
            entity.copy(
                groupIds = buildGroupIds(entity.groupIds, savedGroup.id!!),
                activeGroupId = savedGroup.id
            )
        }

        return mapToModel(savedGroup)
    }

    private fun updateUserGroupInfo(userId: Long, update: (UserInfoEntity) -> UserInfoEntity) {
        val userEntity = userRepository.findFirstByTelegramUserIdOrderByIdAsc(userId)
        if (userEntity != null) {
            userRepository.save(update(userEntity))
        } else {
            log.warn("User entity not found for userId=$userId")
        }
    }

    private fun generateInviteToken(): String {
        val random = SecureRandom()
        repeat(MAX_TOKEN_RETRIES) {
            val token = (1..INVITE_TOKEN_LENGTH)
                .map { INVITE_TOKEN_CHARS[random.nextInt(INVITE_TOKEN_CHARS.length)] }
                .joinToString("")
            if (groupRepository.findByInviteToken(token) == null) {
                return token
            }
            log.warn("Invite token collision detected, retrying...")
        }
        throw IllegalStateException("Failed to generate unique invite token after $MAX_TOKEN_RETRIES attempts")
    }

    private fun mapToModel(entity: MoneyGroupEntity): MoneyGroup {
        return MoneyGroup(
            id = entity.id,
            name = entity.name,
            inviteToken = entity.inviteToken,
            ownerId = entity.ownerId,
            memberIds = entity.memberIds,
            type = entity.type,
            initialBalance = entity.initialBalance,
            currency = entity.currency,
            monthStartDay = entity.monthStartDay,
            splitExpenses = entity.splitExpenses,
        )
    }

    private fun publishSetting(groupId: ObjectId, actorId: Long, change: GroupSettingChange) {
        eventPublisher.publishEvent(GroupSettingChanged(groupId, actorId, change))
    }
}
