package ai.moneymanager.chat.transition.ai.handler

import ai.moneymanager.chat.reply.common.formatBalanceBreakdown
import ai.moneymanager.chat.transition.ai.parseIsoDateOrNull
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.MoneyManagerContext
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.FinanceHistoryService
import ai.moneymanager.service.FinanceOperationService
import ai.moneymanager.service.FinanceReportService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.stereotype.Component

private val VALID_MONTH_RANGE = 1..12
private val VALID_YEAR_RANGE = 2000..2100

@Component
class FinanceInfoAiHandler(
    private val groupService: GroupService,
    private val financeOperationService: FinanceOperationService,
    private val financeReportService: FinanceReportService,
    private val financeHistoryService: FinanceHistoryService,
    private val localizationService: LocalizationService
) : AiDomainHandler {

    override fun canHandle(command: BotCommand): Boolean = when (command) {
        is BotCommand.ShowBalance,
        is BotCommand.ShowReport,
        is BotCommand.ShowHistory -> true
        else -> false
    }

    override fun canExecute(action: AiPendingAction): Boolean = false

    override fun prepareAction(
        command: BotCommand,
        context: MoneyManagerContext
    ): AiPreparationResult {
        val lang = context.userInfo?.language
        val groupId = context.userInfo?.activeGroupId
            ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.error.no_active_group", lang))

        val group = groupService.getGroup(groupId)
        val currency = group?.currency ?: Currency.DEFAULT
        val message = when (command) {
            is BotCommand.ShowBalance -> renderBalance(groupId, group?.initialBalance ?: BigDecimal.ZERO, currency, lang)
            is BotCommand.ShowReport -> renderReport(command, groupId, currency, lang)
            is BotCommand.ShowHistory -> renderHistory(command, groupId, currency, lang)
            else -> localizationService.t("ai.error.unknown_command", lang)
        }
        return AiPreparationResult.ImmediateResult(message)
    }

    override fun execute(action: AiPendingAction, context: MoneyManagerContext): String =
        localizationService.t("ai.error.unknown_command", context.userInfo?.language)

    private fun renderBalance(groupId: ObjectId, initialBalance: BigDecimal, currency: Currency, lang: String?): String {
        val breakdown = financeOperationService.calculateBalance(groupId, initialBalance)
        return formatBalanceBreakdown(breakdown, currency, localizationService, lang)
    }

    private fun renderReport(command: BotCommand.ShowReport, groupId: ObjectId, currency: Currency, lang: String?): String {
        val now = LocalDate.now()
        val month = command.month?.takeIf { it in VALID_MONTH_RANGE } ?: now.monthValue
        val year = command.year?.takeIf { it in VALID_YEAR_RANGE } ?: now.year
        return financeReportService.generateAnalyticsReport(groupId, LocalDate.of(year, month, 1), currency, lang)
    }

    private fun renderHistory(command: BotCommand.ShowHistory, groupId: ObjectId, currency: Currency, lang: String?): String {
        val now = LocalDate.now()
        val start = parseIsoDateOrNull(command.startDate) ?: now.withDayOfMonth(1)
        val end = parseIsoDateOrNull(command.endDate) ?: now
        val (from, to) = if (start <= end) start to end else end to start
        return financeHistoryService.generateReport(
            groupId, from, to, currency, lang,
            typeFilter = parseCategoryType(command.type),
            categoryFilter = command.categoryFilter
        )
    }

    private fun parseCategoryType(raw: String?): CategoryType? =
        raw?.let { runCatching { CategoryType.valueOf(it.trim().uppercase()) }.getOrNull() }
}
