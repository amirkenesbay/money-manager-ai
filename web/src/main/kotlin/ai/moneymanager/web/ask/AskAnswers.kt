package ai.moneymanager.web.ask

import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.question.AnswerCard
import ai.moneymanager.domain.model.question.AskSuggestions
import ai.moneymanager.service.question.AskService
import ai.moneymanager.service.question.AskSuggestionService
import ai.moneymanager.service.question.FinanceAnswerService
import ai.moneymanager.service.question.financeQuestionOf
import ai.moneymanager.web.time.UserCalendar
import org.springframework.stereotype.Component

@Component
class AskAnswers(
    private val askService: AskService,
    private val askSuggestionService: AskSuggestionService,
    private val financeAnswerService: FinanceAnswerService,
    private val userCalendar: UserCalendar,
) {
    fun answerOf(user: UserInfo, group: MoneyGroup, commands: List<BotCommand>): AnswerCard? {
        val today = userCalendar.today(user)
        return commands.firstNotNullOfOrNull { financeQuestionOf(it, today) }
            ?.let { askService.answer(user, group, it, today) }
    }

    fun notUnderstood(user: UserInfo, group: MoneyGroup): AnswerCard =
        financeAnswerService.notUnderstood(user.language, askSuggestionService.examples(suggestions(user, group)))

    fun suggestions(user: UserInfo, group: MoneyGroup): AskSuggestions =
        askSuggestionService.suggestions(group, user.language, userCalendar.today(user))
}
