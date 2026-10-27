package ai.moneymanager.domain.model

private const val BOT_USERNAME = "moneyManagerAIbot"
private const val START_LINK_TEMPLATE = "https://t.me/%s?start=%s"

object StartParameters {
    const val OPEN_FINANCE = "finance"
    const val JOIN_PREFIX = "join_"
}

fun buildInviteLink(inviteToken: String): String =
    START_LINK_TEMPLATE.format(BOT_USERNAME, StartParameters.JOIN_PREFIX + inviteToken)
