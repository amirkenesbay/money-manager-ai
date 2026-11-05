package ai.moneymanager.domain.model.question

import java.math.BigDecimal
import java.time.YearMonth

enum class AnswerScreen {
    REPORTS,
    OPERATIONS,
    CATEGORIES,
    RECURRING,
}

data class AnswerLink(val screen: AnswerScreen, val month: YearMonth? = null, val search: String? = null)

data class AnswerBar(val label: String, val value: BigDecimal, val current: Boolean)

data class AnswerRow(val icon: String, val label: String, val value: String, val note: String? = null)

data class AnswerCard(
    val title: String,
    val headline: String? = null,
    val lines: List<String> = emptyList(),
    val bars: List<AnswerBar> = emptyList(),
    val rows: List<AnswerRow> = emptyList(),
    val followUps: List<String> = emptyList(),
    val link: AnswerLink? = null,
    val understood: Boolean = true,
)
