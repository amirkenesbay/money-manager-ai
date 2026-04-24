package ai.moneymanager.chat.reply.category

import ai.moneymanager.chat.reply.common.CategoryTypeForm
import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.chat.reply.common.backButton
import ai.moneymanager.chat.reply.common.cancelButton
import ai.moneymanager.chat.reply.common.categoryTypeLabel
import ai.moneymanager.chat.reply.common.confirmAndCancelButtons
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryManagementReply() {
    reply {
        state = MoneyManagerState.CATEGORY_MANAGEMENT

        message {
            val groupName = context.activeGroupName
            val groupLine = if (groupName != null) "\n|Группа: $groupName" else ""

            text = """
                |📂 Управление категориями$groupLine
                |
                |Здесь вы можете создать новые категории или просмотреть существующие.
            """.trimMargin()

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
                backButton("⬅️ Назад в меню")
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
                |Для управления категориями нужна группа. Создайте группу для личного или совместного учёта.
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "➕ Создать группу"
                        type = MoneyManagerButtonType.CREATE_GROUP
                    }
                }
                backButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryCreateSelectTypeReply() {
    reply {
        state = MoneyManagerState.CATEGORY_CREATE_SELECT_TYPE

        message {
            text = """
                |➕ Создание новой категории
                |
                |Выберите тип категории:
            """.trimMargin()

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
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryCreateEnterNameReply() {
    reply {
        state = MoneyManagerState.CATEGORY_CREATE_ENTER_NAME

        message {
            val categoryType = context.categoryTypeInput
            val typeText = categoryTypeLabel(categoryType)

            if (context.customNameInputMode) {
                text = """
                    |➕ Создание категории $typeText
                    |
                    |✍️ Введите название категории:
                """.trimMargin()

                keyboard {
                    cancelButton()
                }
            } else {
                text = """
                    |➕ Создание категории $typeText
                    |
                    |Выберите готовый вариант или задайте своё название:
                """.trimMargin()

                keyboard {
                    when (categoryType) {
                        CategoryType.EXPENSE -> {
                            buttonRow {
                                button {
                                    text = "🍔 Еда вне дома"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_FOOD_OUT
                                }
                                button {
                                    text = "🏠 ЖКХ"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_UTILITIES
                                }
                            }
                            buttonRow {
                                button {
                                    text = "💊 Медицина"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_MEDICINE
                                }
                                button {
                                    text = "🎮 Развлечения"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_ENTERTAINMENT
                                }
                            }
                            buttonRow {
                                button {
                                    text = "👕 Одежда и обувь"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_CLOTHES
                                }
                                button {
                                    text = "🚕 Такси"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_TAXI
                                }
                            }
                        }
                        CategoryType.INCOME -> {
                            buttonRow {
                                button {
                                    text = "💰 Зарплата"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_SALARY
                                }
                                button {
                                    text = "💸 Премия"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_BONUS
                                }
                            }
                            buttonRow {
                                button {
                                    text = "🎁 Подарок"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_GIFT
                                }
                                button {
                                    text = "💼 Фриланс"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_FREELANCE
                                }
                            }
                            buttonRow {
                                button {
                                    text = "📈 Инвестиции"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_INVESTMENTS
                                }
                                button {
                                    text = "💵 Возврат долга"
                                    type = MoneyManagerButtonType.QUICK_CATEGORY_DEBT_RETURN
                                }
                            }
                        }
                        else -> {}
                    }
                    buttonRow {
                        button {
                            text = "✏️ Своё название"
                            type = MoneyManagerButtonType.ENTER_CUSTOM_NAME
                        }
                    }
                    cancelButton()
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryCreateResultReply() {
    reply {
        state = MoneyManagerState.CATEGORY_CREATE_RESULT

        message {
            newMessage = !context.isQuickCategoryCreation

            val category = context.currentCategory
            val categoryName = context.categoryNameInput ?: "категория"

            text = if (category != null) {
                val icon = category.icon ?: DEFAULT_CATEGORY_ICON
                val typeText = categoryTypeLabel(category.type)
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
                backButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryListSelectTypeReply() {
    reply {
        state = MoneyManagerState.CATEGORY_LIST_SELECT_TYPE

        message {
            text = """
                |📋 Мои категории
                |
                |Выберите тип категорий для просмотра:
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "📉 Расходы"
                        type = MoneyManagerButtonType.CATEGORY_TYPE_EXPENSE
                    }
                }
                buttonRow {
                    button {
                        text = "📈 Доходы"
                        type = MoneyManagerButtonType.CATEGORY_TYPE_INCOME
                    }
                }
                buttonRow {
                    button {
                        text = "🗑 Удалить все категории"
                        type = MoneyManagerButtonType.DELETE_ALL_CATEGORIES
                    }
                }
                cancelButton("⬅️ Назад")
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryListReply() {
    reply {
        state = MoneyManagerState.CATEGORY_LIST

        message {
            val categories = context.categories
            val categoryType = context.categoryTypeInput
            val typeText = categoryTypeLabel(categoryType, CategoryTypeForm.GENITIVE_PLURAL)
            val typeEmoji = when (categoryType) {
                CategoryType.EXPENSE -> "📉"
                CategoryType.INCOME -> "📈"
                else -> "📋"
            }

            text = if (categories.isEmpty()) {
                """
                    |$typeEmoji Категории $typeText
                    |
                    |У вас пока нет категорий $typeText.
                    |Создайте первую категорию!
                """.trimMargin()
            } else {
                """
                    |$typeEmoji Категории $typeText
                    |
                    |Выберите категорию:
                """.trimMargin()
            }

            keyboard {
                if (categories.isNotEmpty()) {
                    categories.forEach { category ->
                        buttonRow {
                            button {
                                val icon = category.icon ?: DEFAULT_CATEGORY_ICON
                                text = "$icon ${category.name}"
                                type = MoneyManagerButtonType.CATEGORY_ITEM
                            }
                        }
                    }
                }
                backButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryActionsReply() {
    reply {
        state = MoneyManagerState.CATEGORY_ACTIONS

        message {
            // show-once: read and reset
            if (context.textInputResponse) {
                newPinnedMessage = true
                context.textInputResponse = false
            }

            val category = context.currentCategory
            val icon = category?.icon ?: DEFAULT_CATEGORY_ICON
            val typeText = categoryTypeLabel(category?.type)

            // show-once: read and reset
            val confirmation = context.renameConfirmation?.let { "\n\n$it" } ?: ""
            context.renameConfirmation = null

            text = """
                |Категория $typeText: $icon ${category?.name}
                |
                |Выберите действие:$confirmation
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = "✏️ Изменить название"
                        type = MoneyManagerButtonType.EDIT_CATEGORY
                    }
                }
                buttonRow {
                    button {
                        text = "🎨 Изменить иконку"
                        type = MoneyManagerButtonType.EDIT_CATEGORY_ICON
                    }
                }
                buttonRow {
                    button {
                        text = "🗑 Удалить категорию"
                        type = MoneyManagerButtonType.DELETE_CATEGORY_BUTTON
                    }
                }
                backButton("⬅️ Назад к списку")
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryEditIconReply() {
    reply {
        state = MoneyManagerState.CATEGORY_EDIT_ICON

        message {
            val category = context.currentCategory
            val icon = category?.icon ?: DEFAULT_CATEGORY_ICON

            // show-once: read and reset
            val errorText = if (context.iconInputError) {
                "\n|\n|⚠️ Пожалуйста, отправьте эмодзи, а не текст"
            } else ""
            context.iconInputError = false

            text = """
                |🎨 Изменение иконки
                |
                |Текущая иконка: $icon ${category?.name}
                |
                |Отправьте новый эмодзи:$errorText
            """.trimMargin()

            keyboard {
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryEditNameReply() {
    reply {
        state = MoneyManagerState.CATEGORY_EDIT_NAME

        message {
            val category = context.currentCategory
            val icon = category?.icon ?: DEFAULT_CATEGORY_ICON

            text = """
                |✏️ Редактирование категории
                |
                |Текущее название: $icon ${category?.name}
                |
                |Введите новое название:
            """.trimMargin()

            keyboard {
                cancelButton()
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.categoryDeleteConfirmReply() {
    reply {
        state = MoneyManagerState.CATEGORY_DELETE_CONFIRM

        message {
            val category = context.currentCategory
            val icon = category?.icon ?: DEFAULT_CATEGORY_ICON

            text = """
                |🗑 Удаление категории
                |
                |Вы уверены, что хотите удалить категорию "$icon ${category?.name}"?
                |
                |⚠️ Это действие необратимо!
            """.trimMargin()

            keyboard {
                confirmAndCancelButtons()
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
                    confirmAndCancelButtons("✅ Да, удалить все")
                } else {
                    backButton()
                }
            }
        }
    }
}
