package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.event.OperationChange
import ai.moneymanager.domain.event.OperationChanged
import ai.moneymanager.domain.event.sameContentAs
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant

private val QUIET_PERIOD: Duration = Duration.ofMinutes(2)
private val LONGEST_WAIT: Duration = Duration.ofMinutes(10)

data class OperationBatch(val groupId: ObjectId, val actorId: Long, val changes: List<OperationChange>)

private data class BatchKey(val groupId: ObjectId, val actorId: Long)

private data class PendingBatch(val changes: List<OperationChange>, val firstAt: Instant, val lastAt: Instant)

@Component
class OperationNoticeBuffer(
    private val clock: Clock,
) {
    private val pending = mutableMapOf<BatchKey, PendingBatch>()

    @Synchronized
    fun add(event: OperationChanged) {
        val now = clock.instant()
        val key = BatchKey(event.groupId, event.actorId)
        val batch = pending[key]
        pending[key] = batch?.copy(changes = batch.changes + event.change, lastAt = now) ?: PendingBatch(listOf(event.change), now, now)
    }

    @Synchronized
    fun drainReady(): List<OperationBatch> {
        val now = clock.instant()
        val ready = pending.filterValues { isReady(it, now) }
        ready.keys.forEach { pending.remove(it) }
        return ready.map { (key, batch) -> OperationBatch(key.groupId, key.actorId, collapse(batch.changes)) }
            .filter { it.changes.isNotEmpty() }
    }

    private fun isReady(batch: PendingBatch, now: Instant): Boolean =
        !now.isBefore(batch.lastAt + QUIET_PERIOD) || !now.isBefore(batch.firstAt + LONGEST_WAIT)
}

fun collapse(changes: List<OperationChange>): List<OperationChange> =
    changes.groupBy { it.operationId }.values.mapNotNull { history -> merge(history.first(), history.last()) }

private fun merge(first: OperationChange, last: OperationChange): OperationChange? = when {
    first is OperationChange.Added && last is OperationChange.Deleted -> null
    first is OperationChange.Added -> OperationChange.Added(finalState(last))
    last is OperationChange.Deleted -> OperationChange.Deleted(initialState(first))
    first is OperationChange.Edited -> OperationChange.Edited(first.before, finalState(last)).takeIf { !it.before.sameContentAs(it.after) }
    else -> last
}

private fun initialState(change: OperationChange) = when (change) {
    is OperationChange.Added -> change.operation
    is OperationChange.Edited -> change.before
    is OperationChange.Deleted -> change.operation
}

private fun finalState(change: OperationChange) = when (change) {
    is OperationChange.Added -> change.operation
    is OperationChange.Edited -> change.after
    is OperationChange.Deleted -> change.operation
}
