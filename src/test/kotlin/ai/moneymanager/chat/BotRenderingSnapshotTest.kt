package ai.moneymanager.chat

import ai.moneymanager.chat.reply.finance.FinanceHistoryFormatter
import ai.moneymanager.chat.reply.finance.FinanceReportFormatter
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.NotificationEntity
import ai.moneymanager.service.FakeFinanceOperationRepository
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.web.ENGLISH
import ai.moneymanager.web.testLocalizationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

private const val RUSSIAN = "ru"
private val SNAPSHOT_DIR: Path = Path.of("src/test/resources/snapshots/bot-rendering")
private val JUNE_START: LocalDate = LocalDate.of(2026, 6, 1)
private val JUNE_END: LocalDate = LocalDate.of(2026, 6, 30)
private val MAY_START: LocalDate = LocalDate.of(2026, 5, 1)
private val FIXED_CLOCK: Clock = Clock.fixed(LocalDate.of(2026, 6, 15).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
private const val OWNER_ID = 1L
private const val MEMBER_ID = 2L

class BotRenderingSnapshotTest {
    private val groupId = ObjectId("6650a1b2c3d4e5f6a7b8c9d0")
    private val groceries = Category(ObjectId("6650a1b2c3d4e5f6a7b8c9d1"), "Продукты", "🛒", CategoryType.EXPENSE, groupId)
    private val taxi = Category(ObjectId("6650a1b2c3d4e5f6a7b8c9d2"), "Такси", null, CategoryType.EXPENSE, groupId)
    private val localization = testLocalizationService()
    private val repository = FakeFinanceOperationRepository().apply { operations = operations() }
    private val historyService = FinanceHistoryFormatter(FinanceHistoryService(repository), localization)
    private val reportService = FinanceReportFormatter(FinanceReportService(repository, FIXED_CLOCK), localization)


    @Test
    fun `history reports`() {
        assertSnapshot("history-month-ru", historyService.generateReport(groupId, JUNE_START, JUNE_END, Currency.KZT, RUSSIAN))
        assertSnapshot("history-range-en", historyService.generateReport(groupId, LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 20), Currency.USD, ENGLISH))
        assertSnapshot("history-itemized-ru", historyService.generateReport(groupId, JUNE_START, JUNE_END, Currency.KZT, RUSSIAN, categoryFilter = "Продукты"))
        assertSnapshot("history-expenses-only-ru", historyService.generateReport(groupId, JUNE_START, JUNE_END, Currency.RUB, RUSSIAN, typeFilter = CategoryType.EXPENSE))
        repository.operations = emptyList()
        assertSnapshot("history-empty-ru", historyService.generateReport(groupId, JUNE_START, JUNE_END, Currency.KZT, RUSSIAN))
    }

    @Test
    fun `finance reports`() {
        assertSnapshot("report-comparison-ru", reportService.generateComparisonReport(groupId, MAY_START, Currency.KZT, RUSSIAN))
        assertSnapshot("report-analytics-ru", reportService.generateAnalyticsReport(groupId, JUNE_START, Currency.KZT, RUSSIAN))
        assertSnapshot("report-analytics-en", reportService.generateAnalyticsReport(groupId, JUNE_START, Currency.EUR, ENGLISH))
        assertSnapshot("report-members-ru", reportService.generateMembersReport(groupId, JUNE_START, mapOf(OWNER_ID to "Амир", MEMBER_ID to "@esen"), Currency.KZT, RUSSIAN))
        assertSnapshot("report-category-ru", reportService.generateCategoryReport(groupId, groceries.id!!, groceries.icon, groceries.name, Currency.KZT, RUSSIAN))
    }

    @Test
    fun `ai confirmations`() {
        val operation = operations().first()
        val actions = mapOf(
            "category-create" to AiPendingAction.CategoryAction.Create("Кафе", CategoryType.EXPENSE, "☕"),
            "category-delete" to AiPendingAction.CategoryAction.Delete(taxi),
            "category-rename" to AiPendingAction.CategoryAction.Rename(groceries, "Еда"),
            "category-change-icon" to AiPendingAction.CategoryAction.ChangeIcon(taxi, "🚕"),
            "category-delete-all" to AiPendingAction.CategoryAction.DeleteAll(groupId, 7),
            "group-create" to AiPendingAction.GroupAction.Create("Семья"),
            "group-delete" to AiPendingAction.GroupAction.Delete(MoneyGroup(groupId, "Семья", "abc123xyz", OWNER_ID, setOf(OWNER_ID, MEMBER_ID), GroupType.SHARED)),
            "notification-create" to AiPendingAction.NotificationAction.CreateDaily("Записать траты", 21, 5),
            "notification-delete" to AiPendingAction.NotificationAction.Delete(notification()),
            "operation-delete" to AiPendingAction.RecentOperationAction.Delete(operation),
            "operation-edit" to AiPendingAction.RecentOperationAction.Edit(operation, BigDecimal("4200"), taxi, LocalDate.of(2026, 6, 3)),
            "transaction-add" to addExpense(),
            "transaction-add-new-category" to AiPendingAction.TransactionAction.AddWithNewCategory(groupId, OWNER_ID, CategoryType.INCOME, 150000.0, "Фриланс", "💻", "заказ <срочный>", JUNE_START),
        )
        val rendered = actions.entries.joinToString("\n\n") { (name, action) ->
            "[$name]\n${action.describe(localization, Currency.KZT, RUSSIAN)}\n${action.describe(localization, Currency.USD, ENGLISH)}"
        }
        assertSnapshot("ai-confirmations", rendered)

        val batchItems = listOf(addExpense(), actions.getValue("transaction-add-new-category") as AiPendingAction.TransactionAction)
            .joinToString("\n") { it.describeBatchItem(localization, Currency.KZT, RUSSIAN) }
        assertSnapshot("ai-batch-items", batchItems)
    }

    private fun addExpense() = AiPendingAction.TransactionAction.Add(
        groupId, OWNER_ID, CategoryType.EXPENSE, 3500.5, groceries, "Магнум & <сыр>", JUNE_START,
    )

    private fun operations(): List<FinanceOperationEntity> = listOf(
        operation(CategoryType.EXPENSE, "3500.00", groceries, LocalDate.of(2026, 6, 12), "Магнум & <сыр>", OWNER_ID),
        operation(CategoryType.EXPENSE, "12000.50", taxi, LocalDate.of(2026, 6, 14), null, MEMBER_ID),
        operation(CategoryType.EXPENSE, "800", groceries, LocalDate.of(2026, 6, 20), "хлеб", MEMBER_ID),
        operation(CategoryType.INCOME, "450000", Category(ObjectId("6650a1b2c3d4e5f6a7b8c9d3"), "Зарплата", "💰", CategoryType.INCOME, groupId), LocalDate.of(2026, 6, 5), null, OWNER_ID),
        operation(CategoryType.EXPENSE, "5000", groceries, LocalDate.of(2026, 5, 18), null, OWNER_ID),
    )

    private fun operation(type: CategoryType, amount: String, category: Category, date: LocalDate, description: String?, creator: Long) =
        FinanceOperationEntity(
            id = ObjectId(),
            groupId = groupId,
            creatorId = creator,
            type = type,
            amount = BigDecimal(amount),
            categoryId = category.id!!,
            categoryName = category.name,
            categoryIcon = category.icon,
            operationDate = date,
            description = description,
        )

    private fun notification() = NotificationEntity(
        telegramUserId = OWNER_ID,
        name = "Оплатить интернет",
        icon = "🌐",
        frequencyType = FrequencyType.MONTHLY,
        hour = 10,
        minute = 0,
        dayOfMonth = 5,
        nextFireTime = LocalDateTime.of(2026, 7, 5, 10, 0),
    )

    private fun assertSnapshot(name: String, actual: String) {
        val file = SNAPSHOT_DIR.resolve("$name.txt")
        if (Files.notExists(file)) {
            Files.createDirectories(SNAPSHOT_DIR)
            Files.writeString(file, actual)
            error("Snapshot $name recorded — review it and rerun")
        }
        assertThat(actual).describedAs("Rendering of $name changed").isEqualTo(Files.readString(file))
    }
}
