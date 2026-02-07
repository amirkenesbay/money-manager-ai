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
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FinanceOperationService(
    private val userInfoRepository: UserInfoRepository,
    private val moneyGroupRepository: MoneyGroupRepository,
    private val categoryRepository: CategoryRepository,
    private val financeOperationRepository: FinanceOperationRepository
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun create(createDto: CreateFinanceOperationRequestDto): FinanceOperationEntity {

        log.info("Create finance operation: $createDto")

        val telegramUser = userInfoRepository.findUserInfoEntityByTelegramUserId(createDto.telegramUserId)
            ?: throw IllegalArgumentException("User not found")

        val groupId = telegramUser.groupIds.firstOrNull()
            ?: throw IllegalArgumentException("User has no groups")

        val group = moneyGroupRepository.findById(groupId)
            .orElseThrow { IllegalArgumentException("Group not found") }

        if (!group.memberTelegramUserIds.contains(createDto.telegramUserId)) {
            throw IllegalArgumentException("User doesn't have member TelegramUserId")
        }

        if (createDto.categoryId != null) {
            val category = categoryRepository.findById(createDto.categoryId)
                .orElseThrow { IllegalArgumentException("Category ID $createDto.categoryId not found") }

            if (category.groupId != group) {
                throw IllegalArgumentException("Cannot find a group with this ID")
            }

            val requiredCategoryType = when (createDto.operationType) {
                OperationType.EXPENSE -> CategoryType.EXPENSE
                OperationType.INCOME -> CategoryType.INCOME
            }

            if (category.type != requiredCategoryType) {
                throw IllegalArgumentException("Incorrect category type")
            }
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

    fun getBalanceGroup(groupId: ObjectId) {
        log.info("Get balance for group: $groupId")
        financeOperationRepository.getBalanceByGroupId(groupId) ?: throw IllegalArgumentException("Group not found")
    }

    fun getHistoryFinanceOperationByPeriod(groupId: ObjectId, fromDate: LocalDateTime, toDate: LocalDateTime): List<HistoryFinanceOperationDto> {
        log.info("Get history finance operation for group={} by period, from={}, to{}: $groupId, $fromDate, $toDate")
        return financeOperationRepository.getHistoryFinanceOperationByPeriod(groupId,fromDate,toDate)
    }

    fun getAllHistoryFinanceOperation(groupId: ObjectId): List<HistoryFinanceOperationDto> {
        log.info("Get all history finance operation for group: $groupId")
        return financeOperationRepository.getAllHistoryFinanceOperation(groupId)
    }
}

/*package ai.moneymanager.service

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
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FinanceOperationService(
    private val userInfoRepository: UserInfoRepository,
    private val moneyGroupRepository: MoneyGroupRepository,
    private val categoryRepository: CategoryRepository,
    private val financeOperationRepository: FinanceOperationRepository
) {

    private val log = LoggerFactory.getLogger(FinanceOperationService::class.java)

    @Transactional
    fun create(createDto: CreateFinanceOperationRequestDto): FinanceOperationEntity {
        log.info("FinanceOperationService.create started: {}", createDto)

        val telegramUser = userInfoRepository.findUserInfoEntityByTelegramUserId(createDto.telegramUserId)
            ?: run {
                log.warn("FinanceOperationService.create failed: user not found, telegramUserId={}", createDto.telegramUserId)
                throw IllegalArgumentException("User not found")
            }

        val groupId = telegramUser.groupIds.firstOrNull()
            ?: run {
                log.warn("FinanceOperationService.create failed: user has no groups, telegramUserId={}", createDto.telegramUserId)
                throw IllegalArgumentException("User has no groups")
            }

        val group = moneyGroupRepository.findById(groupId).orElseThrow {
            log.warn(
                "FinanceOperationService.create failed: group not found, telegramUserId={}, groupId={}",
                createDto.telegramUserId,
                groupId
            )
            IllegalArgumentException("Group not found")
        }

        if (!group.memberTelegramUserIds.contains(createDto.telegramUserId)) {
            log.warn(
                "FinanceOperationService.create failed: user is not a member of group, telegramUserId={}, groupId={}",
                createDto.telegramUserId,
                groupId
            )
            throw IllegalArgumentException("User doesn't have member TelegramUserId")
        }

        if (createDto.categoryId != null) {
            val category = categoryRepository.findById(createDto.categoryId).orElseThrow {
                log.warn(
                    "FinanceOperationService.create failed: category not found, categoryId={}, telegramUserId={}, groupId={}",
                    createDto.categoryId,
                    createDto.telegramUserId,
                    groupId
                )
                IllegalArgumentException("Category ID ${createDto.categoryId} not found")
            }

            if (category.groupId != groupId) {
                log.warn(
                    "FinanceOperationService.create failed: category does not belong to group, categoryId={}, categoryGroupId={}, requestedGroupId={}, telegramUserId={}",
                    createDto.categoryId,
                    category.groupId,
                    groupId,
                    createDto.telegramUserId
                )
                throw IllegalArgumentException("Category doesn't belong to this group")
            }

            val requiredCategoryType = when (createDto.operationType) {
                OperationType.EXPENSE -> CategoryType.EXPENSE
                OperationType.INCOME -> CategoryType.INCOME
            }

            if (category.type != requiredCategoryType) {
                log.warn(
                    "FinanceOperationService.create failed: category type mismatch, categoryId={}, categoryType={}, requiredType={}, operationType={}, telegramUserId={}, groupId={}",
                    createDto.categoryId,
                    category.type,
                    requiredCategoryType,
                    createDto.operationType,
                    createDto.telegramUserId,
                    groupId
                )
                throw IllegalArgumentException("Incorrect category type")
            }
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

        val saved = financeOperationRepository.save(entity)

        log.info(
            "FinanceOperationService.create success: operationId={}, telegramUserId={}, groupId={}, categoryId={}, operationType={}, amount={}, currency={}, day={}",
            saved.id,
            saved.telegramUserId,
            saved.groupId,
            saved.categoryId,
            saved.operationType,
            saved.amount,
            saved.currency,
            saved.day
        )

        return saved
    }

    fun getBalanceGroup(groupId: ObjectId) : java.math.BigDecimal {
        log.info("FinanceOperationService.getBalanceGroup started: groupId={}", groupId)

        val balance = financeOperationRepository.getBalanceByGroupId(groupId)
            ?: run {
                log.warn("FinanceOperationService.getBalanceGroup: no operations found (balance is null), groupId={}", groupId)
                throw IllegalArgumentException("No operations found for group")
            }

        log.info("FinanceOperationService.getBalanceGroup success: groupId={}, balance={}", groupId, balance)
        return balance
    }

    fun getHistoryFinanceOperationByPeriod(
        groupId: ObjectId,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<HistoryFinanceOperationDto> {
        log.info(
            "FinanceOperationService.getHistoryFinanceOperationByPeriod started: groupId={}, from={}, to={}",
            groupId,
            fromDate,
            toDate
        )

        if (fromDate.isAfter(toDate)) {
            log.warn(
                "FinanceOperationService.getHistoryFinanceOperationByPeriod failed: fromDate > toDate, groupId={}, from={}, to={}",
                groupId,
                fromDate,
                toDate
            )
            throw IllegalArgumentException("fromDate must be <= toDate")
        }

        val result = financeOperationRepository.getHistoryFinanceOperationByPeriod(groupId, fromDate, toDate)

        log.info(
            "FinanceOperationService.getHistoryFinanceOperationByPeriod success: groupId={}, from={}, to={}, count={}",
            groupId,
            fromDate,
            toDate,
            result.size
        )

        return result
    }

    fun getAllHistoryFinanceOperation(groupId: ObjectId): List<HistoryFinanceOperationDto> {
        log.info("FinanceOperationService.getAllHistoryFinanceOperation started: groupId={}", groupId)

        val result = financeOperationRepository.getAllHistoryFinanceOperation(groupId)

        log.info(
            "FinanceOperationService.getAllHistoryFinanceOperation success: groupId={}, count={}",
            groupId,
            result.size
        )

        return result
    }
}
*/
