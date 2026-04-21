package ai.moneymanager.domain.model.nlp

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import org.bson.types.ObjectId

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
}

private fun typeLabel(type: CategoryType): String = when (type) {
    CategoryType.EXPENSE -> "Расход"
    CategoryType.INCOME -> "Доход"
}
