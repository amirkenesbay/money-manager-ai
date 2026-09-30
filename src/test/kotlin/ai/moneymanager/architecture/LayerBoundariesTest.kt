package ai.moneymanager.architecture

import com.lemonappdev.konsist.api.Konsist
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val ROOT = "ai.moneymanager"
private const val BOT = "$ROOT.chat"
private const val SOURCE_ROOT = "src/main/kotlin/ai/moneymanager/"
private val BOT_AND_TELEGRAM = listOf(BOT, "kz.rmr", "org.telegram")
private val WEB_LAYER = listOf("$ROOT.web")
private val CORE_LAYERS = listOf("$ROOT.service", "$ROOT.domain", "$ROOT.repository", "$ROOT.mapper", "$ROOT.config")

private data class BoundaryRule(val layers: List<String>, val forbiddenImports: List<String>)

private val RULES = listOf(
    BoundaryRule(WEB_LAYER, BOT_AND_TELEGRAM),
    BoundaryRule(CORE_LAYERS, BOT_AND_TELEGRAM),
)

class LayerBoundariesTest {
    @Test
    fun `web and core depend neither on the bot nor on Telegram`() {
        assertThat(findViolations())
            .describedAs("Web and core must not import the bot layer or Telegram libraries; move shared code into core instead")
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
