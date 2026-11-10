package ai.moneymanager.chat.telegram.widget

import org.springframework.core.io.ClassPathResource

private const val STYLE_PATH = "widgets/card.css"

object WidgetHtml {
    private val style: String by lazy { ClassPathResource(STYLE_PATH).inputStream.use { it.readBytes().decodeToString() } }

    fun card(body: String): String = "<style>$style</style><div class=\"w\">$body</div>"

    fun span(className: String, content: String): String = "<span class=\"$className\">$content</span>"

    fun row(left: String, right: String): String = "<div class=\"r\">$left$right</div>"
}
