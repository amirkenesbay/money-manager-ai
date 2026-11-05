package ai.moneymanager.service.question

import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.question.AnswerCard
import ai.moneymanager.domain.model.question.FinanceQuestion
import ai.moneymanager.service.SubscriptionLimitsService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AskService(
    private val financeAnswerService: FinanceAnswerService,
    private val subscriptionLimitsService: SubscriptionLimitsService,
    private val questionMemory: QuestionMemory,
) {
    fun answer(user: UserInfo, group: MoneyGroup, question: FinanceQuestion, today: LocalDate): AnswerCard {
        questionMemory.remember(checkNotNull(user.telegramUserId), question)
        val earliest = subscriptionLimitsService.earliestHistoryDate(user.hasActivePaidSubscription())
        return financeAnswerService.answer(question, group, user.language, today, earliest)
    }

    fun previousQuestion(user: UserInfo): FinanceQuestion? = user.telegramUserId?.let(questionMemory::recall)
}
