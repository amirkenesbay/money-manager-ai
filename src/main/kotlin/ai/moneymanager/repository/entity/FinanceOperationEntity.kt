package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.OperationType
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDate

@Document(collection = "finance_operation")
@CompoundIndex(name = "id_group_date", def = "{'groupId': 1, 'operationDate': 1}")
@CompoundIndex(name = "id_group_user_date", def = "{'groupId': 1, 'telegramUserId': 1, 'operationDate': 1}")
@CompoundIndex(name = "id_group_category_date", def = "{'groupId': 1, 'categoryId': 1, 'operationDate': 1}")
data class FinanceOperationEntity(

    @Id val id: ObjectId? = null,

    val telegramUserId: Long,
    val groupId: ObjectId,
    val categoryId: ObjectId? = null,

    val operationDate: LocalDate,
    val amount: BigDecimal,
    val operationType: OperationType,
    val currency: CurrencyType,
    val description: String? = null,
    val auditInfo: AuditInfo = AuditInfo()
)