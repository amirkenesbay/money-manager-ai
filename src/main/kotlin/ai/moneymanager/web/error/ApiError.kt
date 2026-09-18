package ai.moneymanager.web.error

/**
 * Единый формат ошибки REST API (`docs/webapp/03-API.md`).
 *
 * [details] несёт машиночитаемый контекст ошибки — например, упёртый лимит тарифа.
 */
data class ApiError(
    val code: ApiErrorCode,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
)
