package ai.moneymanager.service

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.financeOperation.OperationType
import ai.moneymanager.dto.CreateFinanceOperationRequestDto
import ai.moneymanager.dto.HistoryFinanceOperationDto
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
    fun create(createDto: CreateFinanceOperationRequestDto): FinanceOperationEntity {

        log.info("Create finance operation: $createDto")

        val telegramUser = userInfoRepository.findUserInfoEntityByTelegramUserId(createDto.telegramUserId)
            ?: throw IllegalArgumentException("User not found")

        val groupId = telegramUser.activeGroupId
            ?: telegramUser.groupIds.firstOrNull()
            ?: throw IllegalArgumentException("User has no groups")

        val group = moneyGroupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("Group not found") }

        if (!group.memberTelegramUserIds.contains(createDto.telegramUserId)) {
            throw IllegalArgumentException("User doesn't have member TelegramUserId")
        }

        if (createDto.categoryId != null) {
            val category = categoryRepository.findById(createDto.categoryId)
                .orElseThrow { IllegalArgumentException("Category ID ${createDto.categoryId} not found") }

            if (category.groupId != groupId) {
                throw IllegalArgumentException("Category doesn't belong to this group")
            }

            val requiredCategoryType = when (createDto.operationType) {
                OperationType.EXPENSE -> CategoryType.EXPENSE
                OperationType.INCOME -> CategoryType.INCOME
            }

            if (category.type != requiredCategoryType) {
                throw IllegalArgumentException("Incorrect category type")
            }
        }

        if (createDto.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Amount must be greater than zero")
        }

        val entity = FinanceOperationEntity(
            id = null,
            telegramUserId = createDto.telegramUserId,
            groupId = group.id,
            categoryId = createDto.categoryId,
            day = createDto.day,
            amount = createDto.amount,
            operationType = createDto.operationType,
            currency = createDto.currency,
            description = createDto.description?.trim()?.takeIf { it.isNotBlank() }
        )

        return financeOperationRepository.save(entity)
    }

    /**
     * Получить полную сумму всей группы
     */
    fun getBalanceGroup(groupId: ObjectId): BigDecimal {
        log.info("Get balance for group: {}", groupId)
        return financeOperationRepository.getBalanceByGroupId(groupId)
            ?: throw IllegalArgumentException("Group not found")
    }

    /**
     * Получить историю финансовых операций в конкретный период
     */
    fun getHistoryFinanceOperationByPeriod(
        groupId: ObjectId,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<HistoryFinanceOperationDto> {
        log.info("Get history finance operation for group={} by period, from={}, to={}", groupId, fromDate, toDate)
        return financeOperationRepository.getHistoryFinanceOperationByPeriod(groupId, fromDate, toDate)
    }

    /**
     * Получить всю историю финансовых операций
     */
    fun getAllHistoryFinanceOperation(groupId: ObjectId): List<HistoryFinanceOperationDto> {
        log.info("Get all history finance operation for group: {}", groupId)
        return financeOperationRepository.getAllHistoryFinanceOperation(groupId)
    }
}
