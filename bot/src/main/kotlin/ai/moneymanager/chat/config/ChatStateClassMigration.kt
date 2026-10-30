package ai.moneymanager.chat.config

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

private const val CHATS_COLLECTION = "chats"
private const val LEGACY_PACKAGE = "ai.moneymanager.domain.model"
private const val DIALOG_FILTER = "dialog"
private const val STATE_CLASS_FIELD = "stateClass"
private const val CONTEXT_CLASS_FIELD = "context._class"

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ChatStateClassMigration(
    private val mongoTemplate: MongoTemplate
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        renameDialogClass(STATE_CLASS_FIELD, MoneyManagerState::class)
        renameDialogClass(CONTEXT_CLASS_FIELD, MoneyManagerContext::class)
    }

    private fun renameDialogClass(field: String, currentClass: KClass<*>) {
        val legacyName = "$LEGACY_PACKAGE.${currentClass.simpleName}"
        val currentName = requireNotNull(currentClass.qualifiedName)
        val query = Query.query(Criteria.where("dialogs.$field").`is`(legacyName))
        val update = Update()
            .set("dialogs.$[$DIALOG_FILTER].$field", currentName)
            .filterArray(Criteria.where("$DIALOG_FILTER.$field").`is`(legacyName))
        val result = mongoTemplate.updateMulti(query, update, CHATS_COLLECTION)
        if (result.modifiedCount > 0) {
            log.info("Chat migration: {} -> {} in {} chat(s)", legacyName, currentName, result.modifiedCount)
        }
    }
}
