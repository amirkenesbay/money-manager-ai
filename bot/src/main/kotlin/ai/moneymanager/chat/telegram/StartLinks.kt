package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.domain.model.StartParameters
import org.bson.types.ObjectId
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val START_LINK_TEMPLATE = "https://t.me/%s?start=%s"
private const val START_COMMAND = "/start "
private const val PARAMETER_SEPARATOR = "_"

fun startLink(botUsername: String, parameter: String): String = START_LINK_TEMPLATE.format(botUsername, parameter)

fun startParameterOf(messageText: String?): String? =
    messageText?.takeIf { it.startsWith(START_COMMAND) }?.removePrefix(START_COMMAND)?.trim()

fun digestMuteParameter(kind: DigestKind): String = StartParameters.DIGEST_MUTE_PREFIX + kind.name.lowercase()

fun mutedDigestKind(messageText: String?): DigestKind? {
    val parameter = startParameterOf(messageText) ?: return null
    return DigestKind.entries.firstOrNull { digestMuteParameter(it) == parameter }
}

fun undoOperationParameter(operationId: ObjectId): String = StartParameters.UNDO_OPERATION_PREFIX + operationId.toHexString()

fun undoOperationId(messageText: String?): ObjectId? =
    startParameterOf(messageText)
        ?.takeIf { it.startsWith(StartParameters.UNDO_OPERATION_PREFIX) }
        ?.removePrefix(StartParameters.UNDO_OPERATION_PREFIX)
        ?.takeIf { ObjectId.isValid(it) }
        ?.let { ObjectId(it) }

data class RecurringRecordRequest(val paymentId: ObjectId, val date: LocalDate)

fun recordRecurringParameter(paymentId: ObjectId, date: LocalDate): String =
    StartParameters.RECORD_RECURRING_PREFIX + paymentId.toHexString() + PARAMETER_SEPARATOR + date

fun recurringRecordRequest(messageText: String?): RecurringRecordRequest? {
    val parameter = startParameterOf(messageText)?.takeIf { it.startsWith(StartParameters.RECORD_RECURRING_PREFIX) } ?: return null
    val (paymentId, date) = parameter.removePrefix(StartParameters.RECORD_RECURRING_PREFIX).split(PARAMETER_SEPARATOR).takeIf { it.size == 2 } ?: return null
    if (!ObjectId.isValid(paymentId)) return null
    return try {
        RecurringRecordRequest(ObjectId(paymentId), LocalDate.parse(date))
    } catch (exception: DateTimeParseException) {
        null
    }
}
