package ai.moneymanager.domain.model

private const val BOT_USERNAME = "moneyManagerAIbot"
private const val START_LINK_TEMPLATE = "https://t.me/%s?start=%s"

object StartParameters {
    const val OPEN_FINANCE = "finance"
    const val JOIN_PREFIX = "join_"
    const val DIGEST_MUTE_PREFIX = "digest_off_"
    const val UNDO_OPERATION_PREFIX = "undo_"
    const val RECORD_RECURRING_PREFIX = "pay_"
    const val DEBT_PAID_PREFIX = "debt_"
    const val LOAN_REPAID_PREFIX = "loanpaid_"
    const val LOAN_LATER_PREFIX = "loanlater_"
}

fun buildInviteLink(inviteToken: String): String =
    START_LINK_TEMPLATE.format(BOT_USERNAME, StartParameters.JOIN_PREFIX + inviteToken)
