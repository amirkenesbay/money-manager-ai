package ai.moneymanager.web.access

import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.GroupService
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

private const val GROUP_NOT_FOUND_KEY = "${API_ERROR_KEY_PREFIX}group_not_found"
private const val GROUP_ACCESS_DENIED_KEY = "${API_ERROR_KEY_PREFIX}group_access_denied"
private const val GROUP_OWNER_ONLY_KEY = "${API_ERROR_KEY_PREFIX}group_owner_only"

@Component
class GroupAccessGuard(
    private val groupService: GroupService,
) {
    fun requireMember(user: UserInfo, groupId: ObjectId): MoneyGroup {
        val group = groupService.getGroup(groupId)
            ?: throw ApiException(ApiErrorCode.NOT_FOUND, GROUP_NOT_FOUND_KEY)
        val userId = user.telegramUserId
        if (userId == null || userId !in group.memberIds) {
            throw ApiException(ApiErrorCode.FORBIDDEN, GROUP_ACCESS_DENIED_KEY)
        }
        return group
    }

    fun requireOwner(user: UserInfo, groupId: ObjectId): MoneyGroup {
        val group = requireMember(user, groupId)
        if (group.ownerId != user.telegramUserId) throw ApiException(ApiErrorCode.FORBIDDEN, GROUP_OWNER_ONLY_KEY)
        return group
    }
}
