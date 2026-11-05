package ai.moneymanager.service.imports

import ai.moneymanager.domain.model.imports.ColumnMapping
import ai.moneymanager.domain.model.imports.ImportSource
import ai.moneymanager.domain.model.imports.ImportTable
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val DRAFT_LIFETIME: Duration = Duration.ofHours(1)

data class ImportDraft(
    val token: String,
    val userId: Long,
    val groupId: ObjectId,
    val table: ImportTable,
    val source: ImportSource?,
    val mapping: ColumnMapping?,
    val createdAt: Instant,
)

@Component
class ImportDrafts(
    private val clock: Clock,
) {
    private val drafts = ConcurrentHashMap<String, ImportDraft>()

    fun create(userId: Long, groupId: ObjectId, table: ImportTable, source: ImportSource?, mapping: ColumnMapping?): ImportDraft {
        val now = clock.instant()
        drafts.values.removeIf { it.createdAt.plus(DRAFT_LIFETIME).isBefore(now) }
        return ImportDraft(UUID.randomUUID().toString(), userId, groupId, table, source, mapping, now).also { drafts[it.token] = it }
    }

    fun find(token: String, userId: Long, groupId: ObjectId): ImportDraft? =
        drafts[token]?.takeIf { it.userId == userId && it.groupId == groupId && it.createdAt.plus(DRAFT_LIFETIME).isAfter(clock.instant()) }

    fun update(draft: ImportDraft): ImportDraft = draft.also { drafts[it.token] = it }

    fun remove(token: String) {
        drafts.remove(token)
    }
}
