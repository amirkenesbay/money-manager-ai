package ai.moneymanager.web.budget

import ai.moneymanager.domain.model.Budget
import ai.moneymanager.domain.model.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.BudgetService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

data class BudgetResponse(
    val categoryId: ObjectId,
    val icon: String,
    val name: String,
    val limit: BigDecimal,
    val spent: BigDecimal,
    val remaining: BigDecimal,
    val percent: Int,
    val from: LocalDate,
    val to: LocalDate,
)

@RestController
@RequestMapping("$API_V1_PREFIX/groups/{groupId}/budgets")
class BudgetController(
    private val budgetService: BudgetService,
    private val groupAccessGuard: GroupAccessGuard,
) {
    @GetMapping
    fun budgets(user: UserInfo, @PathVariable groupId: ObjectId): List<BudgetResponse> {
        val group = groupAccessGuard.requireMember(user, groupId)
        return budgetService.budgets(group).sortedByDescending { it.percent }.map { it.toResponse() }
    }

    private fun Budget.toResponse() = BudgetResponse(
        categoryId = checkNotNull(category.id),
        icon = category.icon ?: DEFAULT_CATEGORY_ICON,
        name = category.name,
        limit = limit,
        spent = spent,
        remaining = remaining,
        percent = percent,
        from = period.start,
        to = period.end,
    )
}
