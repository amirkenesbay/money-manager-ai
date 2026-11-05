package ai.moneymanager.domain.model.question

enum class AskTopic {
    SUM,
    COMPARE,
    WHERE,
    BUDGETS,
    RECURRING,
}

data class AskTopicSuggestions(
    val topic: AskTopic,
    val title: String,
    val questions: List<String>,
)

data class AskSuggestions(
    val questions: List<String>,
    val topics: List<AskTopicSuggestions>,
)
