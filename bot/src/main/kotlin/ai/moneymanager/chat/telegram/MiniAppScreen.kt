package ai.moneymanager.chat.telegram

import java.time.YearMonth

private const val SCREEN_PARAMETER = "screen"
private const val MONTH_PARAMETER = "month"
private const val IMPORT_PARAMETER = "import"

enum class MiniAppScreen(val parameter: String?) {
    HOME(null),
    ADD_OPERATION("add"),
    CATEGORIES("categories"),
    REPORTS("reports"),
    OPERATIONS("operations"),
    GROUPS("groups"),
    ASK("ask"),
    DEBTS("debts"),
    IMPORT("import"),
}

fun miniAppUrl(baseUrl: String, screen: MiniAppScreen, month: YearMonth? = null, importToken: String? = null): String {
    val parameters = listOfNotNull(
        screen.parameter?.let { "$SCREEN_PARAMETER=$it" },
        month?.let { "$MONTH_PARAMETER=$it" },
        importToken?.let { "$IMPORT_PARAMETER=$it" },
    )
    return if (parameters.isEmpty()) baseUrl else "$baseUrl?${parameters.joinToString("&")}"
}
