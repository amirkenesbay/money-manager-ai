package ai.moneymanager.service.imports

import ai.moneymanager.domain.model.imports.ColumnMapping
import ai.moneymanager.domain.model.imports.ColumnRole
import ai.moneymanager.domain.model.imports.ImportTable
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.GeminiService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

private const val SAMPLE_ROWS = 5
private const val CELL_SEPARATOR = " | "
private const val JSON_START = '{'
private const val JSON_END = '}'

@Component
class ImportColumnAi(
    private val geminiService: GeminiService,
    private val aiPromptService: AiPromptService,
) {
    private val objectMapper = ObjectMapper()

    fun guess(table: ImportTable): ColumnMapping? {
        val headers = table.headers.mapIndexed { index, header -> "$index: $header" }.joinToString(CELL_SEPARATOR)
        val samples = table.rows.take(SAMPLE_ROWS).joinToString("\n") { it.joinToString(CELL_SEPARATOR) }
        val reply = geminiService.generateText(aiPromptService.importColumnsPrompt("$headers\n$samples")) ?: return null
        return mappingOf(reply, table.headers.size)
    }

    private fun mappingOf(reply: String, columnCount: Int): ColumnMapping? {
        val json = reply.substringAfter(JSON_START, "").substringBeforeLast(JSON_END, "").takeIf { it.isNotEmpty() } ?: return null
        val values = runCatching { objectMapper.readTree("$JSON_START$json$JSON_END") }.getOrNull() ?: return null
        val roles = ColumnRole.entries.mapNotNull { role ->
            values.get(role.name)?.takeIf { it.isInt }?.intValue()?.takeIf { it in 0 until columnCount }?.let { role to it }
        }.toMap()
        return ColumnMapping(roles).takeIf { it.complete }
    }
}
