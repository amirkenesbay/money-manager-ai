package ai.moneymanager.domain.model

import org.bson.types.ObjectId

/**
 * Группа для совместного учета финансов
 */
data class MoneyGroup(
    val id: ObjectId? = null,
    val name: String,
    val inviteToken: String,
    val ownerTelegramUserId: Long,
    val memberTelegramUserIds: Set<Long>,
    val type: GroupType
)

/**
 * Тип группы
 */
enum class GroupType {
    /**
     * Личный учет (создается автоматически при регистрации)
     */
    PERSONAL,

    /**
     * Совместный учет (2+ человек)
     */
    SHARED
}