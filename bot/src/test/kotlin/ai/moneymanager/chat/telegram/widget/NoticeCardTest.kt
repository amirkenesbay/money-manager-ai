package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.domain.event.OperationChange
import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.BudgetThreshold
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MonthPeriod
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testLocalizationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

private val TODAY: LocalDate = LocalDate.of(2026, 11, 11)

class NoticeCardTest {
    private val renderer = NoticeCardRenderer(testLocalizationService(), Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC))
    private val group = testGroup()
    private val food = Category(ObjectId(), "Еда", "🍔", CategoryType.EXPENSE, ObjectId())
    private val period = MonthPeriod(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))

    private fun operation(amount: String, categoryId: ObjectId = food.id!!, name: String = "Еда", description: String? = null) = FinanceOperationEntity(
        id = ObjectId(), groupId = ObjectId(), creatorId = 1, type = CategoryType.EXPENSE, amount = BigDecimal(amount),
        categoryId = categoryId, categoryName = name, categoryIcon = "🍔", operationDate = TODAY, description = description,
    )

    @Test
    fun `budget card shows what is left per day and switches to what is spent on tap`() {
        val html = renderer.budget(Budget(food, BigDecimal("40000"), BigDecimal("36800"), period), BudgetThreshold.NEAR, group, "ru")

        assertThat(html).contains("92%", "🍔 Еда", "data-a=\"3 200₸\"", "по 160₸ в день", "data-b=\"36 800₸\"", "потрачено из 40 000₸").doesNotContain("rf o")
        assertFitsWidgetLimits(html)
    }

    @Test
    fun `budget card over the limit shows the overspend`() {
        val html = renderer.budget(Budget(food, BigDecimal("40000"), BigDecimal("43200"), period), BudgetThreshold.OVER, group, "ru")

        assertThat(html).contains("rf o", "+3 200₸", "сверх лимита 40 000₸")
    }

    @Test
    fun `new category card shows who added it with the icon and name`() {
        val html = renderer.categoryCreated("Dana", group, "💐", "Далия", "ru")

        assertThat(html).contains("<i>D</i>", "Dana", "class=\"j\">💐", "Новая категория", "Далия")
        assertFitsWidgetLimits(html)
    }

    @Test
    fun `one added operation shows the amount, category and its budget`() {
        val budget = Budget(food, BigDecimal("50000"), BigDecimal("32400"), period)
        val html = renderer.added(listOf(OperationChange.Added(operation("3180"))), "Dana", group, listOf(budget), "ru")

        assertThat(html).contains("data-n=\"3180\"", "−3 180₸", "🍔 Еда", "Бюджет", "32 400 / 50 000₸", "width:64%")
    }

    @Test
    fun `one added operation without a budget shows no budget line`() {
        val html = renderer.added(listOf(OperationChange.Added(operation("3180"))), "Dana", group, emptyList(), "ru")

        assertThat(html).doesNotContain("Бюджет")
    }

    @Test
    fun `several added operations are listed`() {
        val html = renderer.added(listOf(OperationChange.Added(operation("12400")), OperationChange.Added(operation("2000", name = "Такси"))), "Dana", group, emptyList(), "ru")

        assertThat(html).contains("🍔 Еда", "−12 400₸", "🍔 Такси", "−2 000₸")
    }

    @Test
    fun `edits and deletions stay as text`() {
        assertThat(renderer.added(listOf(OperationChange.Deleted(operation("12400"))), "Dana", group, emptyList(), "ru")).isNull()
    }

    @Test
    fun `escapes names typed by people`() {
        assertThat(renderer.categoryCreated("<b>", group, "💐", "<i>", "ru")).contains("&lt;b&gt;", "&lt;i&gt;")
    }
}
