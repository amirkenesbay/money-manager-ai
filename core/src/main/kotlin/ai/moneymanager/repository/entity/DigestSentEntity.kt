package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.DigestKind
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document(collection = "digest_sent")
@CompoundIndex(def = "{'telegramUserId': 1, 'kind': 1, 'periodStart': 1}", unique = true)
data class DigestSentEntity(
    val id: ObjectId? = null,
    val telegramUserId: Long,
    val kind: DigestKind,
    val periodStart: LocalDate,
)
