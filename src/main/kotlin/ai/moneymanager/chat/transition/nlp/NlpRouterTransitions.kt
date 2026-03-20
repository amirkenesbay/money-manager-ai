package ai.moneymanager.chat.transition.nlp

import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import gcardone.junidecode.Junidecode
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.EventType

internal fun matchesGroupName(groupName: String, searchName: String): Boolean {
    val normalizedGroupName = groupName.lowercase()
    val normalizedSearchName = searchName.lowercase()

    if (normalizedGroupName == normalizedSearchName) return true

    val translitGroupName = Junidecode.unidecode(normalizedGroupName).lowercase()
    val translitSearchName = Junidecode.unidecode(normalizedSearchName).lowercase()

    return translitGroupName == translitSearchName
}

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.createNlpRouter(
    sourceState: MoneyManagerState,
    targetState: MoneyManagerState
) {
    transition {
        name = "Route NLP: ${sourceState.name} → ${targetState.name}"

        condition {
            from = sourceState
            eventType = EventType.TRIGGERED
            guard {
                val matches = context.parsedCommand?.targetState == targetState
                log.info("🔍 NLP Router (${sourceState.name}→${targetState.name}): command=${context.parsedCommand}, match=$matches")
                matches
            }
        }

        action {
            log.info("🎯 Routing from ${sourceState.name} to ${targetState.name}")
            context.parsedCommand = null
        }

        then {
            to = targetState
        }
    }
}