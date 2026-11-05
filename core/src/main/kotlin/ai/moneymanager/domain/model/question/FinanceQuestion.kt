package ai.moneymanager.domain.model.question

import ai.moneymanager.domain.model.CategoryType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

enum class FinanceIntent {
    SUM,
    COMPARE,
    BREAKDOWN,
    LARGEST,
    PLACE,
    BUDGETS,
    BALANCE,
    RECURRING,
    MEMBERS,
    FORECAST,
    AVERAGE,
    TRENDS,
    AFFORDABILITY,
    SAVINGS;

    companion object {
        fun of(name: String?): FinanceIntent? = entries.firstOrNull { it.name.equals(name?.trim(), ignoreCase = true) }
    }
}

enum class PeriodPreset {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS,
    THIS_YEAR,
    LAST_YEAR,
    ALL;

    companion object {
        fun of(name: String?): PeriodPreset? = entries.firstOrNull { it.name.equals(name?.trim(), ignoreCase = true) }
    }
}

sealed interface PeriodSpec {
    data class Preset(val preset: PeriodPreset) : PeriodSpec
    data class Month(val month: YearMonth) : PeriodSpec
    data class Range(val from: LocalDate, val to: LocalDate) : PeriodSpec
}

data class FinanceQuestion(
    val intent: FinanceIntent,
    val type: CategoryType? = null,
    val categories: List<String> = emptyList(),
    val period: PeriodSpec? = null,
    val comparePeriod: PeriodSpec? = null,
    val member: String? = null,
    val place: String? = null,
    val tag: String? = null,
    val minAmount: BigDecimal? = null,
    val limit: Int? = null,
    val amount: BigDecimal? = null,
)
