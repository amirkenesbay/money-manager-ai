package ai.moneymanager.chat.reply.question

import ai.moneymanager.chat.reply.common.bold
import ai.moneymanager.chat.reply.common.escapeHtml
import ai.moneymanager.chat.reply.common.italic
import ai.moneymanager.domain.model.question.AnswerCard
import ai.moneymanager.domain.model.question.AnswerRow

private const val LINE_BREAK = "\n"
private const val ROW_TEMPLATE = "%s %s — %s"
private const val NOTE_TEMPLATE = " · %s"

fun formatAnswerCard(card: AnswerCard): String {
    val parts = listOfNotNull(
        bold(escapeHtml(card.title)),
        card.headline?.let { bold(escapeHtml(it)) },
    ) + card.lines.map { escapeHtml(it) } + card.rows.map(::row) + card.followUps.take(1).map { italic(escapeHtml(it)) }
    return parts.joinToString(LINE_BREAK)
}

private fun row(row: AnswerRow): String =
    ROW_TEMPLATE.format(row.icon, escapeHtml(row.label), escapeHtml(row.value)) + (row.note?.let { NOTE_TEMPLATE.format(escapeHtml(it)) } ?: "")
