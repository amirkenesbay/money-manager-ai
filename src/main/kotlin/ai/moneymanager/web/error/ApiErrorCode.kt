package ai.moneymanager.web.error

/**
 * Machine-readable error codes returned by the REST API.
 *
 * The contract lives in `docs/webapp/03-API.md`; codes mirror the existing sealed result
 * types of the service layer so a client can branch on the cause, not on the HTTP status.
 */
enum class ApiErrorCode {
    UNAUTHORIZED,
}
