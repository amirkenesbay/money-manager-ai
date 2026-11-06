package ai.moneymanager.service

import ai.moneymanager.domain.model.OperationDraft
import ai.moneymanager.domain.model.RecurringDraft
import ai.moneymanager.domain.model.RecurringMode
import ai.moneymanager.domain.model.RecurringSaveResult
import ai.moneymanager.repository.RecurringPaymentRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.RecurringPaymentEntity
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.LocalDate

private const val MAX_CATCH_UP = 31

data class RecordedOccurrence(val date: LocalDate, val operation: FinanceOperationEntity?)

@Service
class RecurringPaymentService(
    private val recurringPaymentRepository: RecurringPaymentRepository,
    private val financeOperationService: FinanceOperationService,
    private val categoryService: CategoryService,
) {
    fun payments(groupId: ObjectId): List<RecurringPaymentEntity> = recurringPaymentRepository.findByGroupIdOrderByNextDateAsc(groupId)

    fun create(actorId: Long, groupId: ObjectId, draft: RecurringDraft, firstRecorded: Boolean, maxActive: Int?, today: LocalDate): RecurringSaveResult {
        if (limitReached(actorId, maxActive)) return RecurringSaveResult.LimitReached(checkNotNull(maxActive))
        val next = if (firstRecorded) draft.recurrence.nextAfter(draft.startDate, draft.startDate) else draft.recurrence.firstOnOrAfter(draft.startDate, today)
        val payment = RecurringPaymentEntity(
            groupId = groupId,
            creatorId = actorId,
            type = draft.type,
            amount = draft.amount,
            categoryId = checkNotNull(draft.category.id),
            categoryName = draft.category.name,
            categoryIcon = draft.category.icon,
            description = draft.description,
            unit = draft.recurrence.unit,
            interval = draft.recurrence.interval,
            startDate = draft.startDate,
            endDate = draft.endDate,
            nextDate = next,
            mode = draft.mode,
        )
        return RecurringSaveResult.Saved(recurringPaymentRepository.save(payment.copy(active = !payment.finished)))
    }

    fun update(groupId: ObjectId, paymentId: ObjectId, draft: RecurringDraft, today: LocalDate): RecurringSaveResult {
        val existing = recurringPaymentRepository.findByGroupIdAndId(groupId, paymentId) ?: return RecurringSaveResult.NotFound
        val updated = existing.copy(
            type = draft.type,
            amount = draft.amount,
            categoryId = checkNotNull(draft.category.id),
            categoryName = draft.category.name,
            categoryIcon = draft.category.icon,
            description = draft.description,
            unit = draft.recurrence.unit,
            interval = draft.recurrence.interval,
            startDate = draft.startDate,
            endDate = draft.endDate,
            nextDate = draft.recurrence.firstOnOrAfter(draft.startDate, today),
            mode = draft.mode,
        )
        return RecurringSaveResult.Saved(recurringPaymentRepository.save(updated.copy(active = existing.active && !updated.finished)))
    }

    fun setActive(actorId: Long, groupId: ObjectId, paymentId: ObjectId, active: Boolean, maxActive: Int?, today: LocalDate): RecurringSaveResult {
        val existing = recurringPaymentRepository.findByGroupIdAndId(groupId, paymentId) ?: return RecurringSaveResult.NotFound
        if (active == existing.active) return RecurringSaveResult.Saved(existing)
        if (active && limitReached(actorId, maxActive)) return RecurringSaveResult.LimitReached(checkNotNull(maxActive))
        val next = if (active) existing.recurrence.firstOnOrAfter(existing.startDate, today) else existing.nextDate
        return RecurringSaveResult.Saved(recurringPaymentRepository.save(existing.copy(active = active, nextDate = next)))
    }

    fun delete(groupId: ObjectId, paymentId: ObjectId): Boolean {
        val existing = recurringPaymentRepository.findByGroupIdAndId(groupId, paymentId) ?: return false
        recurringPaymentRepository.delete(existing)
        return true
    }

    fun dueBy(date: LocalDate): List<RecurringPaymentEntity> = recurringPaymentRepository.findByActiveTrueAndNextDateLessThanEqual(date)

    fun recordDue(payment: RecurringPaymentEntity, today: LocalDate): List<RecordedOccurrence> {
        var current = payment
        val recorded = mutableListOf<RecordedOccurrence>()
        while (!current.nextDate.isAfter(today) && !current.finished && recorded.size < MAX_CATCH_UP) {
            val operation = if (current.mode == RecurringMode.AUTO) record(current, current.nextDate, current.creatorId) else null
            recorded += RecordedOccurrence(current.nextDate, operation)
            current = current.copy(nextDate = current.recurrence.nextAfter(current.startDate, current.nextDate))
        }
        recurringPaymentRepository.save(current.copy(active = !current.finished))
        return recorded
    }

    fun recordOccurrence(actorId: Long, paymentId: ObjectId, date: LocalDate): Pair<RecurringPaymentEntity, FinanceOperationEntity>? {
        val payment = recurringPaymentRepository.findById(paymentId).orElse(null) ?: return null
        return record(payment, date, actorId)?.let { payment to it }
    }

    fun undoOperation(actorId: Long, operationId: ObjectId): FinanceOperationEntity? = financeOperationService.deleteOwnOperation(actorId, operationId)

    fun claimHeadsUp(payment: RecurringPaymentEntity, tomorrow: LocalDate): Boolean {
        if (payment.nextDate != tomorrow || payment.headsUpSentFor == tomorrow) return false
        recurringPaymentRepository.save(payment.copy(headsUpSentFor = tomorrow))
        return true
    }

    private fun record(payment: RecurringPaymentEntity, date: LocalDate, actorId: Long): FinanceOperationEntity? {
        val category = categoryService.getCategory(payment.categoryId) ?: return null
        return financeOperationService.create(groupId = payment.groupId, creatorId = actorId, draft = OperationDraft(payment.type, payment.amount, category, date, payment.description))
    }

    fun activeCount(actorId: Long): Long = recurringPaymentRepository.countByCreatorIdAndActiveTrue(actorId)

    private fun limitReached(actorId: Long, maxActive: Int?): Boolean =
        maxActive != null && activeCount(actorId) >= maxActive
}
