package ai.moneymanager.service

import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.MoneyGroupEntity
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class GroupService(
    private val groupRepository: MoneyGroupRepository,
    private val userRepository: UserInfoRepository,
    private val categoryService: CategoryService
) {

    /**
     * Создает новую группу для совместного учета
     */
    fun createGroup(ownerId: Long, name: String): MoneyGroup {
        val inviteToken = generateInviteToken()
        val groupEntity = MoneyGroupEntity(
            name = name,
            inviteToken = inviteToken,
            ownerTelegramUserId = ownerId,
            memberTelegramUserIds = setOf(ownerId),
            type = GroupType.SHARED
        )

        val savedGroup = groupRepository.save(groupEntity)
        println("✅ Group created: id=${savedGroup.id}, name=$name, ownerId=$ownerId")

        // Добавляем группу к пользователю
        val userEntity = userRepository.findUserInfoEntityByTelegramUserId(ownerId)
        if (userEntity != null) {
            println("📝 Found user entity: id=${userEntity.id}, groupIds=${userEntity.groupIds}")
            val updatedGroupIds = userEntity.groupIds + savedGroup.id!!
            val updatedUser = userEntity.copy(
                groupIds = updatedGroupIds,
                activeGroupId = savedGroup.id
            )
            val savedUser = userRepository.save(updatedUser)
            println("💾 Updated user: id=${savedUser.id}, groupIds=${savedUser.groupIds}, activeGroupId=${savedUser.activeGroupId}")
        } else {
            println("❌ User entity NOT FOUND for ownerId=$ownerId")
        }

        return mapToModel(savedGroup)
    }

    /**
     * Создает личную группу для пользователя (вызывается при регистрации)
     */
    fun createPersonalGroup(userId: Long, userName: String?): MoneyGroup {
        val groupEntity = MoneyGroupEntity(
            name = "Личный учет",
            inviteToken = generateInviteToken(), // Не используется для личных групп
            ownerTelegramUserId = userId,
            memberTelegramUserIds = setOf(userId),
            type = GroupType.PERSONAL
        )

        val savedGroup = groupRepository.save(groupEntity)
        println("✅ Personal group created: id=${savedGroup.id}, ownerId=$userId")

        // Устанавливаем как активную группу
        val userEntity = userRepository.findUserInfoEntityByTelegramUserId(userId)
        if (userEntity != null) {
            val updatedUser = userEntity.copy(
                groupIds = setOf(savedGroup.id!!),
                activeGroupId = savedGroup.id
            )
            userRepository.save(updatedUser)

            // Создаем дефолтные категории для личной группы
            categoryService.createDefaultCategories(savedGroup.id!!)
            println("📋 Created default categories for personal group")
        }

        return mapToModel(savedGroup)
    }

    /**
     * Присоединение к группе по invite token
     */
    fun joinGroup(userId: Long, inviteToken: String): MoneyGroup? {
        val groupEntity = groupRepository.findByInviteToken(inviteToken) ?: return null

        // Проверяем, не состоит ли пользователь уже в группе
        if (groupEntity.memberTelegramUserIds.contains(userId)) {
            return mapToModel(groupEntity)
        }

        // Добавляем пользователя в группу
        val updatedGroup = groupEntity.copy(
            memberTelegramUserIds = groupEntity.memberTelegramUserIds + userId
        )
        val savedGroup = groupRepository.save(updatedGroup)

        // Обновляем пользователя
        val userEntity = userRepository.findUserInfoEntityByTelegramUserId(userId)
        if (userEntity != null) {
            val updatedUser = userEntity.copy(
                groupIds = userEntity.groupIds + savedGroup.id!!,
                activeGroupId = savedGroup.id
            )
            userRepository.save(updatedUser)
        }

        return mapToModel(savedGroup)
    }

    /**
     * Получить группу по ID
     */
    fun getGroup(groupId: ObjectId): MoneyGroup? {
        return groupRepository.findById(groupId)
            .map { mapToModel(it) }
            .orElse(null)
    }

    /**
     * Получить группу по invite token
     */
    fun getGroupByToken(inviteToken: String): MoneyGroup? {
        return groupRepository.findByInviteToken(inviteToken)?.let { mapToModel(it) }
    }

    /**
     * Получить все группы пользователя
     */
    fun getUserGroups(userId: Long): List<MoneyGroup> {
        return groupRepository.findByMemberTelegramUserIdsContaining(userId)
            .map { mapToModel(it) }
    }

    /**
     * Переключить активную группу пользователя
     */
    fun switchActiveGroup(userId: Long, groupId: ObjectId) {
        val userEntity = userRepository.findUserInfoEntityByTelegramUserId(userId)
        if (userEntity != null && userEntity.groupIds.contains(groupId)) {
            val updatedUser = userEntity.copy(activeGroupId = groupId)
            userRepository.save(updatedUser)
        }
    }

    /**
     * Получить активную группу пользователя
     */
    fun getActiveGroup(userId: Long): MoneyGroup? {
        val userEntity = userRepository.findUserInfoEntityByTelegramUserId(userId) ?: return null
        val activeGroupId = userEntity.activeGroupId ?: return null
        return getGroup(activeGroupId)
    }

    /**
     * Обновить название группы (только владелец может изменить)
     */
    fun updateGroupName(userId: Long, groupId: ObjectId, newName: String): MoneyGroup? {
        val groupEntity = groupRepository.findById(groupId).orElse(null) ?: return null

        // Проверяем, является ли пользователь владельцем
        if (groupEntity.ownerTelegramUserId != userId) {
            return null
        }

        // Обновляем название
        val updatedGroup = groupEntity.copy(name = newName)
        val savedGroup = groupRepository.save(updatedGroup)

        return mapToModel(savedGroup)
    }

    /**
     * Удалить группу (только владелец может удалить)
     */
    fun deleteGroup(userId: Long, groupId: ObjectId): Boolean {
        val groupEntity = groupRepository.findById(groupId).orElse(null) ?: return false

        // Проверяем, является ли пользователь владельцем
        if (groupEntity.ownerTelegramUserId != userId) {
            return false
        }

        // Удаляем все категории группы (каскадное удаление)
        val deletedCategoriesCount = categoryService.deleteAllCategoriesForGroup(groupId)
        println("🗑 Deleted $deletedCategoriesCount categories for group $groupId")

        // Удаляем группу
        groupRepository.delete(groupEntity)

        // Удаляем группу из всех пользователей
        groupEntity.memberTelegramUserIds.forEach { memberId ->
            val memberEntity = userRepository.findUserInfoEntityByTelegramUserId(memberId)
            if (memberEntity != null) {
                val updatedGroupIds = memberEntity.groupIds - groupId
                val updatedActiveGroupId = if (memberEntity.activeGroupId == groupId) {
                    // Если удаляемая группа была активной, выбираем другую
                    updatedGroupIds.firstOrNull()
                } else {
                    memberEntity.activeGroupId
                }

                val updatedMember = memberEntity.copy(
                    groupIds = updatedGroupIds,
                    activeGroupId = updatedActiveGroupId
                )
                userRepository.save(updatedMember)
            }
        }

        return true
    }

    /**
     * Генерация уникального токена приглашения
     */
    private fun generateInviteToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        return (1..9)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Маппинг Entity -> Model
     */
    private fun mapToModel(entity: MoneyGroupEntity): MoneyGroup {
        return MoneyGroup(
            id = entity.id,
            name = entity.name,
            inviteToken = entity.inviteToken,
            ownerTelegramUserId = entity.ownerTelegramUserId,
            memberTelegramUserIds = entity.memberTelegramUserIds,
            type = entity.type
        )
    }
}