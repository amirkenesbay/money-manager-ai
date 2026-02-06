package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.FinanceOperation
import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.DayType
import ai.moneymanager.domain.model.financeOperation.OperationType
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal

@Document(collection = "finance_operation")
@CompoundIndex(name = "id_group_day", def = "{'groupId': 1, 'day': 1}")
@CompoundIndex(name = "id_group_user_day", def = "{'groupId': 1, 'telegramUserId': 1, 'day': 1}")
@CompoundIndex(name = "id_group_category_day", def = "{'groupId': 1, 'categoryId': 1, 'day': 1}")
data class FinanceOperationEntity(

    @Id val id: ObjectId? = null,

    @Indexed
    val telegramUserId: Long,

    @Indexed
    val groupId: ObjectId? = null,

    @Indexed
    val categoryId: ObjectId? = null,

    val day: DayType,
    val amount: BigDecimal,
    val operationType: OperationType,
    val currency: CurrencyType,
    val description: String? = null,
    val auditInfo: AuditInfo = AuditInfo()
) {
    companion object {
        fun from(financeOperation: FinanceOperation) = FinanceOperationEntity(
            id = financeOperation.id,
            telegramUserId = financeOperation.telegramUserId,
            groupId = financeOperation.groupId,
            categoryId = financeOperation.categoryId,
            day = financeOperation.day,
            amount = financeOperation.amount,
            operationType = financeOperation.operationType,
            currency = financeOperation.currency,
            description = financeOperation.description
        )
    }
}