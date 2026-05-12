package ai.moneymanager.chat.transition.ai

import gcardone.junidecode.Junidecode

fun matchesEntityName(entityName: String, searchName: String): Boolean {
    val normalizedEntity = stripLeadingNonLetters(entityName).lowercase()
    val normalizedSearch = stripLeadingNonLetters(searchName).lowercase()

    if (normalizedEntity.isEmpty() || normalizedSearch.isEmpty()) return false
    if (normalizedEntity == normalizedSearch) return true

    val translitEntity = Junidecode.unidecode(normalizedEntity).lowercase()
    val translitSearch = Junidecode.unidecode(normalizedSearch).lowercase()

    return translitEntity == translitSearch
}

internal fun stripLeadingNonLetters(value: String): String {
    val trimmed = value.trim()
    return trimmed.substring(indexOfFirstLetterOrDigit(trimmed)).trimStart()
}

internal fun extractLeadingNonLetters(value: String): String? {
    val trimmed = value.trim()
    val cut = indexOfFirstLetterOrDigit(trimmed)
    if (cut == 0) return null
    return trimmed.substring(0, cut).trim().takeIf { it.isNotEmpty() }
}

private fun indexOfFirstLetterOrDigit(value: String): Int {
    var index = 0
    while (index < value.length) {
        val codePoint = value.codePointAt(index)
        if (Character.isLetterOrDigit(codePoint)) return index
        index += Character.charCount(codePoint)
    }
    return value.length
}
