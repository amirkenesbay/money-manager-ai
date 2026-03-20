package ai.moneymanager.chat.reply.category

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryManagementReply() {
    reply {
        state = MoneyManagerState.CATEGORY_MANAGEMENT

        message {
            text = """
                📂 Управление категориями

                Здесь вы можете создать новые категории или просмотреть существующие.
            """.trimIndent()

            keyboard {
                buttonRow {
                    button {
                        text = "➕ Создать категорию"
                        type = MoneyManagerButtonType.CREATE_CATEGORY
                    }
                }
                buttonRow {
                    button {
                        text = "📋 Мои категории"
                        type = MoneyManagerButtonType.MY_CATEGORIES
                    }
                }
                buttonRow {
                    button {
                        text = "🗑 Удалить все категории"
                        type = MoneyManagerButtonType.DELETE_ALL_CATEGORIES
                    }
                }
                buttonRow {
                    button {
                        text = "⬅️ Назад в меню"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryNoGroupWarningReply() {
    reply {
        state = MoneyManagerState.CATEGORY_NO_GROUP_WARNING

        message {
            text = """
                |⚠️ Нет активной группы
                |
                |Для создания категорий необходимо сначала создать группу.
                |Категории привязаны к группе и могут использоваться только в рамках этой группы.
                |
                |Создайте группу или выберите существующую для продолжения.
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "➕ Создать группу"
                        type = MoneyManagerButtonType.CREATE_GROUP
                    }
                }
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryCreateSelectTypeReply() {
    reply {
        state = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE

        message {
            text = """
                ➕ Создание новой категории

                Выберите тип категории:
            """.trimIndent()

            keyboard {
                buttonRow {
                    button {
                        text = "📉 Расход"
                        type = MoneyManagerButtonType.CATEGORY_TYPE_EXPENSE
                    }
                }
                buttonRow {
                    button {
                        text = "📈 Доход"
                        type = MoneyManagerButtonType.CATEGORY_TYPE_INCOME
                    }
                }
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryCreateEnterNameReply() {
    reply {
        state = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME

        message {
            val categoryType = context.categoryTypeInput
            val typeText = when (categoryType) {
                CategoryType.EXPENSE -> "расхода"
                CategoryType.INCOME -> "дохода"
                else -> "категории"
            }

            text = """
                ➕ Создание категории $typeText

                Введите название категории:
            """.trimIndent()

            keyboard {
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryCreateResultReply() {
    reply {
        state = MoneyManagerState.CATEGORY_CREATE_RESULT

        message {
            val category = context.currentCategory
            val categoryName = context.categoryNameInput ?: "категория"

            text = if (category != null) {
                val icon = category.icon ?: "📌"
                val typeText = if (category.type == CategoryType.EXPENSE) "расхода" else "дохода"
                """
                    |✅ Категория $typeText создана
                    |
                    |$icon ${category.name}
                """.trimMargin()
            } else {
                """
                    |❌ Не удалось создать категорию
                    |
                    |Категория «$categoryName» уже существует в этой группе.
                """.trimMargin()
            }

            keyboard {
                if (category != null) {
                    buttonRow {
                        button {
                            text = "➕ Создать ещё"
                            type = MoneyManagerButtonType.CREATE_CATEGORY
                        }
                    }
                }
                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryListReply() {
    reply {
        state = MoneyManagerState.CATEGORY_LIST

        message {
            val categories = context.categories

            text = if (categories.isEmpty()) {
                """
                    📋 Список категорий

                    У вас пока нет категорий.
                    Создайте первую категорию!
                """.trimIndent()
            } else {
                """
                    📋 Список категорий

                    Выберите категорию для редактирования:
                """.trimIndent()
            }

            keyboard {
                if (categories.isNotEmpty()) {
                    // Показываем категории как кнопки с номерами
                    categories.forEachIndexed { index, category ->
                        buttonRow {
                            button {
                                val icon = category.icon ?: "📌"
                                val typeEmoji = if (category.type == CategoryType.EXPENSE) "📉" else "📈"
                                text = "${index + 1}. $icon $typeEmoji ${category.name}"
                                type = MoneyManagerButtonType.CATEGORY_ITEM
                            }
                        }
                    }
                }

                buttonRow {
                    button {
                        text = "⬅️ Назад"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryActionsReply() {
    reply {
        state = MoneyManagerState.CATEGORY_ACTIONS

        message {
            val category = context.currentCategory
            val icon = category?.icon ?: "📌"
            val typeText = if (category?.type == CategoryType.EXPENSE) "расхода" else "дохода"

            text = """
                Категория $typeText: $icon ${category?.name}

                Выберите действие:
            """.trimIndent()

            keyboard {
                buttonRow {
                    button {
                        text = "✏️ Изменить название"
                        type = MoneyManagerButtonType.EDIT_CATEGORY
                    }
                }
                buttonRow {
                    button {
                        text = "🗑 Удалить категорию"
                        type = MoneyManagerButtonType.DELETE_CATEGORY_BUTTON
                    }
                }
                buttonRow {
                    button {
                        text = "⬅️ Назад к списку"
                        type = MoneyManagerButtonType.BACK_TO_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryEditNameReply() {
    reply {
        state = MoneyManagerState.CATEGORY_EDIT_NAME

        message {
            val category = context.currentCategory
            val icon = category?.icon ?: "📌"

            text = """
                ✏️ Редактирование категории

                Текущее название: $icon ${category?.name}

                Введите новое название:
            """.trimIndent()

            keyboard {
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryDeleteConfirmReply() {
    reply {
        state = MoneyManagerState.CATEGORY_DELETE_CONFIRM

        message {
            val category = context.currentCategory
            val icon = category?.icon ?: "📌"

            text = """
                🗑 Удаление категории

                Вы уверены, что хотите удалить категорию "$icon ${category?.name}"?

                ⚠️ Это действие необратимо!
            """.trimIndent()

            keyboard {
                buttonRow {
                    button {
                        text = "✅ Да, удалить"
                        type = MoneyManagerButtonType.CONFIRM_DELETE
                    }
                }
                buttonRow {
                    button {
                        text = "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryDeleteAllConfirmReply() {
    reply {
        state = MoneyManagerState.CATEGORY_DELETE_ALL_CONFIRM

        message {
            val categories = context.categories
            val categoryCount = categories.size

            text = if (categoryCount == 0) {
                """
                    |❌ Нет категорий для удаления
                    |
                    |В текущей группе нет категорий.
                """.trimMargin()
            } else {
                val expenseCount = categories.count { it.type == CategoryType.EXPENSE }
                val incomeCount = categories.count { it.type == CategoryType.INCOME }

                """
                    |⚠️ Удаление всех категорий
                    |
                    |Вы уверены, что хотите удалить ВСЕ категории группы?
                    |
                    |Всего категорий: $categoryCount
                    |├─ 📉 Расходы: $expenseCount
                    |└─ 📈 Доходы: $incomeCount
                    |
                    |⚠️ Это действие необратимо!
                    |Все категории будут безвозвратно удалены.
                """.trimMargin()
            }

            keyboard {
                if (categoryCount > 0) {
                    buttonRow {
                        button {
                            text = "✅ Да, удалить все"
                            type = MoneyManagerButtonType.CONFIRM_DELETE
                        }
                    }
                }
                buttonRow {
                    button {
                        text = if (categoryCount == 0) "⬅️ Назад" else "❌ Отмена"
                        type = MoneyManagerButtonType.CANCEL
                    }
                }
            }
        }
    }
}