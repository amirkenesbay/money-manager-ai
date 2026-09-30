package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.BalanceBreakdown
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.UserInfoService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class FinanceResponseFactory(
    private val userInfoService: UserInfoService,
) {
    fun balance(group: MoneyGroup, breakdown: BalanceBreakdown) = BalanceResponse(
        currency = group.currency,
        initial = breakdown.initial,
        income = breakdown.income,
        expense = breakdown.expense,
        total = breakdown.total,
    )

    fun operationsPage(page: Page<FinanceOperationEntity>): OperationsPageResponse {
        val creatorNames = userInfoService.displayNames(page.content.map { it.creatorId }.toSet())
        return OperationsPageResponse(
            items = page.content.map { it.toResponse(creatorNames.getValue(it.creatorId)) },
            page = page.number,
            size = page.size,
            totalItems = page.totalElements,
        )
    }

    private fun FinanceOperationEntity.toResponse(creatorName: String) = OperationResponse(
        id = checkNotNull(id),
        type = type,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryIcon = categoryIcon ?: DEFAULT_CATEGORY_ICON,
        operationDate = operationDate,
        description = description,
        creatorId = creatorId,
        creatorName = creatorName,
    )
}
