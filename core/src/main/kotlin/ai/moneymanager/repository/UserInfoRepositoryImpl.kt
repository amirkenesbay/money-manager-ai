package ai.moneymanager.repository

import ai.moneymanager.domain.model.AppHint
import ai.moneymanager.repository.entity.UserInfoEntity
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

private const val TELEGRAM_USER_ID_FIELD = "telegramUserId"
private const val SEEN_HINTS_FIELD = "seenHints"
private const val ID_FIELD = "_id"
private const val BOT_BLOCKED_AT_FIELD = "botBlockedAt"

@Repository
class UserInfoRepositoryImpl(
    private val mongoTemplate: MongoTemplate
) : UserInfoRepositoryCustom {
    override fun addSeenHint(telegramUserId: Long, hint: AppHint): UserInfoEntity? {
        val query = Query(Criteria.where(TELEGRAM_USER_ID_FIELD).`is`(telegramUserId)).with(Sort.by(Sort.Direction.ASC, ID_FIELD))
        val update = Update().addToSet(SEEN_HINTS_FIELD, hint)
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), UserInfoEntity::class.java)
    }

    override fun setBotBlockedAt(telegramUserId: Long, blockedAt: LocalDateTime?): Boolean {
        val stillOpposite = if (blockedAt == null) Criteria.where(BOT_BLOCKED_AT_FIELD).ne(null) else Criteria.where(BOT_BLOCKED_AT_FIELD).`is`(null)
        val query = Query(Criteria.where(TELEGRAM_USER_ID_FIELD).`is`(telegramUserId).andOperator(stillOpposite))
        val update = if (blockedAt == null) Update().unset(BOT_BLOCKED_AT_FIELD) else Update().set(BOT_BLOCKED_AT_FIELD, blockedAt)
        return mongoTemplate.updateMulti(query, update, UserInfoEntity::class.java).modifiedCount > 0
    }
}
