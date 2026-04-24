package ai.moneymanager.service

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
    private val categoryService: CategoryService
) {

    private val log = LoggerFactory.getLogger(GroupService::class.java)

    companion object {
        private const val INVITE_TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private const val INVITE_TOKEN_LENGTH = 9
        private const val MAX_TOKEN_RETRIES = 10
    }

    fun createGroup(ownerId: Long, name: String): MoneyGroup? {
        if (groupRepository.findByOwnerIdAndName(ownerId, name) != null) {
            return null
        }
        return saveGroupAndUpdateUser(name, ownerId, GroupType.SHARED) { existing, newId ->
            existing + newId
        }
    }

    fun createPersonalGroup(userId: Long, name: String): MoneyGroup {
        val group = saveGroupAndUpdateUser(name, userId, GroupType.PERSONAL) { _, newId ->
            setOf(newId)
        }
        categoryService.createDefaultCategories(group.id!!)
        log.info("Created default categories for personal group: ${group.id}")
        return group
    }

    fun joinGroup(userId: Long, inviteToken: String): MoneyGroup? {
        val groupEntity = groupRepository.findByInviteToken(inviteToken) ?: return null

        if (groupEntity.memberIds.contains(userId)) {
            return mapToModel(groupEntity)
        }

        val updatedGroup = groupEntity.copy(memberIds = groupEntity.memberIds + userId)
        val savedGroup = groupRepository.save(updatedGroup)

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
        val userEntity = userRepository.findUserInfoEntityByTelegramUserId(userId)
        if (userEntity != null && userEntity.groupIds.contains(groupId)) {
            userRepository.save(userEntity.copy(activeGroupId = groupId))
        }
    }

    fun getActiveGroup(userId: Long): MoneyGroup? {
        val userEntity = userRepository.findUserInfoEntityByTelegramUserId(userId) ?: return null
        val activeGroupId = userEntity.activeGroupId ?: return null
        return getGroup(activeGroupId)
    }

    fun setInitialBalance(groupId: ObjectId, amount: BigDecimal): MoneyGroup? {
        val groupEntity = groupRepository.findById(groupId).orElse(null) ?: return null
        val saved = groupRepository.save(groupEntity.copy(initialBalance = amount))
        log.info("Initial balance set: groupId=$groupId, amount=$amount")
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
        return mapToModel(savedGroup)
    }

    fun deleteGroup(userId: Long, groupId: ObjectId): Boolean {
        val groupEntity = groupRepository.findById(groupId).orElse(null) ?: return false

        if (groupEntity.ownerId != userId) {
            return false
        }

        val deletedCategoriesCount = categoryService.deleteAllCategoriesForGroup(groupId)
        log.info("Deleted $deletedCategoriesCount categories for group $groupId")

        groupRepository.delete(groupEntity)

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

    private fun saveGroupAndUpdateUser(
        name: String,
        ownerId: Long,
        type: GroupType,
        buildGroupIds: (Set<ObjectId>, ObjectId) -> Set<ObjectId>
    ): MoneyGroup {
        val groupEntity = MoneyGroupEntity(
            name = name,
            inviteToken = generateInviteToken(),
            ownerId = ownerId,
            memberIds = setOf(ownerId),
            type = type
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
        val userEntity = userRepository.findUserInfoEntityByTelegramUserId(userId)
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
            initialBalance = entity.initialBalance
        )
    }
}
