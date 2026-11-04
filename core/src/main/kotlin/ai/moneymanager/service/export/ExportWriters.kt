package ai.moneymanager.service.export

import ai.moneymanager.domain.model.ExportFormat
import ai.moneymanager.domain.model.ExportRow
import ai.moneymanager.domain.model.ExportTable
import org.dhatim.fastexcel.Workbook
import java.io.ByteArrayOutputStream

private const val BYTE_ORDER_MARK = "﻿"
private const val CSV_SEPARATOR = ";"
private const val CSV_LINE_BREAK = "\r\n"
private const val QUOTE = "\""
private const val ESCAPED_QUOTE = "\"\""
private val NEEDS_QUOTES = Regex("[;\"\r\n]")
private const val APPLICATION_NAME = "MoneyManager"
private const val APPLICATION_VERSION = "1.0"
private const val SHEET_NAME = "Operations"
private const val DATE_FORMAT = "dd.mm.yyyy"
private const val AMOUNT_FORMAT = "#,##0.00"
private const val HEADER_ROW = 0
private const val DATE_COLUMN = 0
private const val AMOUNT_COLUMN = 3
private const val DATE_COLUMN_WIDTH = 12.0
private const val TEXT_COLUMN_WIDTH = 20.0

fun writeExport(table: ExportTable, format: ExportFormat): ByteArray = when (format) {
    ExportFormat.CSV -> csv(table)
    ExportFormat.XLSX -> xlsx(table)
}

private fun csv(table: ExportTable): ByteArray {
    val lines = listOf(table.headers) + table.rows.map { row -> row.cells().map { it.toString() } }
    val body = lines.joinToString(CSV_LINE_BREAK, postfix = CSV_LINE_BREAK) { cells -> cells.joinToString(CSV_SEPARATOR, transform = ::csvCell) }
    return (BYTE_ORDER_MARK + body).toByteArray(Charsets.UTF_8)
}

private fun csvCell(value: String): String =
    if (NEEDS_QUOTES.containsMatchIn(value)) QUOTE + value.replace(QUOTE, ESCAPED_QUOTE) + QUOTE else value

private fun xlsx(table: ExportTable): ByteArray {
    val output = ByteArrayOutputStream()
    val workbook = Workbook(output, APPLICATION_NAME, APPLICATION_VERSION)
    val sheet = workbook.newWorksheet(SHEET_NAME)
    table.headers.forEachIndexed { column, header ->
        sheet.value(HEADER_ROW, column, header)
        sheet.style(HEADER_ROW, column).bold().set()
        sheet.width(column, if (column == DATE_COLUMN) DATE_COLUMN_WIDTH else TEXT_COLUMN_WIDTH)
    }
    table.rows.forEachIndexed { index, row ->
        val line = index + 1
        row.cells().forEachIndexed { column, cell ->
            when (column) {
                DATE_COLUMN -> {
                    sheet.value(line, column, row.date)
                    sheet.style(line, column).format(DATE_FORMAT).set()
                }
                AMOUNT_COLUMN -> {
                    sheet.value(line, column, row.amount)
                    sheet.style(line, column).format(AMOUNT_FORMAT).set()
                }
                else -> sheet.value(line, column, cell.toString())
            }
        }
    }
    workbook.finish()
    return output.toByteArray()
}

private fun ExportRow.cells(): List<Any> = listOf(date, type, category, amount.toPlainString(), currency, description, author)
