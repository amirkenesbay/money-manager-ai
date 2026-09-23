package ai.moneymanager.service

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.LocalDate

class FinanceHistoryServiceTest {
    private val groupId = ObjectId()
    private val startDate = LocalDate.of(2026, 7, 1)
    private val endDate = LocalDate.of(2026, 7, 31)

    private val repository = FakeFinanceOperationRepository()
    private val service = FinanceHistoryService(repository, mock(LocalizationService::class.java))

    @Test
    fun `splits operations into income and expense totals`() {
        givenOperations(
            expense(amount = "1000", category = "Продукты", icon = "🛒"),
            expense(amount = "500", category = "Такси", icon = "🚗"),
            income(amount = "300000", category = "Зарплата", icon = "💰"),
        )

        val report = service.buildHistoryReport(groupId, startDate, endDate)

        assertThat(report.totalExpense).isEqualByComparingTo("1500")
        assertThat(report.totalIncome).isEqualByComparingTo("300000")
        assertThat(report.balance).isEqualByComparingTo("298500")
        assertThat(report.isEmpty).isFalse()
        assertThat(report.operations).hasSize(3)
    }

    @Test
    fun `sums categories and orders them from largest to smallest`() {
        givenOperations(
            expense(amount = "300", category = "Продукты", icon = "🛒"),
            expense(amount = "2000", category = "Такси", icon = "🚗"),
            expense(amount = "700", category = "Продукты", icon = "🛒"),
        )

        val report = service.buildHistoryReport(groupId, startDate, endDate)

        assertThat(report.expenseByCategory).hasSize(2)
        assertThat(report.expenseByCategory[0].name).isEqualTo("Такси")
        assertThat(report.expenseByCategory[0].total).isEqualByComparingTo("2000")
        assertThat(report.expenseByCategory[1].name).isEqualTo("Продукты")
        assertThat(report.expenseByCategory[1].total).isEqualByComparingTo("1000")
        assertThat(report.incomeByCategory).isEmpty()
    }

    @Test
    fun `reports empty period`() {
        givenOperations()

        val report = service.buildHistoryReport(groupId, startDate, endDate)

        assertThat(report.isEmpty).isTrue()
        assertThat(report.operations).isEmpty()
        assertThat(report.totalIncome).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(report.totalExpense).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(report.balance).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `keeps only requested operation type`() {
        givenOperations(
            expense(amount = "1000", category = "Продукты", icon = "🛒"),
            income(amount = "5000", category = "Зарплата", icon = "💰"),
        )

        val report = service.buildHistoryReport(groupId, startDate, endDate, typeFilter = CategoryType.EXPENSE)

        assertThat(report.operations).hasSize(1)
        assertThat(report.totalExpense).isEqualByComparingTo("1000")
        assertThat(report.totalIncome).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(report.incomeByCategory).isEmpty()
    }

    @Test
    fun `category filter matches category name and description`() {
        givenOperations(
            expense(amount = "1000", category = "Продукты", icon = "🛒"),
            expense(amount = "400", category = "Такси", icon = "🚗", description = "в аэропорт"),
            expense(amount = "900", category = "Развлечения", icon = "🎬"),
        )

        val byCategory = service.buildHistoryReport(groupId, startDate, endDate, categoryFilter = "Продукты")
        assertThat(byCategory.operations).hasSize(1)
        assertThat(byCategory.totalExpense).isEqualByComparingTo("1000")

        val byDescription = service.buildHistoryReport(groupId, startDate, endDate, categoryFilter = "аэропорт")
        assertThat(byDescription.operations).hasSize(1)
        assertThat(byDescription.totalExpense).isEqualByComparingTo("400")
    }

    @Test
    fun `falls back to default icon when category has none`() {
        givenOperations(expense(amount = "100", category = "Без иконки", icon = null))

        val report = service.buildHistoryReport(groupId, startDate, endDate)

        assertThat(report.operations.single().categoryIcon).isEqualTo("📌")
        assertThat(report.expenseByCategory.single().icon).isEqualTo("📌")
    }

    private fun givenOperations(vararg operations: FinanceOperationEntity) {
        repository.operations = operations.toList()
    }

    private fun expense(
        amount: String,
        category: String,
        icon: String?,
        description: String? = null
    ) = operation(CategoryType.EXPENSE, amount, category, icon, description)

    private fun income(
        amount: String,
        category: String,
        icon: String?,
        description: String? = null
    ) = operation(CategoryType.INCOME, amount, category, icon, description)

    private fun operation(
        type: CategoryType,
        amount: String,
        category: String,
        icon: String?,
        description: String?
    ) = FinanceOperationEntity(
        id = ObjectId(),
        groupId = groupId,
        creatorId = 42L,
        type = type,
        amount = BigDecimal(amount),
        categoryId = ObjectId(),
        categoryName = category,
        categoryIcon = icon,
        operationDate = startDate,
        description = description
    )
}

private class FakeFinanceOperationRepository(
    private val delegate: FinanceOperationRepository = mock(FinanceOperationRepository::class.java)
) : FinanceOperationRepository by delegate {
    var operations: List<FinanceOperationEntity> = emptyList()

    override fun findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(
        groupId: ObjectId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<FinanceOperationEntity> = operations
}
