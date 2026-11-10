package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.MonthPeriod
import ai.moneymanager.testing.testLocalizationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

private val TODAY: LocalDate = LocalDate.of(2026, 11, 10)
private const val MAX_COMPRESSED_BYTES = 4096
private const val MAX_HTML_BYTES = 32768

class OperationCardTest {
    private val renderer = OperationCardRenderer(testLocalizationService())
    private val cafe = Category(ObjectId(), "Кафе", "☕", CategoryType.EXPENSE, ObjectId())
    private val period = MonthPeriod(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))

    private fun card(
        amount: String = "1500",
        description: String? = "Магнум",
        date: LocalDate = TODAY,
        budget: Budget? = null,
        type: CategoryType = CategoryType.EXPENSE,
    ) = OperationCard("✅ text", type, BigDecimal(amount), Currency.KZT, cafe, description, date, TODAY, budget, "ru")

    @Test
    fun `shows the amount, category, place and day`() {
        val html = renderer.render(listOf(card()))

        assertThat(html).contains("☕ Кафе", "−1 500₸", "Магнум · сегодня")
    }

    @Test
    fun `shows how much of the budget is spent`() {
        val html = renderer.render(listOf(card(budget = Budget(cafe, BigDecimal("40000"), BigDecimal("32400"), period))))

        assertThat(html).contains("Бюджет", "32 400", "/ 40 000₸", "class=\"n\"", "width:81%")
    }

    @Test
    fun `caps an overspent budget bar`() {
        val html = renderer.render(listOf(card(budget = Budget(cafe, BigDecimal("10000"), BigDecimal("15000"), period))))

        assertThat(html).contains("class=\"o\"", "width:100%")
    }

    @Test
    fun `names an earlier day by its date`() {
        assertThat(renderer.render(listOf(card(date = TODAY.minusDays(3))))).contains("07.11")
    }

    @Test
    fun `lists several operations in one card`() {
        val html = renderer.render(listOf(card(description = "такси", amount = "2000"), card(description = "обед", amount = "3200")))

        assertThat(html).contains("☕ такси", "−2 000₸", "☕ обед", "−3 200₸")
    }

    @Test
    fun `escapes what the user typed`() {
        assertThat(renderer.render(listOf(card(description = "<script>")))).doesNotContain("<script>").contains("&lt;script&gt;")
    }

    @Test
    fun `fits the telegram widget limits`() {
        val html = renderer.render(listOf(card(budget = Budget(cafe, BigDecimal("40000"), BigDecimal("32400"), period))))

        assertThat(html.toByteArray().size).isLessThanOrEqualTo(MAX_HTML_BYTES)
        assertThat(compressed(html)).isLessThanOrEqualTo(MAX_COMPRESSED_BYTES)
    }

    private fun compressed(html: String): Int {
        val bytes = ByteArrayOutputStream()
        DeflaterOutputStream(bytes, Deflater(Deflater.BEST_COMPRESSION)).use { it.write(html.toByteArray()) }
        return bytes.size()
    }
}
