package ai.moneymanager.domain.model

sealed class GroupCreationResult {
    data class Created(val group: MoneyGroup) : GroupCreationResult()
    object Duplicate : GroupCreationResult()
    data class LimitReached(val limit: Int) : GroupCreationResult()
}
