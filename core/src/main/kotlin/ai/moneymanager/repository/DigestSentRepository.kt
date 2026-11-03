package ai.moneymanager.repository

import ai.moneymanager.domain.model.DigestKind
import ai.moneymanager.repository.entity.DigestSentEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.LocalDate

interface DigestSentRepository : MongoRepository<DigestSentEntity, ObjectId> {
    fun existsByTelegramUserIdAndKindAndPeriodStart(telegramUserId: Long, kind: DigestKind, periodStart: LocalDate): Boolean
}
