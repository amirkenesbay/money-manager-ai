package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.financeOperation.CurrencyType
import ai.moneymanager.domain.model.financeOperation.FinanceCategory
import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.Instant

@Document(collection = "finance_operation")
data class FinanceOperationEntity(
    @Id
    val id: ObjectId,
    val userId: ObjectId,
    val amount: BigDecimal,
    val financeCategory: FinanceCategory,
    val currency: CurrencyType,
    val descriptionOfTheOperation: String? = null,
    @CreatedDate
    val createdAt: Instant,
    @LastModifiedDate
    val updatedAt: Instant,
)