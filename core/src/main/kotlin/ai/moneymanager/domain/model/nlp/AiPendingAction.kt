package ai.moneymanager.domain.model.nlp

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.NotificationEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

sealed class AiPendingAction {
    sealed class CategoryAction : AiPendingAction() {
        data class Create(
            val name: String,
            val type: CategoryType,
            val icon: String?
        ) : CategoryAction()

        data class Delete(
            val category: Category
        ) : CategoryAction()

        data class Rename(
            val category: Category,
            val newName: String
        ) : CategoryAction()

        data class ChangeIcon(
            val category: Category,
            val newIcon: String
        ) : CategoryAction()

        data class DeleteAll(
            val groupId: ObjectId,
            val count: Int
        ) : CategoryAction()
    }

    sealed class GroupAction : AiPendingAction() {
        data class Create(
            val name: String
        ) : GroupAction()

        data class Delete(
            val group: MoneyGroup
        ) : GroupAction()
    }

    sealed class NotificationAction : AiPendingAction() {
        data class CreateDaily(
            val name: String,
            val hour: Int,
            val minute: Int
        ) : NotificationAction()

        data class Delete(
            val notification: NotificationEntity
        ) : NotificationAction()
    }

    sealed class RecentOperationAction : AiPendingAction() {
        data class Delete(
            val operation: FinanceOperationEntity
        ) : RecentOperationAction()

        data class Edit(
            val operation: FinanceOperationEntity,
            val newAmount: BigDecimal?,
            val newCategory: Category?,
            val newOperationDate: LocalDate?
        ) : RecentOperationAction()
    }

    sealed class TransactionAction : AiPendingAction() {
        abstract val groupId: ObjectId
        abstract val creatorId: Long
        abstract val type: CategoryType
        abstract val amount: Double
        abstract val description: String?
        abstract val operationDate: LocalDate
        abstract val categoryDisplayName: String
        abstract val categoryDisplayIcon: String?
        abstract val isNewCategory: Boolean

        data class Add(
            override val groupId: ObjectId,
            override val creatorId: Long,
            override val type: CategoryType,
            override val amount: Double,
            val category: Category,
            override val description: String?,
            override val operationDate: LocalDate
        ) : TransactionAction() {
            override val categoryDisplayName: String get() = category.name
            override val categoryDisplayIcon: String? get() = category.icon
            override val isNewCategory: Boolean get() = false
        }

        data class AddWithNewCategory(
            override val groupId: ObjectId,
            override val creatorId: Long,
            override val type: CategoryType,
            override val amount: Double,
            val suggestedCategoryName: String,
            val suggestedCategoryIcon: String,
            override val description: String?,
            override val operationDate: LocalDate
        ) : TransactionAction() {
            override val categoryDisplayName: String get() = suggestedCategoryName
            override val categoryDisplayIcon: String get() = suggestedCategoryIcon
            override val isNewCategory: Boolean get() = true
        }
    }
}
