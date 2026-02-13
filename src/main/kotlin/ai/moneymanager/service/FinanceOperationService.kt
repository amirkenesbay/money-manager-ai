package ai.moneymanager.service

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.FinanceOperation
import ai.moneymanager.domain.model.financeOperation.OperationType
import ai.moneymanager.dto.CreateFinanceOperationRequestDto
import ai.moneymanager.dto.HistoryFinanceOperationDto
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.CategoryEntity
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class FinanceOperationService(
    private val userInfoRepository: UserInfoRepository,
    private val moneyGroupRepository: MoneyGroupRepository,
    private val categoryRepository: CategoryRepository,
    private val financeOperationRepository: FinanceOperationRepository
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Создать финансовую операцию
     */
    @Transactional
    fun create(createDto: CreateFinanceOperationRequestDto): FinanceOperationEntity {

        log.info("Create finance operation: $createDto")

        if (createDto.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Amount must be greater than zero")
        }

        if (!userInfoRepository.existsByTelegramUserId(createDto.telegramUserId)) throw IllegalStateException("User not found")

        checkAccessToTheGroup(createDto.groupId, createDto.telegramUserId)

        createDto.categoryId?.let { categoryId ->
            val category = categoryRepository.findById(categoryId)
                .orElseThrow { IllegalStateException("Category not found") }

            if (category.groupId != createDto.groupId) throw IllegalStateException("Category doesn't belong to this group")

            val operationType = when (category.type) {
                CategoryType.EXPENSE -> OperationType.EXPENSE
                CategoryType.INCOME -> OperationType.INCOME
            }

            if (operationType != createDto.operationType) throw IllegalStateException("Incorrect category type")
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

        val financeOperationEntity = FinanceOperationEntity.from(financeOperation)

        log.info("The financial transaction has been successfully created: {}", financeOperationEntity)

        return financeOperationRepository.save(financeOperationEntity)
    }

    /**
     * Получить полную сумму всей группы
     */
    fun getBalanceGroupFromGroupEntity(groupId: ObjectId, telegramUserId: Long): BigDecimal {
        log.info("Get balance for group: {}", groupId)

        checkAccessToTheGroup(groupId, telegramUserId)

        return financeOperationRepository.getBalanceByGroupIdFromGroupEntity(groupId) ?: BigDecimal.ZERO
    }

    /**
     * Получить историю финансовых операций у группы в конкретный период
     */
    fun getHistoryFinanceOperationByPeriodFromGroupEntity(groupId: ObjectId, telegramUserId: Long, fromDate: LocalDateTime,
                                                          toDate: LocalDateTime): List<HistoryFinanceOperationDto> {

        log.info("Get history finance operation by period: {} - {}", fromDate, toDate)

        checkAccessToTheGroup(groupId, telegramUserId)

        if (fromDate.isAfter(toDate)) throw IllegalStateException("fromDate must be <= toDate")

        return financeOperationRepository.getHistoryFinanceOperationByPeriodFromGroupEntity(groupId, fromDate, toDate)
    }

    /**
     * Получить всю историю финансовых операций группы
     */
    fun getAllHistoryFinanceOperationFromGroupEntity(groupId: ObjectId, telegramUserId: Long): List<HistoryFinanceOperationDto> {
        log.info("Get all history finance operation for group: {}", groupId)

        checkAccessToTheGroup(groupId, telegramUserId)

        return financeOperationRepository.getAllHistoryFinanceOperationFromGroupEntity(groupId)
    }

    /**
     * Получить баланс группы за всю историю доходов группы
     */
    fun getIncomeOperationBalanceForAllTimeFromGroup(groupId: ObjectId, telegramUserId: Long): BigDecimal {
        log.info("Get income operation for group: {}", groupId)

        checkAccessToTheGroup(groupId, telegramUserId)

        return financeOperationRepository.getIncomeBalanceByGroupIdFromGroupEntity(groupId) ?: BigDecimal.ZERO
    }

    /**
     * Получить баланс группы за всю историю расходов группы
     */
    fun getExpenseOperationBalanceForAllTimeFromGroupEntity(groupId: ObjectId, telegramUserId: Long): BigDecimal {
        log.info("Get expense operation for group: {}", groupId)

        checkAccessToTheGroup(groupId, telegramUserId)

        return financeOperationRepository.getExpenseBalanceByGroupIdFromGroupEntity(groupId) ?: BigDecimal.ZERO
    }

    /**
     * Получить сумму доходов по категории за весь период
     */
    fun getIncomeBalanceByCategoryId(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId): BigDecimal {
        log.info("Get income balance for category: {}", categoryId)

        checkCategory(groupId, telegramUserId, categoryId, CategoryType.INCOME)

        return financeOperationRepository.getIncomeBalanceByCategoryIdFromCategoryEntity(categoryId) ?: BigDecimal.ZERO
    }

    /**
     * Получить сумму расходов по категории за весь период
     */
    fun getExpenseBalanceByCategoryId(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId): BigDecimal {
        log.info("Get expense balance for category: {}", categoryId)

        checkCategory(groupId, telegramUserId, categoryId, CategoryType.EXPENSE)
        return financeOperationRepository.getExpenseBalanceByCategoryIdFromCategoryEntity(categoryId) ?: BigDecimal.ZERO
    }

    /**
     * Получить баланс категории за всю историю существование категории
     */
    fun getBalanceCategoryFromCategoryEntity(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId): BigDecimal {
        log.info("Get balance category for group: {}", groupId)
        checkCategory(groupId, telegramUserId, categoryId)
        return financeOperationRepository.getBalanceByCategoryIdFromCategoryEntity(categoryId) ?: BigDecimal.ZERO
    }

    /**
     * Получить историю финансовых операций у категории в конкретный период
     */
    fun getHistoryFinanceOperationByPeriodFromCategoryEntity(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId,
                                                             fromDate: LocalDateTime, toDate: LocalDateTime): List<HistoryFinanceOperationDto> {

        log.info("Get history finance operation by period: {} - {}", fromDate, toDate)

        checkCategory(groupId, telegramUserId, categoryId)

        if (fromDate.isAfter(toDate)) throw IllegalStateException("fromDate must be <= toDate")

        return financeOperationRepository.getHistoryFinanceOperationByPeriodFromCategoryEntity(categoryId, fromDate, toDate)
    }

    /**
     * Получить всю историю финансовых операций категории
     */
    fun getAllHistoryFinanceOperationFromCategoryEntity(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId): List<HistoryFinanceOperationDto> {
        log.info("Get history finance operation by category: {}", categoryId)

        checkCategory(groupId, telegramUserId, categoryId)

        return financeOperationRepository.getAllHistoryFinanceOperationFromCategoryEntity(categoryId)
    }

    /**
     * Получить categoryId.
     */
    private fun getCategoryOrThrow(categoryId: ObjectId) =
        categoryRepository.findById(categoryId).orElseThrow { IllegalStateException("Category not found") }

    /**
     * Проверить, что пользователь является owner или member группы.
     */
    private fun checkAccessToTheGroup(groupId: ObjectId, telegramUserId: Long) {
        if (!moneyGroupRepository.existsByIdAndOwnerOrMemberTelegramUserId(groupId, telegramUserId))
            throw IllegalStateException("User is not a member of the group")
    }

    /**
     * Проверяет, что категория принадлежит указанной группе.
     */
    private fun validateCategoryBelongsToGroup(groupId: ObjectId, category: CategoryEntity) {
        if (category.groupId != groupId) throw IllegalStateException("Category doesn't belong to this group")
    }

    /**
     * Проверяет, что категория (INCOME/EXPENSE).
     */
    private fun validateCategoryType(categoryEntity: CategoryEntity, categoryType: CategoryType) {
        if (categoryEntity.type != categoryType) throw IllegalStateException("Category is not $categoryType type")
    }

    /**
     * Проверка доступа пользователя к группе, существование категории,
     * принадлежность категории группе, соответствие типа категории
     */
    private fun checkCategory(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId, categoryType: CategoryType? = null) {

        checkAccessToTheGroup(groupId, telegramUserId)

        val category = getCategoryOrThrow(categoryId)

        validateCategoryBelongsToGroup(groupId, category)

        if (categoryType != null) validateCategoryType(category, categoryType)
    }
}
