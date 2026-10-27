package ai.moneymanager.web.error

class ApiException(
    val code: ApiErrorCode,
    val messageKey: String = code.messageKey,
    val messageArgs: List<Any> = emptyList(),
    val details: Map<String, Any> = emptyMap(),
) : RuntimeException(messageKey)

private const val LIMIT_DETAIL = "limit"

fun limitReached(code: ApiErrorCode, limit: Int) = ApiException(
    code = code,
    messageArgs = listOf(limit),
    details = mapOf(LIMIT_DETAIL to limit),
)

fun badRequest(messageKey: String, vararg args: Any) = ApiException(ApiErrorCode.BAD_REQUEST, messageKey, args.toList())
