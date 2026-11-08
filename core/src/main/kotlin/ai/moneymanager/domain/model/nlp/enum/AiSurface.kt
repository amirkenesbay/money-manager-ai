package ai.moneymanager.domain.model.nlp.enum

enum class AiSurface {
    BOT,
    MINI_APP;

    fun offers(function: GeminiFunction): Boolean = this == BOT || function.inMiniApp
}
