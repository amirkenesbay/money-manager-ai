package ai.moneymanager.service

import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.BudgetThreshold
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.repository.BudgetAlertRepository
import ai.moneymanager.repository.entity.BudgetAlertEntity
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

data class BudgetAlert(val budget: Budget, val threshold: BudgetThreshold)

@Service
class BudgetAlertService(
    private val budgetService: BudgetService,
    private val budgetAlertRepository: BudgetAlertRepository,
) {
    fun claimNewAlerts(group: MoneyGroup): List<BudgetAlert> =
        budgetService.budgets(group).mapNotNull { budget ->
            val claimed = budget.reachedThresholds.filter { claim(budget, it) }
            claimed.maxByOrNull { it.percent }?.let { BudgetAlert(budget, it) }
        }

    private fun claim(budget: Budget, threshold: BudgetThreshold): Boolean = try {
        budgetAlertRepository.insert(
            BudgetAlertEntity(
                groupId = budget.category.groupId,
                categoryId = checkNotNull(budget.category.id),
                periodStart = budget.period.start,
                threshold = threshold,
            )
        )
        true
    } catch (exception: DuplicateKeyException) {
        false
    }
}
