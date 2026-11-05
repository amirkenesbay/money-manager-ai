package ai.moneymanager.chat.transition.ai.handler

import ai.moneymanager.chat.reply.common.resolveCurrency
import ai.moneymanager.chat.reply.loan.LoanFormatter
import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.domain.model.nlp.AiPendingAction
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.loan.LoanService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class LoanAiHandler(
    private val loanService: LoanService,
    private val groupService: GroupService,
    private val loanFormatter: LoanFormatter,
    private val localizationService: LocalizationService,
) : AiDomainHandler {
    override fun canHandle(command: BotCommand): Boolean = command is BotCommand.AddLoan || command is BotCommand.RepayLoan

    override fun canExecute(action: AiPendingAction): Boolean = false

    override fun prepareAction(command: BotCommand, context: MoneyManagerContext): AiPreparationResult {
        val user = checkNotNull(context.userInfo)
        val language = user.language
        val groupId = user.activeGroupId ?: return AiPreparationResult.ImmediateResult(localizationService.t("ai.error.no_active_group", language))
        val userId = checkNotNull(user.telegramUserId)
        val today = LocalDate.now(user.zone)
        val currency = resolveCurrency(groupService, groupId)
        val message = when (command) {
            is BotCommand.AddLoan -> loanFormatter.created(loanService.create(groupId, userId, command.draft(today)), currency, language)
            is BotCommand.RepayLoan ->
                loanService.repayPerson(groupId, userId, command.direction, command.person, command.amount?.let { BigDecimal.valueOf(it) }, today)
                    ?.let { loanFormatter.repaid(it, currency, language) }
                    ?: loanFormatter.notFound(command.person, language)
            else -> localizationService.t("ai.error.unknown_command", language)
        }
        return AiPreparationResult.ImmediateResult(message)
    }

    override fun execute(action: AiPendingAction, context: MoneyManagerContext): String =
        localizationService.t("ai.error.unknown_command", context.userInfo?.language)
}
