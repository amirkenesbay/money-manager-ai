package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.BudgetThreshold
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document(collection = "budget_alert")
@CompoundIndex(def = "{'groupId': 1, 'categoryId': 1, 'periodStart': 1, 'threshold': 1}", unique = true)
data class BudgetAlertEntity(
    val id: ObjectId? = null,
    val groupId: ObjectId,
    val categoryId: ObjectId,
    val periodStart: LocalDate,
    val threshold: BudgetThreshold,
)
