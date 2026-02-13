package ai.moneymanager.service

import ai.moneymanager.domain.model.MoneyGroup
import org.bson.types.ObjectId

interface GroupService {
    /**
     * Создает новую группу для совместного учета
     */
    fun createGroup(ownerId: Long, name: String): MoneyGroup

    /**
     * Создает личную группу для пользователя (вызывается при регистрации)
     */
    fun createPersonalGroup(userId: Long, userName: String?): MoneyGroup

    /**
     * Присоединение к группе по invite token
     */
    fun joinGroup(userId: Long, inviteToken: String): MoneyGroup?

    /**
     * Получить группу по ID
     */
    fun getGroup(groupId: ObjectId): MoneyGroup?

    /**
     * Получить группу по invite token
     */
    fun getGroupByToken(inviteToken: String): MoneyGroup?

    /**
     * Получить все группы пользователя
     */
    fun getUserGroups(userId: Long): List<MoneyGroup>

    /**
     * Переключить активную группу пользователя
     */
    fun switchActiveGroup(userId: Long, groupId: ObjectId)

    /**
     * Получить активную группу пользователя
     */
    fun getActiveGroup(userId: Long): MoneyGroup?

    /**
     * Обновить название группы (только владелец может изменить)
     */
    fun updateGroupName(userId: Long, groupId: ObjectId, newName: String): MoneyGroup?

    /**
     * Удалить группу (только владелец может удалить)
     */
    fun deleteGroup(userId: Long, groupId: ObjectId): Boolean


}