package ai.moneymanager.service

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.financeOperation.OperationType
import ai.moneymanager.dto.CreateFinanceOperationRequestDto
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FinanceOperationService(
    private val userInfoRepository: UserInfoRepository,
    private val moneyGroupRepository: MoneyGroupRepository,
    private val categoryRepository: CategoryRepository,
    private val financeOperationRepository: FinanceOperationRepository
) {

    @Transactional
    fun create(createDto: CreateFinanceOperationRequestDto): FinanceOperationEntity {

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
}