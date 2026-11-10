package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Digest
import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.DigestPeriod
import ai.moneymanager.domain.model.MonthLeft
import ai.moneymanager.domain.model.MonthPeriod
import ai.moneymanager.domain.model.report.CategoryTotal
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testLocalizationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

private val DAY: LocalDate = LocalDate.of(2026, 11, 10)
private val TOP = listOf(
    CategoryTotal("🛒", "Продукты", BigDecimal("4200")),
    CategoryTotal("🚕", "Такси", BigDecimal("2000")),
    CategoryTotal("☕", "Кафе", BigDecimal("1500")),
    CategoryTotal("🏠", "Жильё", BigDecimal("700")),
)

class DigestCardTest {
    private val renderer = DigestCardRenderer(testLocalizationService())
    private val group = testGroup()
    private val cafe = Category(ObjectId(), "Кафе", "☕", CategoryType.EXPENSE, ObjectId())
    private val november = MonthPeriod(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))

    private fun digest(
        kind: DigestKind,
        start: LocalDate = DAY,
        end: LocalDate = DAY,
        spent: String = "8400",
        income: String = "0",
        previous: String? = null,
        left: MonthLeft? = MonthLeft(YearMonth.of(2026, 11), BigDecimal("246000"), 20),
        budgets: List<Budget> = emptyList(),
    ) = Digest(DigestPeriod(kind, start, end), group, BigDecimal(spent), BigDecimal(income), 5, TOP, previous?.let(::BigDecimal), budgets, left)

    @Test
    fun `day shows the total, top three and what is left for tomorrow`() {
        val html = renderer.render(digest(DigestKind.DAY), "ru")

        assertThat(html).contains("10 ноября", "8 400₸", "Продукты", "Такси", "Кафе", "На завтра", "12 300₸").doesNotContain("Жильё")
    }

    @Test
    fun `day warns when the month is already overspent`() {
        val html = renderer.render(digest(DigestKind.DAY, left = MonthLeft(YearMonth.of(2026, 11), BigDecimal("-3000"), 20)), "ru")

        assertThat(html).contains("Сверх", "3 000₸").doesNotContain("На завтра")
    }

    @Test
    fun `week shows the daily average and the rest of the month`() {
        val html = renderer.render(digest(DigestKind.WEEK, start = DAY.minusDays(6), spent = "70000"), "ru")

        assertThat(html).contains("4 нояб", "10 нояб", "10 000₸ в день", "До конца месяца", "246 000₸")
    }

    @Test
    fun `month shows the change, the split and the balance`() {
        val over = Budget(cafe, BigDecimal("1000"), BigDecimal("1500"), november)
        val html = renderer.render(digest(DigestKind.MONTH, start = november.start, end = november.end, income = "20000", previous = "10000", budgets = listOf(over)), "ru")

        assertThat(html).contains("Ноябрь", "−16% к прошлому месяцу", "Продукты", "50%", "Доход", "20 000₸", "Остаток", "11 600₸", "⚠️ ☕ Кафе", "+500₸")
        assertFitsWidgetLimits(html)
    }

    @Test
    fun `month marks spending growth`() {
        val html = renderer.render(digest(DigestKind.MONTH, start = november.start, end = november.end, previous = "4200"), "ru")

        assertThat(html).contains("u x", "+100% к прошлому месяцу")
    }

    @Test
    fun `escapes category names`() {
        val html = renderer.render(digest(DigestKind.DAY).copy(topExpenses = listOf(CategoryTotal("☕", "<b>", BigDecimal("10")))), "ru")

        assertThat(html).contains("&lt;b&gt;")
    }
}
