package ai.moneymanager.service

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.DigestPeriod
import ai.moneymanager.domain.model.MonthLeft
import ai.moneymanager.repository.DigestSentRepository
import ai.moneymanager.repository.entity.DigestSentEntity
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.testing.InMemoryOperationRepository
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.dao.DuplicateKeyException
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

private val TODAY: LocalDate = LocalDate.of(2026, 11, 20)

class DigestServiceTest {
    private val group = testGroup()
    private val groupId = checkNotNull(group.id)
    private val groceries = Category(ObjectId(), "Продукты", "🛒", CategoryType.EXPENSE, groupId, BigDecimal("50000"))
    private val salary = Category(ObjectId(), "Зарплата", "💰", CategoryType.INCOME, groupId)
    private val repository = InMemoryOperationRepository()
    private val categoryService = mock(CategoryService::class.java)
    private val sentRepository = mock(DigestSentRepository::class.java)
    private val clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val service = DigestService(FinanceHistoryService(repository), BudgetService(categoryService, repository, clock), sentRepository)

    @Test
    fun `sums the day and tells how much the rest of the month allows`() {
        repository.operations = listOf(
            operation(salary, "300000", LocalDate.of(2026, 11, 5)),
            operation(groceries, "20000", LocalDate.of(2026, 11, 10)),
            operation(groceries, "8000", TODAY),
        )

        val digest = service.build(group, DigestPeriod(DigestKind.DAY, TODAY, TODAY), TODAY)

        assertThat(digest?.spent).isEqualByComparingTo("8000")
        assertThat(digest?.operationCount).isEqualTo(1)
        assertThat(digest?.monthLeft).isEqualTo(MonthLeft(YearMonth.of(2026, 11), BigDecimal("272000"), 10))
    }

    @Test
    fun `compares the closed month with the one before and checks its budgets`() {
        `when`(categoryService.getBudgetCategories(groupId)).thenReturn(listOf(groceries))
        repository.operations = listOf(
            operation(groceries, "40000", LocalDate.of(2026, 9, 12)),
            operation(groceries, "60000", LocalDate.of(2026, 10, 12)),
        )

        val digest = service.build(group, DigestPeriod(DigestKind.MONTH, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)), TODAY)

        assertThat(digest?.previousSpent).isEqualByComparingTo("40000")
        assertThat(digest?.budgets?.single()?.spent).isEqualByComparingTo("60000")
        assertThat(digest?.monthLeft).isNull()
    }

    @Test
    fun `has nothing to say about a period without operations`() {
        assertThat(service.build(group, DigestPeriod(DigestKind.WEEK, TODAY.minusDays(6), TODAY), TODAY)).isNull()
    }

    @Test
    fun `claims a period only once`() {
        val period = DigestPeriod(DigestKind.DAY, TODAY, TODAY)
        `when`(sentRepository.insert(any(DigestSentEntity::class.java))).thenThrow(DuplicateKeyException("sent"))

        assertThat(service.claim(TEST_USER_ID, period)).isFalse()
    }

    private fun operation(category: Category, amount: String, date: LocalDate) = FinanceOperationEntity(
        id = ObjectId(),
        groupId = groupId,
        creatorId = TEST_USER_ID,
        type = category.type,
        amount = BigDecimal(amount),
        categoryId = checkNotNull(category.id),
        categoryName = category.name,
        categoryIcon = category.icon,
        operationDate = date,
        description = null,
    )
}
