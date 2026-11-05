package ai.moneymanager.chat.telegram

import java.time.YearMonth

private const val SCREEN_PARAMETER = "screen"
private const val MONTH_PARAMETER = "month"

enum class MiniAppScreen(val parameter: String?) {
    HOME(null),
    ADD_OPERATION("add"),
    CATEGORIES("categories"),
    REPORTS("reports"),
    OPERATIONS("operations"),
    GROUPS("groups"),
    ASK("ask"),
    DEBTS("debts"),
}

fun miniAppUrl(baseUrl: String, screen: MiniAppScreen, month: YearMonth? = null): String {
    val parameters = listOfNotNull(
        screen.parameter?.let { "$SCREEN_PARAMETER=$it" },
        month?.let { "$MONTH_PARAMETER=$it" },
    )
    return if (parameters.isEmpty()) baseUrl else "$baseUrl?${parameters.joinToString("&")}"
}
