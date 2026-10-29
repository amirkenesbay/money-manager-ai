package ai.moneymanager.chat.telegram

private const val SCREEN_PARAMETER = "screen"

enum class MiniAppScreen(val parameter: String?) {
    HOME(null),
    ADD_OPERATION("add"),
    CATEGORIES("categories"),
}

fun miniAppUrl(baseUrl: String, screen: MiniAppScreen): String =
    screen.parameter?.let { "$baseUrl?$SCREEN_PARAMETER=$it" } ?: baseUrl
