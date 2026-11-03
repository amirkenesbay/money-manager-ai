package ai.moneymanager.domain.text

private val NON_LETTERS = Regex("[^\\p{L}\\s]+")
private val WHITESPACE = Regex("\\s+")
private const val YO = 'ё'
private const val YE = 'е'

fun merchantKey(description: String?): String? =
    description
        ?.lowercase()
        ?.replace(YO, YE)
        ?.replace(NON_LETTERS, " ")
        ?.replace(WHITESPACE, " ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
