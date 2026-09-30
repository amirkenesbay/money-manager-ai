package ai.moneymanager.architecture

import com.lemonappdev.konsist.api.Konsist
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val ROOT = "ai.moneymanager"
private const val BOT = "$ROOT.chat"
private const val SOURCE_ROOT = "src/main/kotlin/ai/moneymanager/"
private val TELEGRAM = listOf("kz.rmr", "org.telegram")
private val WEB_LAYER = listOf("$ROOT.web")
private val CORE_LAYERS = listOf("$ROOT.service", "$ROOT.domain", "$ROOT.repository", "$ROOT.mapper")

private data class BoundaryRule(val layers: List<String>, val forbiddenImports: List<String>)

private val RULES = listOf(
    BoundaryRule(WEB_LAYER, listOf(BOT) + TELEGRAM),
    BoundaryRule(CORE_LAYERS, listOf(BOT)),
)

private val KNOWN_VIOLATIONS = setOf(
    "domain/model/nlp/AiPendingAction.kt -> ai.moneymanager.chat.reply.common.escapeHtml",
    "domain/model/nlp/AiPendingAction.kt -> ai.moneymanager.chat.reply.common.formatAmount",
    "domain/model/nlp/AiPendingAction.kt -> ai.moneymanager.chat.reply.common.formatDescriptionSuffix",
    "domain/model/nlp/AiPendingAction.kt -> ai.moneymanager.chat.reply.common.formatIconPrefix",
    "domain/model/nlp/AiPendingAction.kt -> ai.moneymanager.chat.reply.common.formatSignedAmount",
    "domain/model/nlp/AiPendingAction.kt -> ai.moneymanager.chat.reply.common.formatTime",
    "service/FinanceReportFormatter.kt -> ai.moneymanager.chat.reply.common.blockquote",
    "service/FinanceReportFormatter.kt -> ai.moneymanager.chat.reply.common.bold",
    "service/FinanceReportFormatter.kt -> ai.moneymanager.chat.reply.common.code",
    "service/FinanceReportFormatter.kt -> ai.moneymanager.chat.reply.common.escapeHtml",
    "service/FinanceReportFormatter.kt -> ai.moneymanager.chat.reply.common.formatAmount",
    "service/FinanceReportFormatter.kt -> ai.moneymanager.chat.reply.common.italic",
    "service/FinanceReportFormatter.kt -> ai.moneymanager.chat.reply.common.pre",
    "service/FinanceReportFormatter.kt -> ai.moneymanager.chat.reply.common.progressBar",
)

class LayerBoundariesTest {
    private val violations = findViolations()

    @Test
    fun `no layer imports code it must not depend on`() {
        assertThat(violations - KNOWN_VIOLATIONS)
            .describedAs("New boundary violations: web must not depend on the bot or Telegram, core must not depend on the bot")
            .isEmpty()
    }

    @Test
    fun `list of known violations only shrinks`() {
        assertThat(KNOWN_VIOLATIONS - violations)
            .describedAs("These violations are fixed: remove them from KNOWN_VIOLATIONS")
            .isEmpty()
    }
}

private fun findViolations(): Set<String> =
    Konsist.scopeFromProduction().files.flatMap { file ->
        val packageName = file.packagee?.name.orEmpty()
        RULES.filter { rule -> rule.layers.any { packageName.isWithin(it) } }
            .flatMap { rule -> file.imports.map { it.name }.filter { import -> rule.forbiddenImports.any { import.isWithin(it) } } }
            .map { import -> "${file.path.substringAfter(SOURCE_ROOT)} -> $import" }
    }.toSet()

private fun String.isWithin(prefix: String): Boolean = this == prefix || startsWith("$prefix.")
