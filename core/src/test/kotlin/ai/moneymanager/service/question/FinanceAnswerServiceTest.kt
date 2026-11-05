package ai.moneymanager.service.question

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.question.AnswerScreen
import ai.moneymanager.domain.model.question.FinanceIntent
import ai.moneymanager.domain.model.question.FinanceQuestion
import ai.moneymanager.domain.model.question.PeriodPreset
import ai.moneymanager.domain.model.question.PeriodSpec
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.BudgetService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.RecurringPaymentService
import ai.moneymanager.service.UserInfoService
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
import java.time.YearMonth
import java.time.ZoneOffset

private const val RUSSIAN = "ru"
private const val PARTNER = TEST_USER_ID + 1
private val TODAY: LocalDate = LocalDate.of(2026, 11, 20)

class FinanceAnswerServiceTest {
    private val group = testGroup(members = setOf(TEST_USER_ID, PARTNER))
    private val groupId = checkNotNull(group.id)
    private val cafe = Category(ObjectId(), "Кафе", "☕", CategoryType.EXPENSE, groupId)
    private val groceries = Category(ObjectId(), "Продукты", "🛒", CategoryType.EXPENSE, groupId)
    private val salary = Category(ObjectId(), "Зарплата", "💰", CategoryType.INCOME, groupId)
    private val repository = InMemoryOperationRepository()
    private val categoryService = mock(CategoryService::class.java)
    private val userInfoService = mock(UserInfoService::class.java)
    private val clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val service = FinanceAnswerService(
        repository, categoryService, BudgetService(categoryService, repository, clock), mock(RecurringPaymentService::class.java),
        FinanceOperationService(repository, mock(MoneyGroupRepository::class.java)) { }, userInfoService, testLocalizationService(),
    )

    @BeforeEach
    fun givenGroupData() {
        `when`(categoryService.getCategoriesByGroup(groupId)).thenReturn(listOf(cafe, groceries, salary))
        `when`(userInfoService.displayNames(group.memberIds)).thenReturn(mapOf(TEST_USER_ID to "Амир", PARTNER to "Аня"))
        repository.operations = listOf(
            operation(salary, "450000", LocalDate.of(2026, 11, 2)),
            operation(cafe, "1500", LocalDate.of(2026, 11, 3), "Starbucks #работа"),
            operation(cafe, "2500", LocalDate.of(2026, 11, 10), "Starbucks", PARTNER),
            operation(groceries, "20000", LocalDate.of(2026, 11, 12), "Магнум"),
            operation(cafe, "2000", LocalDate.of(2026, 10, 15)),
            operation(cafe, "1000", LocalDate.of(2026, 9, 15)),
        )
    }

    @Test
    fun `sums a category over the month and links to its report`() {
        val card = ask(FinanceQuestion(FinanceIntent.SUM, categories = listOf("кафе"), period = PeriodSpec.Month(YearMonth.of(2026, 11))))

        assertThat(card.title).isEqualTo("☕ Кафе · Ноябрь")
        assertThat(card.headline).isEqualTo("4 000₸")
        assertThat(card.lines).contains("Операций: 2")
        assertThat(card.link?.screen).isEqualTo(AnswerScreen.REPORTS)
        assertThat(card.followUps).isNotEmpty()
    }

    @Test
    fun `finds money spent at a place and when it was last`() {
        val card = ask(FinanceQuestion(FinanceIntent.PLACE, place = "starbucks"))

        assertThat(card.headline).isEqualTo("4 000₸")
        assertThat(card.lines).contains("Последний раз — 10.11")
        assertThat(card.link?.search).isEqualTo("starbucks")
    }

    @Test
    fun `compares a month with the one before`() {
        val card = ask(FinanceQuestion(FinanceIntent.COMPARE, categories = listOf("Кафе"), period = PeriodSpec.Preset(PeriodPreset.THIS_MONTH)))

        assertThat(card.headline).isEqualTo("4 000₸")
        assertThat(card.lines.first()).isEqualTo("на 100% больше, чем 2 000₸ в прошлый раз")
        assertThat(card.bars.map { it.value }).containsExactly(BigDecimal("2000"), BigDecimal("4000"))
    }

    @Test
    fun `shows where the money went with shares`() {
        val card = ask(FinanceQuestion(FinanceIntent.BREAKDOWN))

        assertThat(card.headline).isEqualTo("24 000₸")
        assertThat(card.rows.map { it.label to it.note }).containsExactly("Продукты" to "83%", "Кафе" to "17%")
    }

    @Test
    fun `splits spending by who recorded it`() {
        val card = ask(FinanceQuestion(FinanceIntent.MEMBERS))

        assertThat(card.rows.map { it.label to it.value }).containsExactly("Амир" to "21 500₸", "Аня" to "2 500₸")
    }

    @Test
    fun `tells what can be spent per day this month`() {
        val card = ask(FinanceQuestion(FinanceIntent.BALANCE))

        assertThat(card.lines).contains("В этом месяце: расходы 24 000₸, доходы 450 000₸", "Можно тратить 38 727₸ в день — 11 дн. до конца месяца")
    }

    @Test
    fun `projects spending to the end of the month`() {
        val card = ask(FinanceQuestion(FinanceIntent.FORECAST))

        assertThat(card.headline).isEqualTo("36 000₸")
        assertThat(card.lines).contains("Останется 414 000₸ от дохода")
    }

    @Test
    fun `answers whether a purchase fits without advising`() {
        val card = ask(FinanceQuestion(FinanceIntent.AFFORDABILITY, amount = BigDecimal("500000")))

        assertThat(card.headline).isEqualTo("Не хватит 74 000₸")
    }

    @Test
    fun `names categories that grew against past months`() {
        val card = ask(FinanceQuestion(FinanceIntent.TRENDS))

        assertThat(card.rows.single().label).isEqualTo("Кафе")
        assertThat(card.rows.single().note).isEqualTo("+167%")
    }

    @Test
    fun `explains a missing category and offers real ones`() {
        val card = ask(FinanceQuestion(FinanceIntent.SUM, categories = listOf("Спорт")))

        assertThat(card.title).isEqualTo("Категории «Спорт» нет")
        assertThat(card.rows.map { it.label }).containsExactly("Кафе", "Продукты")
    }

    @Test
    fun `counts only the history the tariff allows and says so`() {
        val card = service.answer(FinanceQuestion(FinanceIntent.SUM, categories = listOf("Кафе"), period = PeriodSpec.Preset(PeriodPreset.ALL)), group, RUSSIAN, TODAY, LocalDate.of(2026, 10, 21))

        assertThat(card.headline).isEqualTo("4 000₸")
        assertThat(card.lines).contains("Учтены операции с 21.10 — раньше доступно в Premium")
    }

    private fun ask(question: FinanceQuestion) = service.answer(question, group, RUSSIAN, TODAY, null)

    private fun operation(category: Category, amount: String, date: LocalDate, description: String? = null, creator: Long = TEST_USER_ID) = FinanceOperationEntity(
        id = ObjectId(), groupId = groupId, creatorId = creator, type = category.type, amount = BigDecimal(amount),
        categoryId = checkNotNull(category.id), categoryName = category.name, categoryIcon = category.icon, operationDate = date, description = description,
    )
}
