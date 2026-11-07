package ai.moneymanager.chat.transition.admin

import ai.moneymanager.chat.state.MoneyManagerContext
import ai.moneymanager.chat.state.MoneyManagerState
import ai.moneymanager.chat.telegram.AdminService
import ai.moneymanager.domain.model.UserAccess
import ai.moneymanager.domain.model.formatUserHandle
import ai.moneymanager.service.UserInfoService
import kz.rmr.chatmachinist.api.transition.DialogBuilder
import kz.rmr.chatmachinist.model.ActionContext
import kz.rmr.chatmachinist.model.EventType

private const val ACTIVATE_COMMAND = "/activate"
private const val ACTIVATE_USAGE = "Использование: /activate <username или telegramId> <дней>"
private const val ACTIVATE_USER_NOT_FOUND = "Пользователь не найден: %s"
private const val ACTIVATE_SUCCESS = "✅ Подписка активирована для %s до %s"
private const val WHITELIST_COMMAND = "/whitelist"
private const val BLACKLIST_COMMAND = "/blacklist"
private const val ADD_ACTION = "add"
private const val REMOVE_ACTION = "remove"
private const val LIST_ACTION = "list"
private const val ACCESS_USAGE = "Использование: %1${'$'}s add|remove <username или telegramId>, %1${'$'}s list — список"
private const val ACCESS_EMPTY = "%s: пусто"
private const val ACCESS_ADDED = "✅ %s добавлен в %s"
private const val ACCESS_REMOVED = "✅ %s убран из %s"
private const val ACCESS_NOT_LISTED = "%s нет в %s"
private const val ACCESS_USER_NOT_FOUND = "Пользователь не найден: %s. Пусть сначала откроет бота."
private const val ADMIN_CANNOT_BE_BLOCKED = "Админа заблокировать нельзя"

private val ACCESS_LISTS = mapOf(WHITELIST_COMMAND to UserAccess.WHITELISTED, BLACKLIST_COMMAND to UserAccess.BLOCKED)
private val ADMIN_COMMANDS = listOf(ACTIVATE_COMMAND) + ACCESS_LISTS.keys

fun DialogBuilder<MoneyManagerState, MoneyManagerContext>.adminDialogTransitions(
    adminService: AdminService,
    userInfoService: UserInfoService
) {
    transition {
        name = "Activate subscription (admin)"

        listOf(MoneyManagerState.MENU, MoneyManagerState.MINI_APP_GATEWAY).forEach { state ->
            condition {
                from = state
                eventType = EventType.COMMAND
                guard { update.message?.text?.startsWith(ACTIVATE_COMMAND) == true && adminService.isAdmin(user.id) }
            }
        }

        action {
            val chatId = update.message?.chatId ?: return@action
            val args = update.message?.text.orEmpty().split(" ").drop(1)
            val identifier = args.getOrNull(0)
            val days = args.getOrNull(1)?.toLongOrNull()

            if (identifier == null || days == null || days <= 0) {
                adminService.reply(chatId, ACTIVATE_USAGE)
                return@action
            }

            val target = userInfoService.findUserByUsernameOrTelegramId(identifier)
            val targetTelegramId = target?.telegramUserId
            if (target == null || targetTelegramId == null) {
                adminService.reply(chatId, ACTIVATE_USER_NOT_FOUND.format(identifier))
                return@action
            }

            val updated = userInfoService.activateSubscription(targetTelegramId, days)
            val expiresAt = updated?.subscriptionExpiresAt
            if (expiresAt != null) {
                val displayName = target.username?.let { "@$it" } ?: identifier
                adminService.reply(chatId, ACTIVATE_SUCCESS.format(displayName, expiresAt.toLocalDate()))
            }
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
        }
    }

    transition {
        name = "Manage access lists (admin)"

        listOf(MoneyManagerState.MENU, MoneyManagerState.MINI_APP_GATEWAY).forEach { state ->
            condition {
                from = state
                eventType = EventType.COMMAND
                guard { accessListOf(update.message?.text) != null && adminService.isAdmin(user.id) }
            }
        }

        action {
            val chatId = update.message?.chatId ?: return@action
            val words = update.message?.text.orEmpty().trim().split(" ").filter { it.isNotBlank() }
            val command = words.first()
            val access = checkNotNull(ACCESS_LISTS[command])
            val reply = when (words.getOrNull(1) ?: LIST_ACTION) {
                LIST_ACTION -> listUsers(command, access, userInfoService)
                ADD_ACTION, REMOVE_ACTION -> words.getOrNull(2)
                    ?.let { changeAccess(command, access, words[1] == ADD_ACTION, it, userInfoService, adminService) }
                    ?: ACCESS_USAGE.format(command)
                else -> ACCESS_USAGE.format(command)
            }
            adminService.reply(chatId, reply)
        }

        then {
            to = MoneyManagerState.MENU
            noReply = true
        }
    }
}

private fun accessListOf(text: String?): UserAccess? = text?.trim()?.split(" ")?.firstOrNull()?.let { ACCESS_LISTS[it] }

private fun listUsers(command: String, access: UserAccess, userInfoService: UserInfoService): String {
    val users = userInfoService.usersWithAccess(access)
    if (users.isEmpty()) return ACCESS_EMPTY.format(command)
    return users.joinToString("\n") { user -> "${formatUserHandle(user)} · ${user.telegramUserId}" }
}

private fun changeAccess(
    command: String,
    access: UserAccess,
    adding: Boolean,
    identifier: String,
    userInfoService: UserInfoService,
    adminService: AdminService,
): String {
    val target = userInfoService.findUserByUsernameOrTelegramId(identifier)
    val targetId = target?.telegramUserId ?: return ACCESS_USER_NOT_FOUND.format(identifier)
    val name = formatUserHandle(target)
    if (adding) {
        if (access == UserAccess.BLOCKED && adminService.isAdmin(targetId)) return ADMIN_CANNOT_BE_BLOCKED
        userInfoService.setAccess(targetId, access)
        return ACCESS_ADDED.format(name, command)
    }
    if (target.access != access) return ACCESS_NOT_LISTED.format(name, command)
    userInfoService.setAccess(targetId, UserAccess.REGULAR)
    return ACCESS_REMOVED.format(name, command)
}

fun ActionContext<MoneyManagerState, MoneyManagerContext>.isAdminCommand(adminService: AdminService): Boolean {
    val command = update.message?.text?.trim()?.split(" ")?.firstOrNull() ?: return false
    return command in ADMIN_COMMANDS && adminService.isAdmin(user.id)
}
