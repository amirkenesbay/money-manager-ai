package ai.moneymanager.service.imports

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.imports.ColumnRole
import ai.moneymanager.domain.model.imports.ImportSource
import ai.moneymanager.domain.model.imports.SkipReason
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.time.LocalDate

private const val ZENMONEY = """date;categoryName;payee;comment;outcomeAccountName;outcome;outcomeCurrencyShortTitle;incomeAccountName;income;incomeCurrencyShortTitle
2026-11-03;Кафе;Starbucks;капучино;Kaspi;1500;KZT;;;
2026-11-02;Зарплата;;;;;;Kaspi;450000;KZT
2026-11-01;;;;Kaspi;20000;KZT;Наличные;20000;KZT
2026-10-30;Такси;;;Card;12;USD;;;"""

private const val COINKEEPER = """Дата,Тип,Из,В,Метки,Сумма,Валюта,Примечание
03.11.2026,Расход,Kaspi,Продукты,,"3 500,50",KZT,Магнум
02.11.2026,Доход,Зарплата,Kaspi,,450000,KZT,
01.11.2026,Перевод,Kaspi,Наличные,,5000,KZT,"""

private const val TAB_MARK = "<tab>"
private const val UNKNOWN = """when<tab>sum<tab>group
05/11/2026<tab>-2000<tab>Taxi
04/11/2026<tab>300000<tab>Salary"""

class ImportParsingTest {
    @Test
    fun `reads amounts and dates written in different ways`() {
        assertThat(parseAmount("1 500,50")).isEqualByComparingTo("1500.50")
        assertThat(parseAmount("1,500.50")).isEqualByComparingTo("1500.50")
        assertThat(parseAmount("−2 000 ₸")).isEqualByComparingTo("-2000")
        assertThat(parseAmount("12,000")).isEqualByComparingTo("12000")
        assertThat(parseDate("03.11.2026")).isEqualTo(LocalDate.of(2026, 11, 3))
        assertThat(parseDate("2026-11-03 14:20:00")).isEqualTo(LocalDate.of(2026, 11, 3))
        assertThat(parseDate("45964")).isEqualTo(LocalDate.of(2025, 11, 3))
    }

    @Test
    fun `recognises a zenmoney export and skips transfers and other currencies`() {
        val table = readTable(ZENMONEY.toByteArray())
        val (source, mapping) = checkNotNull(knownMapping(table))

        val parsed = parseRows(table, mapping, Currency.KZT)

        assertThat(source).isEqualTo(ImportSource.ZENMONEY)
        assertThat(parsed.rows.map { it.type to it.category }).containsExactly(CategoryType.EXPENSE to "Кафе", CategoryType.INCOME to "Зарплата")
        assertThat(parsed.skipped).containsEntry(SkipReason.TRANSFER, 1).containsEntry(SkipReason.CURRENCY, 1)
    }

    @Test
    fun `takes the income category from where the money came in coinkeeper`() {
        val table = readTable(COINKEEPER.toByteArray(Charset.forName("windows-1251")))
        val (source, mapping) = checkNotNull(knownMapping(table))

        val parsed = parseRows(table, mapping, Currency.KZT)

        assertThat(source).isEqualTo(ImportSource.COINKEEPER)
        assertThat(parsed.rows.first().amount).isEqualByComparingTo("3500.50")
        assertThat(parsed.rows.map { it.category }).containsExactly("Продукты", "Зарплата")
        assertThat(parsed.skipped).containsEntry(SkipReason.TRANSFER, 1)
    }

    @Test
    fun `guesses columns of an unknown file from its values`() {
        val table = readTable(UNKNOWN.replace(TAB_MARK, "\t").toByteArray())
        val mapping = checkNotNull(guessMapping(table))

        val parsed = parseRows(table, mapping, Currency.KZT)

        assertThat(mapping[ColumnRole.DATE]).isEqualTo(0)
        assertThat(parsed.rows.map { it.type }).containsExactly(CategoryType.EXPENSE, CategoryType.INCOME)
        assertThat(parsed.rows.map { it.category }).containsOnly(FALLBACK_CATEGORY)
    }
}
