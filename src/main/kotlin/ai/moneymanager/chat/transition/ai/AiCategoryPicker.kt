package ai.moneymanager.chat.transition.ai

import ai.moneymanager.chat.reply.common.DEFAULT_CATEGORY_ICON
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.nlp.AiPendingAction
import gcardone.junidecode.Junidecode

internal fun rankCategoriesByProposed(
    proposedName: String,
    candidates: List<Category>,
    limit: Int
): List<Category> {
    val target = Junidecode.unidecode(stripLeadingNonLetters(proposedName)).lowercase()
    return candidates
        .map { it to levenshtein(target, Junidecode.unidecode(it.name).lowercase()) }
        .sortedBy { (_, distance) -> distance }
        .take(limit)
        .map { (category, _) -> category }
}

internal fun findCategoryByButtonText(
    buttonText: String,
    candidates: List<Category>
): Category? {
    val normalized = buttonText.trim()
    return candidates.firstOrNull { category ->
        val icon = category.icon ?: DEFAULT_CATEGORY_ICON
        "$icon ${category.name}" == normalized
    }
}

internal fun AiPendingAction.TransactionAction.withCategory(
    category: Category
): AiPendingAction.TransactionAction.Add = when (this) {
    is AiPendingAction.TransactionAction.Add -> copy(category = category)
    is AiPendingAction.TransactionAction.AddWithNewCategory -> AiPendingAction.TransactionAction.Add(
        groupId = groupId,
        creatorId = creatorId,
        type = type,
        amount = amount,
        category = category,
        description = description,
        operationDate = operationDate
    )
}

private fun levenshtein(a: String, b: String): Int {
    val m = a.length
    val n = b.length
    if (m == 0) return n
    if (n == 0) return m
    val dp = IntArray(n + 1) { it }
    for (i in 1..m) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..n) {
            val tmp = dp[j]
            dp[j] = if (a[i - 1] == b[j - 1]) prev else minOf(prev, dp[j - 1], dp[j]) + 1
            prev = tmp
        }
    }
    return dp[n]
}
