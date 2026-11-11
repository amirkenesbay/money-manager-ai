package ai.moneymanager.chat.telegram.widget

import ai.moneymanager.chat.reply.common.escapeHtml
import org.springframework.core.io.ClassPathResource
import java.math.BigDecimal
import java.math.RoundingMode

private const val STYLE_PATH = "widgets/card.css"
private const val SCRIPT_PATH = "widgets/card.js"

object WidgetHtml {
    private val style: String by lazy { resource(STYLE_PATH) }
    private val script: String by lazy { resource(SCRIPT_PATH) }

    fun card(body: String): String = "<style>$style</style><div class=\"w\"><canvas></canvas>$body</div><script>$script</script>"

    fun span(className: String, content: String): String = "<span class=\"$className\">$content</span>"

    fun row(left: String, right: String): String = "<div class=\"r\">$left$right</div>"

    fun hero(text: String, amount: BigDecimal): String =
        "<span class=\"h\" data-n=\"${amount.abs().setScale(0, RoundingMode.HALF_UP).toPlainString()}\">$text</span>"

    fun person(name: String, groupName: String, separator: String): String =
        "<span class=\"f\"><i>${escapeHtml(name.take(1).uppercase())}</i>${span("l", "${escapeHtml(name)}$separator${escapeHtml(groupName)}")}</span>"

    private fun resource(path: String): String = ClassPathResource(path).inputStream.use { it.readBytes().decodeToString().trim() }
}
