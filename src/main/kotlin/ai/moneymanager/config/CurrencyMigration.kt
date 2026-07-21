package ai.moneymanager.config

import ai.moneymanager.domain.model.Currency
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component

private const val MONEY_GROUP_COLLECTION = "money_group"
private const val CURRENCY_FIELD = "currency"

/** Разовая идемпотентная миграция: у групп без поля currency (созданных до унификации валют) проставляем KZT. */
@Component
class CurrencyMigration(
    private val mongoTemplate: MongoTemplate
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val query = Query.query(Criteria.where(CURRENCY_FIELD).exists(false))
        val update = Update().set(CURRENCY_FIELD, Currency.DEFAULT.name)
        val result = mongoTemplate.updateMulti(query, update, MONEY_GROUP_COLLECTION)
        if (result.modifiedCount > 0) {
            log.info("Currency migration: set currency={} on {} group(s)", Currency.DEFAULT, result.modifiedCount)
        }
    }
}
