package ai.moneymanager.service.nlp

import ai.moneymanager.domain.text.geminiValue
import ai.moneymanager.domain.model.ExpenseShare
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.text.matchesEntityName

const val SPEAKER_TOKEN = "ME"
const val EVERYONE_TOKEN = "ALL"

private const val NAMES_SEPARATOR = ","

fun expenseShareOf(command: BotCommand, speakerId: Long, members: Map<Long, String>): ExpenseShare? {
    if (command !is BotCommand.AddExpense) return null
    val paidBy = command.paidBy.geminiValue()
    val splitWith = command.splitWith.geminiValue()
    if (paidBy == null && splitWith == null) return null
    val payerId = paidBy?.let { personOf(it, speakerId, members) ?: return null } ?: speakerId
    val people = if (splitWith == null || splitWith.equals(EVERYONE_TOKEN, ignoreCase = true)) {
        members.keys
    } else {
        splitWith.split(NAMES_SEPARATOR).mapNotNull { it.geminiValue() }.map { personOf(it, speakerId, members) ?: return null }.toSet()
    }
    return people.takeIf { it.isNotEmpty() }?.let { ExpenseShare(payerId, it) }
}

private fun personOf(name: String, speakerId: Long, members: Map<Long, String>): Long? =
    if (name.equals(SPEAKER_TOKEN, ignoreCase = true)) speakerId else members.entries.firstOrNull { matchesEntityName(it.value, name) }?.key
