package ai.moneymanager.web.input

import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.badRequest

const val MAX_ICON_LENGTH = 16

private const val NAME_REQUIRED_KEY = "${API_ERROR_KEY_PREFIX}name_required"
private const val NAME_TOO_LONG_KEY = "${API_ERROR_KEY_PREFIX}name_too_long"
private const val ICON_TOO_LONG_KEY = "${API_ERROR_KEY_PREFIX}icon_too_long"

fun requireName(raw: String?, maxLength: Int): String {
    val name = raw?.trim().orEmpty()
    if (name.isEmpty()) throw badRequest(NAME_REQUIRED_KEY)
    if (name.length > maxLength) throw badRequest(NAME_TOO_LONG_KEY, maxLength)
    return name
}

fun normalizeIcon(raw: String?): String? {
    val icon = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (icon.length > MAX_ICON_LENGTH) throw badRequest(ICON_TOO_LONG_KEY)
    return icon
}
