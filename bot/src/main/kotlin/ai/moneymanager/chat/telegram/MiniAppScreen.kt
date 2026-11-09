package ai.moneymanager.chat.telegram

import ai.moneymanager.metrics.MessageType
import java.time.YearMonth

private const val SCREEN_PARAMETER = "screen"
private const val MONTH_PARAMETER = "month"
private const val IMPORT_PARAMETER = "import"
private const val CATEGORY_PARAMETER = "category"
private const val SOURCE_PARAMETER = "src"

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

data class MiniAppLink(
    val screen: MiniAppScreen,
    val month: YearMonth? = null,
    val importToken: String? = null,
    val categoryId: String? = null,
    val source: MessageType? = null,
)

fun miniAppUrl(baseUrl: String, link: MiniAppLink): String {
    val parameters = listOfNotNull(
        link.screen.parameter?.let { "$SCREEN_PARAMETER=$it" },
        link.month?.let { "$MONTH_PARAMETER=$it" },
        link.importToken?.let { "$IMPORT_PARAMETER=$it" },
        link.categoryId?.let { "$CATEGORY_PARAMETER=$it" },
        link.source?.let { "$SOURCE_PARAMETER=${it.tag}" },
    )
    return if (parameters.isEmpty()) baseUrl else "$baseUrl?${parameters.joinToString("&")}"
}
