package ai.moneymanager.web.group

import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.GroupService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$API_V1_PREFIX/groups")
class GroupController(
    private val groupService: GroupService,
    private val groupAccessGuard: GroupAccessGuard,
) {
    @GetMapping
    fun groups(user: UserInfo): List<GroupResponse> {
        val userId = checkNotNull(user.telegramUserId)
        return groupService.getUserGroups(userId).map { it.toResponse(userId, user.activeGroupId) }
    }

    @PostMapping("/{groupId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun activate(user: UserInfo, @PathVariable groupId: ObjectId) {
        groupAccessGuard.requireMember(user, groupId)
        groupService.switchActiveGroup(checkNotNull(user.telegramUserId), groupId)
    }

    private fun MoneyGroup.toResponse(userId: Long, activeGroupId: ObjectId?): GroupResponse {
        val groupId = checkNotNull(id)
        return GroupResponse(
            id = groupId,
            name = name,
            type = type,
            currency = currency,
            isOwner = ownerId == userId,
            isActive = groupId == activeGroupId,
            memberCount = memberIds.size,
            initialBalance = initialBalance,
        )
    }
}
