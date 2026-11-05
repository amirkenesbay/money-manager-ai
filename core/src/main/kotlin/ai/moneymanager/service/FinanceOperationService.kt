package ai.moneymanager.service

import ai.moneymanager.domain.model.BalanceBreakdown
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.ExpenseShare
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.OperationDraft
import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.domain.model.OperationTotals
import ai.moneymanager.domain.text.matchesSearch
import ai.moneymanager.domain.text.tagsOf
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.LoanRepository
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import ai.moneymanager.domain.event.CategoryCorrected
import ai.moneymanager.domain.event.OperationChange
import ai.moneymanager.domain.event.OperationChanged
import ai.moneymanager.domain.event.OperationsChanged
import ai.moneymanager.domain.event.sameContentAs
import org.springframework.context.ApplicationEventPublisher

@Service
class FinanceOperationService(
    private val financeOperationRepository: FinanceOperationRepository,
    private val moneyGroupRepository: MoneyGroupRepository,
    private val loanRepository: LoanRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(FinanceOperationService::class.java)

    fun calculateBalance(groupId: ObjectId, initialBalance: BigDecimal): BalanceBreakdown {
        val ops = financeOperationRepository.findByGroupId(groupId)
        val income = sumOf(ops, CategoryType.INCOME)
        val expense = sumOf(ops, CategoryType.EXPENSE)
        val loans = loanRepository.findByGroupId(groupId).fold(BigDecimal.ZERO) { total, loan -> total + loan.cashEffect }
        return BalanceBreakdown(
            initial = initialBalance,
            income = income,
            expense = expense,
            total = initialBalance + income - expense + loans,
            loans = loans,
        )
    }

    fun findOperations(filter: OperationFilter, page: Int, size: Int): Page<FinanceOperationEntity> {
        val pageable = PageRequest.of(page, size)
        if (!filter.isSearch) return financeOperationRepository.findPage(filter, pageable)
        val matches = matching(filter)
        val from = minOf(pageable.offset.toInt(), matches.size)
        return PageImpl(matches.subList(from, minOf(from + size, matches.size)), pageable, matches.size.toLong())
    }

    fun totals(filter: OperationFilter): OperationTotals {
        val matches = matching(filter)
        return OperationTotals(sumOf(matches, CategoryType.INCOME), sumOf(matches, CategoryType.EXPENSE), matches.size)
    }

    fun recentTags(groupId: ObjectId, limit: Int): List<String> =
        financeOperationRepository.findAll(OperationFilter(groupId)).asSequence()
            .flatMap { tagsOf(it.description) }
            .distinct()
            .take(limit)
            .toList()

    private fun matching(filter: OperationFilter): List<FinanceOperationEntity> =
        financeOperationRepository.findAll(filter).filter { operation -> filter.query?.let { matchesSearch(operation, it) } != false }

    private fun sumOf(operations: List<FinanceOperationEntity>, type: CategoryType): BigDecimal =
        operations.filter { it.type == type }.fold(BigDecimal.ZERO) { total, operation -> total + operation.amount }

    fun save(
        groupId: ObjectId,
        creatorId: Long,
        type: CategoryType,
        amount: Double,
        categoryId: ObjectId,
        categoryName: String,
        categoryIcon: String?,
        operationDate: LocalDate,
        description: String?
    ): FinanceOperationEntity =
        saveOperation(groupId, creatorId, type, BigDecimal.valueOf(amount), categoryId, categoryName, categoryIcon, operationDate, description, null)

    private fun saveOperation(
        groupId: ObjectId,
        creatorId: Long,
        type: CategoryType,
        amount: BigDecimal,
        categoryId: ObjectId,
        categoryName: String,
        categoryIcon: String?,
        operationDate: LocalDate,
        description: String?,
        requestedShare: ExpenseShare?,
    ): FinanceOperationEntity {
        val share = shareOf(groupId, type, requestedShare, creatorId)
        val entity = FinanceOperationEntity(
            groupId = groupId,
            creatorId = creatorId,
            type = type,
            amount = amount,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            operationDate = operationDate,
            description = description,
            payerId = share?.payerId,
            participantIds = share?.participantIds,
        )

        val saved = financeOperationRepository.save(entity)
        log.info("Saved finance operation: type={}, amount={}, category={}, groupId={}", type, amount, categoryName, groupId)
        eventPublisher.publishEvent(OperationsChanged(groupId))
        eventPublisher.publishEvent(OperationChanged(groupId, creatorId, OperationChange.Added(saved)))
        return saved
    }

    fun create(groupId: ObjectId, creatorId: Long, draft: OperationDraft, categoryCorrected: Boolean = false): FinanceOperationEntity {
        val categoryId = checkNotNull(draft.category.id)
        val saved = saveOperation(groupId, creatorId, draft.type, draft.amount, categoryId, draft.category.name, draft.category.icon, draft.operationDate, draft.description, draft.share)
        if (categoryCorrected) publishCorrection(saved)
        return saved
    }

    fun replace(actorId: Long, groupId: ObjectId, operationId: ObjectId, draft: OperationDraft): FinanceOperationEntity? =
        updateOperation(actorId, groupId, operationId) {
            val share = shareOf(groupId, draft.type, draft.share ?: it.share, it.creatorId)
            it.copy(
                type = draft.type,
                amount = draft.amount,
                categoryId = checkNotNull(draft.category.id),
                categoryName = draft.category.name,
                categoryIcon = draft.category.icon,
                operationDate = draft.operationDate,
                description = draft.description,
                payerId = share?.payerId,
                participantIds = share?.participantIds,
            )
        }

    private fun shareOf(groupId: ObjectId, type: CategoryType, requested: ExpenseShare?, defaultPayerId: Long): ExpenseShare? {
        if (type != CategoryType.EXPENSE) return null
        val group = moneyGroupRepository.findById(groupId).orElse(null) ?: return null
        if (!group.splitExpenses || group.type != GroupType.SHARED) return null
        return requested?.takeIf { it.fitsMembers(group.memberIds) } ?: ExpenseShare(defaultPayerId, group.memberIds)
    }

    fun getOperation(groupId: ObjectId, operationId: ObjectId): FinanceOperationEntity? =
        financeOperationRepository.findByGroupIdAndIdIn(groupId, listOf(operationId)).firstOrNull()

    fun updateAmount(actorId: Long, groupId: ObjectId, operationId: ObjectId, amount: BigDecimal): FinanceOperationEntity? =
        updateOperation(actorId, groupId, operationId) { it.copy(amount = amount) }

    fun updateType(actorId: Long, groupId: ObjectId, operationId: ObjectId, type: CategoryType): FinanceOperationEntity? =
        updateOperation(actorId, groupId, operationId) { it.copy(type = type) }

    fun updateDate(actorId: Long, groupId: ObjectId, operationId: ObjectId, date: LocalDate): FinanceOperationEntity? =
        updateOperation(actorId, groupId, operationId) { it.copy(operationDate = date) }

    fun updateDescription(actorId: Long, groupId: ObjectId, operationId: ObjectId, description: String?): FinanceOperationEntity? =
        updateOperation(actorId, groupId, operationId) { it.copy(description = description) }

    fun updateCategory(
        actorId: Long,
        groupId: ObjectId,
        operationId: ObjectId,
        categoryId: ObjectId,
        categoryName: String,
        categoryIcon: String?
    ): FinanceOperationEntity? = updateOperation(actorId, groupId, operationId) {
        it.copy(categoryId = categoryId, categoryName = categoryName, categoryIcon = categoryIcon)
    }

    fun deleteOwnOperation(actorId: Long, operationId: ObjectId): FinanceOperationEntity? {
        val operation = financeOperationRepository.findById(operationId).orElse(null) ?: return null
        if (operation.creatorId != actorId) return null
        return operation.takeIf { deleteOperation(actorId, it.groupId, operationId) }
    }

    fun deleteOperation(actorId: Long, groupId: ObjectId, operationId: ObjectId): Boolean {
        val existing = getOperation(groupId, operationId) ?: return false
        financeOperationRepository.delete(existing)
        log.info("Deleted finance operation: id={}, groupId={}", operationId, groupId)
        eventPublisher.publishEvent(OperationsChanged(groupId))
        eventPublisher.publishEvent(OperationChanged(groupId, actorId, OperationChange.Deleted(existing)))
        return true
    }

    private fun updateOperation(
        actorId: Long,
        groupId: ObjectId,
        operationId: ObjectId,
        mutate: (FinanceOperationEntity) -> FinanceOperationEntity
    ): FinanceOperationEntity? {
        val existing = getOperation(groupId, operationId) ?: return null
        val saved = financeOperationRepository.save(mutate(existing))
        log.info("Updated finance operation: id={}, groupId={}", operationId, groupId)
        eventPublisher.publishEvent(OperationsChanged(groupId))
        if (!existing.sameContentAs(saved)) eventPublisher.publishEvent(OperationChanged(groupId, actorId, OperationChange.Edited(existing, saved)))
        if (existing.categoryId != saved.categoryId) publishCorrection(saved)
        return saved
    }

    private fun publishCorrection(saved: FinanceOperationEntity) {
        eventPublisher.publishEvent(CategoryCorrected(saved.groupId, saved.description, saved.categoryId))
    }
}
