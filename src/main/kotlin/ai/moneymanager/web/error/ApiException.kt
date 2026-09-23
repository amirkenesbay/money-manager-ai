package ai.moneymanager.web.error

class ApiException(
    val code: ApiErrorCode,
    val messageKey: String = code.messageKey,
    val messageArgs: List<Any> = emptyList(),
    val details: Map<String, Any> = emptyMap(),
) : RuntimeException(messageKey)
