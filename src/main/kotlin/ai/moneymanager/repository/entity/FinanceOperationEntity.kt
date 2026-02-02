package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.DayType
import ai.moneymanager.domain.model.financeOperation.OperationType
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal

@Document(collection = "finance_operation")
data class FinanceOperationEntity(
    @Id
    val id: ObjectId ? = null,
    val telegramUserId: ObjectId,
    val groupId: ObjectId? = null,
    val day: DayType,
    val amount: BigDecimal,
    val operationType: OperationType,
    val currency: CurrencyType,
    val description: String? = null,
    val auditInfo: AuditInfo = AuditInfo()
)