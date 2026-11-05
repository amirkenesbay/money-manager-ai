package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class BalanceResponse(
    val currency: Currency,
    val initial: BigDecimal,
    val income: BigDecimal,
    val expense: BigDecimal,
    val total: BigDecimal,
)

data class OperationResponse(
    val id: ObjectId,
    val type: CategoryType,
    val amount: BigDecimal,
    val categoryId: ObjectId,
    val categoryName: String,
    val categoryIcon: String,
    val operationDate: LocalDate,
    val description: String?,
    val creatorId: Long,
    val creatorName: String,
    val payerId: Long? = null,
    val participantIds: Set<Long>? = null,
)

data class OperationsPageResponse(
    val items: List<OperationResponse>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totals: OperationTotalsResponse? = null,
)

data class OperationTotalsResponse(
    val income: BigDecimal,
    val expense: BigDecimal,
    val count: Int,
)

data class CategoryAmountResponse(
    val icon: String,
    val name: String,
    val total: BigDecimal,
)

data class MonthSummaryResponse(
    val month: YearMonth,
    val from: LocalDate,
    val to: LocalDate,
    val currency: Currency,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal,
    val operationCount: Int,
    val expenseByCategory: List<CategoryAmountResponse>,
    val dailyExpense: List<DailyAmountResponse>,
)

data class DailyAmountResponse(
    val date: LocalDate,
    val total: BigDecimal,
)
