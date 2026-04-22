package ai.moneymanager.domain.model.nlp

import ai.moneymanager.chat.reply.common.formatAmount
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate

sealed class AiPendingAction {
    abstract val confirmDescription: String

    sealed class CategoryAction : AiPendingAction() {
        data class Create(
            val name: String,
            val type: CategoryType,
            val icon: String?
        ) : CategoryAction() {
            override val confirmDescription: String
                get() {
                    val iconPart = if (icon != null) "$icon " else ""
                    return "Создать категорию $iconPart«$name» (${typeLabel(type)})"
                }
        }

        data class Delete(
            val category: Category
        ) : CategoryAction() {
            override val confirmDescription: String
                get() {
                    val iconPart = if (category.icon != null) "${category.icon} " else ""
                    return "Удалить категорию $iconPart«${category.name}» (${typeLabel(category.type)})"
                }
        }

        data class Rename(
            val category: Category,
            val newName: String
        ) : CategoryAction() {
            override val confirmDescription: String
                get() = "Переименовать «${category.name}» → «$newName» (${typeLabel(category.type)})"
        }

        data class ChangeIcon(
            val category: Category,
            val newIcon: String
        ) : CategoryAction() {
            override val confirmDescription: String
                get() {
                    val oldIcon = category.icon ?: "—"
                    return "Изменить иконку «${category.name}»: $oldIcon → $newIcon"
                }
        }

        data class DeleteAll(
            val groupId: ObjectId,
            val count: Int
        ) : CategoryAction() {
            override val confirmDescription: String
                get() = "Удалить ВСЕ категории группы ($count шт.)"
        }
    }

    sealed class TransactionAction : AiPendingAction() {
        data class Add(
            val groupId: ObjectId,
            val creatorId: Long,
            val type: CategoryType,
            val amount: Double,
            val category: Category,
            val description: String?,
            val operationDate: LocalDate
        ) : TransactionAction() {
            override val confirmDescription: String
                get() {
                    val typeWord = if (type == CategoryType.EXPENSE) "расход" else "доход"
                    val iconPart = category.icon?.let { "$it " } ?: ""
                    val descPart = description?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                    return "Добавить $typeWord: $iconPart«${category.name}» — ${formatAmount(BigDecimal.valueOf(amount))}$descPart"
                }
        }
    }
}

private fun typeLabel(type: CategoryType): String = when (type) {
    CategoryType.EXPENSE -> "Расход"
    CategoryType.INCOME -> "Доход"
}
