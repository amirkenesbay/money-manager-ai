package ai.moneymanager.service.question

import ai.moneymanager.domain.model.question.FinanceQuestion
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val FOLLOW_UP_WINDOW: Duration = Duration.ofMinutes(5)

private data class RememberedQuestion(val question: FinanceQuestion, val askedAt: Instant)

@Service
class QuestionMemory(
    private val clock: Clock,
) {
    private val questions = ConcurrentHashMap<Long, RememberedQuestion>()

    fun remember(userId: Long, question: FinanceQuestion) {
        questions[userId] = RememberedQuestion(question, clock.instant())
    }

    fun recall(userId: Long): FinanceQuestion? {
        val remembered = questions[userId] ?: return null
        if (remembered.askedAt.plus(FOLLOW_UP_WINDOW).isAfter(clock.instant())) return remembered.question
        questions.remove(userId, remembered)
        return null
    }
}
