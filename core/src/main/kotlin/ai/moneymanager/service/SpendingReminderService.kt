package ai.moneymanager.service

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.FALLBACK_TIMEZONE
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.repository.FinanceOperationRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private const val QUICK_CATEGORY_COUNT = 3
private const val QUICK_CATEGORY_DAYS = 30L

@Service
class SpendingReminderService(
    private val operationRepository: FinanceOperationRepository,
    private val categoryService: CategoryService,
) {
    fun recordedToday(user: UserInfo): Boolean {
        val telegramUserId = user.telegramUserId ?: return false
        val zone = reminderZone(user)
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        return operationRepository.existsByCreatorIdAndAuditInfoCreatedAtGreaterThanEqual(telegramUserId, startOfDay)
    }

    fun quickCategories(user: UserInfo): List<Category> {
        val telegramUserId = user.telegramUserId ?: return emptyList()
        val groupId = user.activeGroupId ?: return emptyList()
        val since = LocalDateTime.now().minusDays(QUICK_CATEGORY_DAYS)
        val uses = operationRepository
            .findByCreatorIdAndGroupIdAndTypeAndAuditInfoCreatedAtGreaterThanEqual(telegramUserId, groupId, CategoryType.EXPENSE, since)
            .groupingBy { it.categoryId }
            .eachCount()
        return categoryService.getCategoriesByGroupAndType(groupId, CategoryType.EXPENSE)
            .sortedByDescending { uses[it.id] ?: 0 }
            .take(QUICK_CATEGORY_COUNT)
    }

    private fun reminderZone(user: UserInfo): ZoneId = if (user.timezone == null) ZoneId.of(FALLBACK_TIMEZONE) else user.zone
}
