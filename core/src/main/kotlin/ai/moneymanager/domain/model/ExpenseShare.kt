package ai.moneymanager.domain.model

data class ExpenseShare(
    val payerId: Long,
    val participantIds: Set<Long>,
) {
    init {
        require(participantIds.isNotEmpty()) { "An expense is shared by at least one member" }
    }

    fun fitsMembers(memberIds: Set<Long>): Boolean = payerId in memberIds && memberIds.containsAll(participantIds)
}
