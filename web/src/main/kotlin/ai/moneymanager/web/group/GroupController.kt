package ai.moneymanager.web.group

import ai.moneymanager.domain.model.GroupCreationResult
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.MAX_GROUP_NAME_LENGTH
import ai.moneymanager.domain.model.MONTH_START_DAYS
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.buildInviteLink
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.error.badRequest
import ai.moneymanager.web.error.limitReached
import ai.moneymanager.web.input.requireName
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private const val PERSONAL_GROUP_LOCKED_KEY = "${API_ERROR_KEY_PREFIX}personal_group_locked"
private const val PERSONAL_GROUP_INVITE_KEY = "${API_ERROR_KEY_PREFIX}personal_group_invite"
private const val NEGATIVE_BALANCE_KEY = "${API_ERROR_KEY_PREFIX}negative_balance"
private const val MONTH_START_DAY_KEY = "${API_ERROR_KEY_PREFIX}month_start_day"

@RestController
@RequestMapping("$API_V1_PREFIX/groups")
class GroupController(
    private val groupService: GroupService,
    private val groupAccessGuard: GroupAccessGuard,
    private val subscriptionLimitsService: SubscriptionLimitsService,
    private val userInfoService: UserInfoService,
) {
    @GetMapping
    fun groups(user: UserInfo): List<GroupResponse> {
        val userId = checkNotNull(user.telegramUserId)
        return groupService.getUserGroups(userId).map { it.toResponse(user) }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(user: UserInfo, @RequestBody request: CreateGroupRequest): GroupResponse {
        val userId = checkNotNull(user.telegramUserId)
        val name = requireName(request.name, MAX_GROUP_NAME_LENGTH)
        val maxOwned = subscriptionLimitsService.maxOwnedSharedGroups(user.hasActivePaidSubscription())
        return when (val result = groupService.createGroup(userId, name, request.currency, maxOwned)) {
            is GroupCreationResult.Created -> result.group.toResponse(user, result.group.id)
            GroupCreationResult.Duplicate -> throw duplicate(name)
            is GroupCreationResult.LimitReached -> throw limitReached(ApiErrorCode.GROUP_LIMIT_REACHED, result.limit)
        }
    }

    @PatchMapping("/{groupId}")
    fun update(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: UpdateGroupRequest): GroupResponse {
        val group = groupAccessGuard.requireOwner(user, groupId)
        val userId = checkNotNull(user.telegramUserId)
        val renamed = request.name?.let { raw ->
            val name = requireName(raw, MAX_GROUP_NAME_LENGTH)
            groupService.updateGroupName(userId, groupId, name) ?: throw duplicate(name)
        } ?: group
        val withCurrency = request.currency?.let { checkNotNull(groupService.updateCurrency(userId, groupId, it)) } ?: renamed
        val updated = request.monthStartDay?.let { day ->
            if (day !in MONTH_START_DAYS) throw badRequest(MONTH_START_DAY_KEY)
            checkNotNull(groupService.updateMonthStartDay(userId, groupId, day))
        } ?: withCurrency
        val split = request.splitExpenses?.let { enabled ->
            if (group.type == GroupType.PERSONAL) throw badRequest(PERSONAL_GROUP_LOCKED_KEY)
            checkNotNull(groupService.updateSplitExpenses(userId, groupId, enabled))
        } ?: updated
        val withDefault = request.splitDefault?.let { splitDefault ->
            if (group.type == GroupType.PERSONAL) throw badRequest(PERSONAL_GROUP_LOCKED_KEY)
            checkNotNull(groupService.updateSplitDefault(userId, groupId, splitDefault))
        } ?: split
        return withDefault.toResponse(user)
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(user: UserInfo, @PathVariable groupId: ObjectId) {
        val group = groupAccessGuard.requireOwner(user, groupId)
        if (group.type == GroupType.PERSONAL) throw badRequest(PERSONAL_GROUP_LOCKED_KEY)
        groupService.deleteGroup(checkNotNull(user.telegramUserId), groupId)
    }

    @PostMapping("/{groupId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun activate(user: UserInfo, @PathVariable groupId: ObjectId) {
        groupAccessGuard.requireMember(user, groupId)
        groupService.switchActiveGroup(checkNotNull(user.telegramUserId), groupId)
    }

    @GetMapping("/{groupId}/members")
    fun members(user: UserInfo, @PathVariable groupId: ObjectId): List<MemberResponse> {
        val group = groupAccessGuard.requireMember(user, groupId)
        val names = userInfoService.displayNames(group.memberIds)
        return group.memberIds
            .sortedByDescending { it == group.ownerId }
            .map { memberId -> MemberResponse(memberId, names.getValue(memberId), memberId == group.ownerId) }
    }

    @GetMapping("/{groupId}/invite")
    fun invite(user: UserInfo, @PathVariable groupId: ObjectId): InviteResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        if (group.type == GroupType.PERSONAL) throw badRequest(PERSONAL_GROUP_INVITE_KEY)
        return InviteResponse(group.inviteToken, buildInviteLink(group.inviteToken))
    }

    @PutMapping("/{groupId}/initial-balance")
    fun initialBalance(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: InitialBalanceRequest): GroupResponse {
        groupAccessGuard.requireMember(user, groupId)
        if (request.amount.signum() < 0) throw badRequest(NEGATIVE_BALANCE_KEY)
        val updated = checkNotNull(groupService.setInitialBalance(checkNotNull(user.telegramUserId), groupId, request.amount))
        return updated.toResponse(user)
    }

    @PutMapping("/{groupId}/notices")
    fun notices(user: UserInfo, @PathVariable groupId: ObjectId, @RequestBody request: GroupNoticesRequest): GroupResponse {
        val group = groupAccessGuard.requireMember(user, groupId)
        val updated = checkNotNull(userInfoService.setGroupNoticeLevel(checkNotNull(user.telegramUserId), groupId, request.level))
        return group.toResponse(updated)
    }

    private fun duplicate(name: String) = ApiException(ApiErrorCode.GROUP_DUPLICATE, messageArgs = listOf(name))

    private fun MoneyGroup.toResponse(user: UserInfo, activeGroupId: ObjectId? = user.activeGroupId): GroupResponse {
        val groupId = checkNotNull(id)
        return GroupResponse(
            id = groupId,
            name = name,
            type = type,
            currency = currency,
            isOwner = ownerId == user.telegramUserId,
            isActive = groupId == activeGroupId,
            memberCount = memberIds.size,
            initialBalance = initialBalance,
            monthStartDay = monthStartDay,
            noticeLevel = user.noticeLevel(groupId),
            splitExpenses = splitsExpenses,
            splitDefault = splitDefault,
        )
    }
}
