package ai.moneymanager.repository

import ai.moneymanager.repository.entity.AiRateLimitEntity
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Ручная реализация вместо обычного save(): нужен атомарный upsert+increment одной командой Mongo —
 * read-modify-write через стандартный репозиторий гонит гонку при параллельных запросах одного юзера.
 */
@Repository
class AiRateLimitRepositoryImpl(
    private val mongoTemplate: MongoTemplate
) : AiRateLimitRepositoryCustom {

    override fun incrementAndGet(telegramUserId: Long, date: LocalDate): AiRateLimitEntity {
        val query = Query(
            Criteria.where("telegramUserId").`is`(telegramUserId).and("date").`is`(date)
        )
        val update = Update().inc("count", 1).setOnInsert("createdAt", LocalDateTime.now())
        val options = FindAndModifyOptions.options().upsert(true).returnNew(true)

        // findAndModify с upsert(true) + returnNew(true) не возвращает null — но тип у API nullable
        return mongoTemplate.findAndModify(query, update, options, AiRateLimitEntity::class.java)
            ?: AiRateLimitEntity(telegramUserId = telegramUserId, date = date, count = 1)
    }
}
