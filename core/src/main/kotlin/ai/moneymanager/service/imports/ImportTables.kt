package ai.moneymanager.service.imports

import ai.moneymanager.domain.model.imports.ImportTable
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

private const val ZIP_MAGIC = "PK"
private const val BOM = '﻿'
private const val REPLACEMENT = '�'
private const val QUOTE = '"'
private val LEGACY_CHARSET: Charset = Charset.forName("windows-1251")
private val DELIMITERS = listOf(';', ',', '\t')
const val MAX_IMPORT_BYTES = 5_000_000
const val MAX_IMPORT_ROWS = 20_000

fun readTable(bytes: ByteArray): ImportTable {
    val raw = if (String(bytes, 0, minOf(bytes.size, ZIP_MAGIC.length), StandardCharsets.ISO_8859_1) == ZIP_MAGIC) readSheet(bytes) else readCsv(bytes)
    val rows = raw.filter { row -> row.any { it.isNotBlank() } }
    require(rows.size > 1) { "The file has no rows" }
    require(rows.size <= MAX_IMPORT_ROWS + 1) { "The file has too many rows" }
    return ImportTable(rows.first().map { it.trim().trimStart(BOM) }, rows.drop(1))
}

private fun readSheet(bytes: ByteArray): List<List<String>> =
    ReadableWorkbook(ByteArrayInputStream(bytes)).use { workbook ->
        workbook.firstSheet.openStream().use { stream ->
            stream.map { row -> (0 until row.cellCount).map { index -> row.getCellRawValue(index).orElse("") } }.toList()
        }
    }

private fun readCsv(bytes: ByteArray): List<List<String>> {
    val utf = String(bytes, StandardCharsets.UTF_8)
    val text = if (REPLACEMENT in utf) String(bytes, LEGACY_CHARSET) else utf
    val firstLine = text.trimStart(BOM).lineSequence().firstOrNull().orEmpty()
    val delimiter = DELIMITERS.maxBy { candidate -> firstLine.count { it == candidate } }
    return parseCsv(text.trimStart(BOM), delimiter)
}

private fun parseCsv(text: String, delimiter: Char): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var quoted = false
    var index = 0
    while (index < text.length) {
        val char = text[index]
        when {
            quoted && char == QUOTE && text.getOrNull(index + 1) == QUOTE -> cell.append(QUOTE).also { index++ }
            char == QUOTE -> quoted = !quoted
            !quoted && char == delimiter -> row.add(cell.toString()).also { cell.clear() }
            !quoted && (char == '\n' || char == '\r') -> {
                if (char == '\r' && text.getOrNull(index + 1) == '\n') index++
                row.add(cell.toString())
                cell.clear()
                rows.add(row)
                row = mutableListOf()
            }
            else -> cell.append(char)
        }
        index++
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) rows.add(row + cell.toString())
    return rows
}
