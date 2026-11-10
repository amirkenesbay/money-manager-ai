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
import java.time.LocalDate
import java.time.YearMonth

class NoticeCardTest {
    private val renderer = NoticeCardRenderer(testLocalizationService())
    private val group = testGroup()
    private val cafe = Category(ObjectId(), "Кафе", "☕", CategoryType.EXPENSE, ObjectId())
    private val period = MonthPeriod(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))

    private fun operation(amount: String, name: String = "Продукты", description: String? = "Магнум") = FinanceOperationEntity(
        id = ObjectId(), groupId = ObjectId(), creatorId = 1, type = CategoryType.EXPENSE, amount = BigDecimal(amount),
        categoryId = ObjectId(), categoryName = name, categoryIcon = "🛒", operationDate = LocalDate.of(2026, 11, 10), description = description,
    )

    @Test
    fun `budget near the limit shows what is left in a ring`() {
        val html = renderer.budget(Budget(cafe, BigDecimal("40000"), BigDecimal("36800"), period), BudgetThreshold.NEAR, group, "ru")

        assertThat(html).contains("92%", "☕ Кафе", "3 200₸", "из 40 000₸", "до 30.11").doesNotContain("rf o")
        assertFitsWidgetLimits(html)
    }

    @Test
    fun `budget over the limit shows the overspend in red`() {
        val html = renderer.budget(Budget(cafe, BigDecimal("40000"), BigDecimal("43200"), period), BudgetThreshold.OVER, group, "ru")

        assertThat(html).contains("rf o", "+3 200₸", "stroke-dashoffset=\"0.0\"")
    }

    @Test
    fun `one added operation is shown big with who added it`() {
        val html = renderer.added(listOf(OperationChange.Added(operation("12400"))), "Дана", group)

        assertThat(html).contains("<i>Д</i>", "Дана", "−12 400₸", "🛒 Продукты · Магнум")
    }

    @Test
    fun `several added operations are listed`() {
        val html = renderer.added(listOf(OperationChange.Added(operation("12400")), OperationChange.Added(operation("2000", "Такси"))), "Дана", group)

        assertThat(html).contains("🛒 Продукты", "−12 400₸", "🛒 Такси", "−2 000₸")
    }

    @Test
    fun `edits and deletions stay as text`() {
        assertThat(renderer.added(listOf(OperationChange.Deleted(operation("12400"))), "Дана", group)).isNull()
    }
}
