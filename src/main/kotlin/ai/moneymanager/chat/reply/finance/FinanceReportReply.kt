package ai.moneymanager.chat.reply.finance

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.MoneyManagerButtonType
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.MoneyManagerState
import ai.moneymanager.service.LocalizationService
import kz.rmr.chatmachinist.api.reply.RepliesBuilder

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportMenuReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_REPORT_MENU

        message {
            val lang = context.userInfo?.language
            val title = localizationService.t("finance.report.menu.title", lang)
            val subtitle = localizationService.t("finance.report.menu.subtitle", lang)

            text = """
                |$title
                |
                |$subtitle
            """.trimMargin()

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.report.button.comparison", lang)
                        type = MoneyManagerButtonType.REPORT_COMPARISON
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("finance.report.button.analytics", lang)
                        type = MoneyManagerButtonType.REPORT_ANALYTICS
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("finance.report.button.members", lang)
                        type = MoneyManagerButtonType.REPORT_BY_MEMBERS
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("finance.report.button.category", lang)
                        type = MoneyManagerButtonType.REPORT_BY_CATEGORY
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_FINANCE
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportComparisonReply(
    localizationService: LocalizationService
) {
    reportWithNavigationReply(MoneyManagerState.FINANCE_REPORT_COMPARISON, localizationService)
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportAnalyticsReply(
    localizationService: LocalizationService
) {
    reportWithNavigationReply(MoneyManagerState.FINANCE_REPORT_ANALYTICS, localizationService)
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportMembersReply(
    localizationService: LocalizationService
) {
    reportWithNavigationReply(MoneyManagerState.FINANCE_REPORT_MEMBERS, localizationService)
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportCategorySelectReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_REPORT_CATEGORY_SELECT

        message {
            val lang = context.userInfo?.language
            val categories = context.categories
            val title = localizationService.t("finance.report.category.title", lang)
            val subtitle = localizationService.t("finance.report.category.subtitle", lang)

            text = """
                |$title
                |
                |$subtitle
            """.trimMargin()

            keyboard {
                categories.forEach { category ->
                    val icon = category.icon ?: DEFAULT_CATEGORY_ICON
                    buttonRow {
                        button {
                            text = "$icon ${category.name}"
                            type = MoneyManagerButtonType.REPORT_CATEGORY_ITEM
                        }
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_REPORT_MENU
                    }
                }
            }
        }
    }
}

fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.financeReportCategoryViewReply(
    localizationService: LocalizationService
) {
    reply {
        state = MoneyManagerState.FINANCE_REPORT_CATEGORY_VIEW

        message {
            val lang = context.userInfo?.language
            text = context.reportText ?: localizationService.t("finance.report.loading", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_REPORT_MENU
                    }
                }
            }
        }
    }
}

private fun RepliesBuilder<MoneyManagerState, MoneyManagerContext>.reportWithNavigationReply(
    reportState: MoneyManagerState,
    localizationService: LocalizationService
) {
    reply {
        state = reportState

        message {
            val lang = context.userInfo?.language
            text = context.reportText ?: localizationService.t("finance.report.loading", lang)

            keyboard {
                buttonRow {
                    button {
                        text = localizationService.t("finance.report.button.prev", lang)
                        type = MoneyManagerButtonType.REPORT_PREV
                    }
                    button {
                        text = localizationService.t("finance.report.button.next", lang)
                        type = MoneyManagerButtonType.REPORT_NEXT
                    }
                }
                buttonRow {
                    button {
                        text = localizationService.t("common.back", lang)
                        type = MoneyManagerButtonType.BACK_TO_REPORT_MENU
                    }
                }
            }
        }
    }
}
