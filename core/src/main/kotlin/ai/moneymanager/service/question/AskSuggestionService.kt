package ai.moneymanager.service.question

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.domain.model.currentMonthPeriod
import ai.moneymanager.domain.model.question.AskSuggestions
import ai.moneymanager.domain.model.question.AskTopic
import ai.moneymanager.domain.model.question.AskTopicSuggestions
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.service.BudgetService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.RecurringPaymentService
import ai.moneymanager.service.localeFor
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

private const val TOP_CATEGORIES = 2
private const val QUESTIONS_PER_TOPIC = 3
private const val BUDGET_ALERT_PERCENT = 80
private const val NOT_UNDERSTOOD_EXAMPLES = 3

private data class SuggestionData(
    val topCategories: List<Category>,
    val alertBudget: Category?,
    val hasBudgets: Boolean,
    val hasRecurring: Boolean,
    val shared: Boolean,
)

@Service
class AskSuggestionService(
    private val financeOperationRepository: FinanceOperationRepository,
    private val categoryService: CategoryService,
    private val budgetService: BudgetService,
    private val recurringPaymentService: RecurringPaymentService,
    private val localizationService: LocalizationService,
) {
    fun suggestions(group: MoneyGroup, language: String?, today: LocalDate): AskSuggestions {
        val data = load(group, today)
        val topics = AskTopic.entries
            .mapNotNull { topic -> questions(topic, data, language).takeIf { it.isNotEmpty() }?.let { topic to it } }
            .map { (topic, questions) -> AskTopicSuggestions(topic, t("topic.${topic.name.lowercase()}", language), questions.take(QUESTIONS_PER_TOPIC)) }
        return AskSuggestions(topics.map { it.questions.first() }, topics)
    }

    fun examples(suggestions: AskSuggestions): List<String> = suggestions.questions.take(NOT_UNDERSTOOD_EXAMPLES)

    private fun questions(topic: AskTopic, data: SuggestionData, language: String?): List<String> {
        val names = data.topCategories.map { it.name.lowercase(localeFor(language)) }
        return when (topic) {
            AskTopic.SUM -> names.map { t("follow.category", language, it) } + t("suggest.last_month", language)
            AskTopic.COMPARE -> names.take(1).map { t("suggest.compare_category", language, it) } + t("suggest.compare_month", language)
            AskTopic.WHERE -> listOfNotNull(
                t("follow.where", language),
                t("suggest.members", language).takeIf { data.shared },
                t("follow.largest", language),
                t("follow.trends", language),
            )
            AskTopic.BUDGETS -> listOfNotNull(
                data.alertBudget?.let { t("suggest.budget_category", language, it.name.lowercase(localeFor(language))) },
                t("follow.balance", language),
                t("follow.budgets", language).takeIf { data.hasBudgets },
                t("follow.forecast", language),
            )
            AskTopic.RECURRING -> if (data.hasRecurring) listOf(t("suggest.recurring_total", language), t("suggest.recurring_soon", language)) else emptyList()
        }
    }

    private fun load(group: MoneyGroup, today: LocalDate): SuggestionData {
        val groupId = checkNotNull(group.id)
        val budgets = budgetService.budgets(group, currentMonthPeriod(today, group.monthStartDay))
        return SuggestionData(
            topCategories = topCategories(group, today),
            alertBudget = budgets.firstOrNull { it.effectiveLimit.signum() > 0 && it.spent.multiply(HUNDRED) >= it.effectiveLimit.multiply(BigDecimal(BUDGET_ALERT_PERCENT)) }?.category,
            hasBudgets = budgets.isNotEmpty(),
            hasRecurring = recurringPaymentService.payments(groupId).any { it.active },
            shared = group.memberIds.size > 1,
        )
    }

    private fun topCategories(group: MoneyGroup, today: LocalDate): List<Category> {
        val groupId = checkNotNull(group.id)
        val period = currentMonthPeriod(today, group.monthStartDay)
        val expenses = categoryService.getCategoriesByGroup(groupId).filter { it.type == CategoryType.EXPENSE }
        val spent = financeOperationRepository.findAll(OperationFilter(groupId, period.start, period.end, CategoryType.EXPENSE))
            .groupBy { it.categoryId }
            .mapValues { (_, operations) -> operations.sumOf { it.amount } }
        return expenses.sortedByDescending { spent[it.id] ?: BigDecimal.ZERO }.take(TOP_CATEGORIES)
    }

    private fun t(key: String, language: String?, vararg args: Any): String = localizationService.t("ask.$key", language, *args)
}
