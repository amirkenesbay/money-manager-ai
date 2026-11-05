package ai.moneymanager.service.question

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.question.AskTopic
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.BudgetService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.RecurringPaymentService
import ai.moneymanager.testing.InMemoryOperationRepository
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testLocalizationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private const val RUSSIAN = "ru"
private val TODAY: LocalDate = LocalDate.of(2026, 11, 20)

class AskSuggestionServiceTest {
    private val group = testGroup(members = setOf(TEST_USER_ID, TEST_USER_ID + 1))
    private val groupId = checkNotNull(group.id)
    private val cafe = Category(ObjectId(), "Кафе", "☕", CategoryType.EXPENSE, groupId)
    private val taxi = Category(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId, monthlyLimit = BigDecimal("10000"))
    private val gifts = Category(ObjectId(), "Подарки", "🎁", CategoryType.EXPENSE, groupId)
    private val repository = InMemoryOperationRepository()
    private val categoryService = mock(CategoryService::class.java)
    private val recurringPaymentService = mock(RecurringPaymentService::class.java)
    private val clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val service = AskSuggestionService(
        repository, categoryService, BudgetService(categoryService, repository, clock), recurringPaymentService, testLocalizationService(),
    )

    @BeforeEach
    fun givenGroupData() {
        `when`(categoryService.getCategoriesByGroup(groupId)).thenReturn(listOf(gifts, cafe, taxi))
        `when`(categoryService.getBudgetCategories(groupId)).thenReturn(listOf(taxi))
        `when`(recurringPaymentService.payments(groupId)).thenReturn(emptyList())
        repository.operations = listOf(
            operation(taxi, "9000", LocalDate.of(2026, 11, 5)),
            operation(cafe, "4000", LocalDate.of(2026, 11, 8)),
            operation(gifts, "50000", LocalDate.of(2026, 10, 8)),
        )
    }

    @Test
    fun `builds examples from the categories spent on most this month`() {
        val suggestions = service.suggestions(group, RUSSIAN, TODAY)

        val sum = suggestions.topics.first { it.topic == AskTopic.SUM }
        assertThat(sum.title).isEqualTo("Сколько")
        assertThat(sum.questions).containsExactly("Сколько на такси в этом месяце?", "Сколько на кафе в этом месяце?", "Сколько я потратил в прошлом месяце?")
        assertThat(suggestions.questions.first()).isEqualTo("Сколько на такси в этом месяце?")
    }

    @Test
    fun `asks about a budget close to its limit and who spent what in a shared group`() {
        val suggestions = service.suggestions(group, RUSSIAN, TODAY)

        assertThat(suggestions.topics.first { it.topic == AskTopic.BUDGETS }.questions.first()).isEqualTo("Уложусь в бюджет на такси?")
        assertThat(suggestions.topics.first { it.topic == AskTopic.WHERE }.questions).contains("Кто сколько потратил?")
    }

    @Test
    fun `hides subscriptions until there is a regular payment`() {
        val suggestions = service.suggestions(group, RUSSIAN, TODAY)

        assertThat(suggestions.topics.map { it.topic }).doesNotContain(AskTopic.RECURRING)
        assertThat(service.examples(suggestions)).hasSize(3)
    }

    private fun operation(category: Category, amount: String, date: LocalDate) = FinanceOperationEntity(
        id = ObjectId(), groupId = groupId, creatorId = TEST_USER_ID, type = category.type, amount = BigDecimal(amount),
        categoryId = checkNotNull(category.id), categoryName = category.name, categoryIcon = category.icon, operationDate = date,
    )
}
