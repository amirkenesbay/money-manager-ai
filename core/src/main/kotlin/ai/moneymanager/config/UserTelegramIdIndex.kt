package ai.moneymanager.config

import ai.moneymanager.repository.entity.UserInfoEntity
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.schema.JsonSchemaObject
import org.springframework.stereotype.Component

private const val TELEGRAM_USER_ID_FIELD = "telegramUserId"
private const val INDEX_NAME = "telegramUserId_unique"

@Component
class UserTelegramIdIndex(
    private val mongoTemplate: MongoTemplate
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        try {
            mongoTemplate.indexOps(UserInfoEntity::class.java).createIndex(uniqueTelegramUserIdIndex())
        } catch (exception: DataIntegrityViolationException) {
            log.error(
                "Unique index on {} was not created: duplicate users already exist. Remove the duplicates and restart.",
                TELEGRAM_USER_ID_FIELD,
                exception
            )
        }
    }

    private fun uniqueTelegramUserIdIndex(): Index =
        Index()
            .on(TELEGRAM_USER_ID_FIELD, Sort.Direction.ASC)
            .named(INDEX_NAME)
            .unique()
            .partial(PartialIndexFilter.of(Criteria.where(TELEGRAM_USER_ID_FIELD).type(JsonSchemaObject.Type.NUMBER)))
}
