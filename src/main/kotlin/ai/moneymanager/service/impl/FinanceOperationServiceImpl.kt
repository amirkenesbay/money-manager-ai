package ai.moneymanager.service.impl

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.FinanceOperation
import ai.moneymanager.domain.model.financeOperation.OperationType
import ai.moneymanager.dto.CreateFinanceOperationRequestDto
import ai.moneymanager.dto.HistoryFinanceOperationDto
import ai.moneymanager.dto.UpdateFinanceOperationRequestDto
import ai.moneymanager.mapper.FinanceOperationMapper
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.CategoryEntity
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.FinanceOperationService
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class FinanceOperationServiceImpl(
    private val userInfoRepository: UserInfoRepository,
    private val moneyGroupRepository: MoneyGroupRepository,
    private val categoryRepository: CategoryRepository,
    private val financeOperationRepository: FinanceOperationRepository,
    private val financeOperationMapper: FinanceOperationMapper
) : FinanceOperationService {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Создать финансовую операцию
     */
    @Transactional
    override fun create(createDto: CreateFinanceOperationRequestDto): FinanceOperation {

        log.info(
            "Create finance operation: telegramUserId={}, groupId={}, categoryId={}, operationType={}, amount={}, currency={}",
            createDto.telegramUserId,
            createDto.groupId,
            createDto.categoryId,
            createDto.operationType,
            createDto.amount,
            createDto.currency
        )

        if (!userInfoRepository.existsByTelegramUserId(createDto.telegramUserId))
            throw IllegalStateException("User with telegramUserId=${createDto.telegramUserId} not found")

        checkUserIsOwnerOrMember(createDto.groupId, createDto.telegramUserId)

        createDto.categoryId?.let { categoryId ->
            val category = categoryRepository.findById(categoryId)
                .orElseThrow { IllegalStateException("Category with id=$categoryId not found") }

            if (category.groupId != createDto.groupId) throw IllegalStateException("Category doesn't belong to this group")

            val operationType = category.toOperationType()

            if (operationType != createDto.operationType)
                throw IllegalStateException(
                    "Category with id=${category.id} has type=${category.type}," +
                            " expected operationType=${createDto.operationType}"
                )
        }

        val financeOperation = FinanceOperation.create(
            telegramUserId = createDto.telegramUserId,
            groupId = createDto.groupId,
            categoryId = createDto.categoryId,
            day = createDto.day,
            amount = createDto.amount,
            operationType = createDto.operationType,
            currency = createDto.currency,
            description = createDto.description
        )

        val financeOperationDb = financeOperationRepository.save(financeOperationMapper.fromModel(financeOperation))
        return financeOperationMapper.fromDocument(financeOperationDb)
    }

    /**
     * Получить баланс группы
     */
    override fun getBalanceGroupFromGroupEntity(groupId: ObjectId, telegramUserId: Long): BigDecimal {
        log.info("Get balance for group: groupId={}, telegramUserId={}", groupId, telegramUserId)

        checkUserIsOwnerOrMember(groupId, telegramUserId)

        return financeOperationRepository.getBalanceByGroupIdFromGroupEntity(groupId) ?: BigDecimal.ZERO
    }

    /**
     * Получить историю финансовых операций у группы в конкретный период
     */
    override fun getHistoryFinanceOperationByPeriodFromGroupEntity(
        groupId: ObjectId, telegramUserId: Long, fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<HistoryFinanceOperationDto> {

        log.info(
            "Get finance operation history for group: groupId={}, telegramUserId={}, fromDate={}, toDate={}",
            groupId,
            telegramUserId,
            fromDate,
            toDate
        )

        checkUserIsOwnerOrMember(groupId, telegramUserId)

        if (fromDate.isAfter(toDate))
            throw IllegalStateException("Invalid period: fromDate=$fromDate is after toDate=$toDate")

        return financeOperationRepository.getHistoryFinanceOperationByPeriodFromGroupEntity(groupId, fromDate, toDate)
    }

    /**
     * Получить всю историю финансовых операций группы
     */
    override fun getAllHistoryFinanceOperationFromGroupEntity(
        groupId: ObjectId,
        telegramUserId: Long
    ): List<HistoryFinanceOperationDto> {
        log.info("Get all finance operation history for group: groupId={}, telegramUserId={}", groupId, telegramUserId)

        checkUserIsOwnerOrMember(groupId, telegramUserId)

        return financeOperationRepository.getAllHistoryFinanceOperationFromGroupEntity(groupId)
    }

    /**
     * Получить баланс группы за всю историю доходов группы
     */
    override fun getIncomeOperationBalanceForAllTimeFromGroup(groupId: ObjectId, telegramUserId: Long): BigDecimal {
        log.info("Get income balance for group: groupId={}, telegramUserId={}", groupId, telegramUserId)

        checkUserIsOwnerOrMember(groupId, telegramUserId)

        return financeOperationRepository.getIncomeBalanceByGroupIdFromGroupEntity(groupId) ?: BigDecimal.ZERO
    }

    /**
     * Получить баланс группы за всю историю расходов группы
     */
    override fun getExpenseOperationBalanceForAllTimeFromGroupEntity(
        groupId: ObjectId,
        telegramUserId: Long
    ): BigDecimal {
        log.info("Get expense balance for group: groupId={}, telegramUserId={}", groupId, telegramUserId)

        checkUserIsOwnerOrMember(groupId, telegramUserId)

        return financeOperationRepository.getExpenseBalanceByGroupIdFromGroupEntity(groupId) ?: BigDecimal.ZERO
    }

    /**
     * Получить сумму доходов по категории за весь период
     */
    override fun getIncomeBalanceByCategoryId(
        groupId: ObjectId,
        telegramUserId: Long,
        categoryId: ObjectId
    ): BigDecimal {
        log.info(
            "Get income balance by category: groupId={}, telegramUserId={}, categoryId={}",
            groupId,
            telegramUserId,
            categoryId
        )

        checkCategory(groupId, telegramUserId, categoryId, CategoryType.INCOME)

        return financeOperationRepository.getIncomeBalanceByCategoryIdFromCategoryEntity(categoryId) ?: BigDecimal.ZERO
    }

    /**
     * Получить сумму расходов по категории за весь период
     */
    override fun getExpenseBalanceByCategoryId(
        groupId: ObjectId,
        telegramUserId: Long,
        categoryId: ObjectId
    ): BigDecimal {
        log.info(
            "Get expense balance by category: groupId={}, telegramUserId={}, categoryId={}",
            groupId,
            telegramUserId,
            categoryId
        )

        checkCategory(groupId, telegramUserId, categoryId, CategoryType.EXPENSE)
        return financeOperationRepository.getExpenseBalanceByCategoryIdFromCategoryEntity(categoryId) ?: BigDecimal.ZERO
    }

    /**
     * Получить баланс категории за всю историю существование категории
     */
    override fun getBalanceCategoryFromCategoryEntity(
        groupId: ObjectId,
        telegramUserId: Long,
        categoryId: ObjectId
    ): BigDecimal {
        log.info(
            "Get category balance: groupId={}, telegramUserId={}, categoryId={}",
            groupId,
            telegramUserId,
            categoryId
        )
        checkCategory(groupId, telegramUserId, categoryId)
        return financeOperationRepository.getBalanceByCategoryIdFromCategoryEntity(categoryId) ?: BigDecimal.ZERO
    }

    /**
     * Получить историю финансовых операций у категории в конкретный период
     */
    override fun getHistoryFinanceOperationByPeriodFromCategoryEntity(
        groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId,
        fromDate: LocalDateTime, toDate: LocalDateTime
    ): List<HistoryFinanceOperationDto> {

        log.info(
            "Get finance operation history for category: groupId={}, telegramUserId={}, categoryId={}, fromDate={}, toDate={}",
            groupId,
            telegramUserId,
            categoryId,
            fromDate,
            toDate
        )

        checkCategory(groupId, telegramUserId, categoryId)

        if (fromDate.isAfter(toDate)) throw IllegalStateException("fromDate must be <= toDate")

        return financeOperationRepository.getHistoryFinanceOperationByPeriodFromCategoryEntity(
            categoryId,
            fromDate,
            toDate
        )
    }

    /**
     * Получить всю историю финансовых операций категории
     */
    override fun getAllHistoryFinanceOperationFromCategoryEntity(
        groupId: ObjectId,
        telegramUserId: Long,
        categoryId: ObjectId
    ): List<HistoryFinanceOperationDto> {
        log.info(
            "Get all finance operation history for category: groupId={}, telegramUserId={}, categoryId={}",
            groupId,
            telegramUserId,
            categoryId
        )

        checkCategory(groupId, telegramUserId, categoryId)

        return financeOperationRepository.getAllHistoryFinanceOperationFromCategoryEntity(categoryId)
    }

    /**
     * Изменить запись
     */
    @Transactional
    override fun updateFinanceOperation(id: ObjectId, updateDto: UpdateFinanceOperationRequestDto): FinanceOperation {
        log.info(
            "Update finance operation: id={}, telegramUserId={}, groupId={}, categoryId={}, day={}, amount={}, currency={}",
            id,
            updateDto.telegramUserId,
            updateDto.groupId,
            updateDto.categoryId,
            updateDto.day,
            updateDto.amount,
            updateDto.currency
        )

        val currentFinanceOperation = getFinanceOperation(id)

        validUser(currentFinanceOperation, updateDto.telegramUserId)

        val groupId = updateDto.groupId ?: currentFinanceOperation.groupId

        checkUserIsOwnerOrMember(currentFinanceOperation.groupId, updateDto.telegramUserId)

        if (updateDto.groupId != null && groupId != currentFinanceOperation.groupId) {
            checkUserIsOwnerOrMember(groupId, updateDto.telegramUserId)
        }

        val categoryId = updateDto.categoryId ?: currentFinanceOperation.categoryId
        ?: throw IllegalStateException("Category is required for finance operation id=$id")

        val category = getCategory(categoryId)
        validateCategoryBelongsToGroup(groupId, category)

        val updatedDomain = FinanceOperation.update(
            telegramUserId = currentFinanceOperation.telegramUserId,
            groupId = groupId,
            categoryId = categoryId,
            day = updateDto.day ?: currentFinanceOperation.day,
            amount = updateDto.amount ?: currentFinanceOperation.amount,
            operationType = category.toOperationType(),
            currency = updateDto.currency ?: currentFinanceOperation.currency,
            description = updateDto.description?.trim()?.takeIf { it.isNotBlank() }
                ?: currentFinanceOperation.description
        ).copy(id = currentFinanceOperation.id)

        val saved = financeOperationRepository.save(financeOperationMapper.fromModel(updatedDomain))

        log.info(
            "Finance operation updated: id={}, telegramUserId={}, groupId={}, categoryId={}, operationType={}, amount={}, currency={}",
            updatedDomain.id,
            updatedDomain.telegramUserId,
            updatedDomain.groupId,
            updatedDomain.categoryId,
            updatedDomain.operationType,
            updatedDomain.amount,
            updatedDomain.currency
        )

        return financeOperationMapper.fromDocument(saved)
    }

    /**
     * Удалить запись
     */
    override fun deleteFinanceOperation(id: ObjectId, telegramUserId: Long) {
        log.info("Delete finance operation: id={}, telegramUserId={}", id, telegramUserId)

        val currentFinanceOperation = getFinanceOperation(id)

        validUser(currentFinanceOperation, telegramUserId)

        checkUserIsOwnerOrMember(currentFinanceOperation.groupId, telegramUserId)

        log.info("Finance operation deleted successfully: id={}, telegramUserId={}", id, telegramUserId)

        financeOperationRepository.delete(currentFinanceOperation)
    }

    /**
     * Получить категорию по categoryId.
     */
    private fun getCategory(categoryId: ObjectId) =
        categoryRepository.findById(categoryId)
            .orElseThrow { IllegalStateException("Category with id=$categoryId not found") }

    /**
     * Проверить, что пользователь является owner или member группы.
     */
    private fun checkUserIsOwnerOrMember(groupId: ObjectId, telegramUserId: Long) {
        if (!moneyGroupRepository.existsByIdAndOwnerOrMemberTelegramUserId(groupId, telegramUserId)) {
            throw IllegalStateException("User with telegramUserId=$telegramUserId is not a member of groupId=$groupId")
        }
    }

    /**
     * Проверяет, что категория принадлежит указанной группе.
     */
    private fun validateCategoryBelongsToGroup(groupId: ObjectId, category: CategoryEntity) {
        if (category.groupId != groupId)
            throw IllegalStateException("Category with id=${category.id} does not belong to groupId=$groupId")
    }

    /**
     * Проверяет, что категория (INCOME/EXPENSE).
     */
    private fun validateCategoryType(categoryEntity: CategoryEntity, categoryType: CategoryType) {
        if (categoryEntity.type != categoryType)
            throw IllegalStateException(
                "Category with id=${categoryEntity.id} has type=${categoryEntity.type}, expected=$categoryType"
            )
    }

    /**
     * Проверка доступа пользователя к группе, существование категории,
     * принадлежность категории группе, соответствие типа категории
     */
    private fun checkCategory(
        groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId,
        categoryType: CategoryType? = null
    ) {

        checkUserIsOwnerOrMember(groupId, telegramUserId)

        val category = getCategory(categoryId)

        validateCategoryBelongsToGroup(groupId, category)

        if (categoryType != null) validateCategoryType(category, categoryType)
    }

    /**
     * Получить финансовую операцию по id
     */
    private fun getFinanceOperation(id: ObjectId) =
        financeOperationRepository.findById(id)
            .orElseThrow { IllegalStateException("Finance operation with id=$id not found") }

    /**
     * Проверка, что юзер является автором записи
     */
    private fun validUser(financeOperationEntity: FinanceOperationEntity, telegramUserId: Long) {
        if (financeOperationEntity.telegramUserId != telegramUserId) {
            throw IllegalStateException(
                "Finance operation id=${financeOperationEntity.id} does not belong to telegramUserId=$telegramUserId"
            )
        }
    }

    /**
     * Преобразование в нужный тип категории
     */
    private fun CategoryEntity.toOperationType(): OperationType =
        when (this.type) {
            CategoryType.EXPENSE -> OperationType.EXPENSE
            CategoryType.INCOME -> OperationType.INCOME
        }
}