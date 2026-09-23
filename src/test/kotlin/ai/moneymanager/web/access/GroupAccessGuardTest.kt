package ai.moneymanager.web.access

import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.service.GroupService
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

private const val MEMBER_ID = 42L
private const val STRANGER_ID = 7L

class GroupAccessGuardTest {
    private val groupService = mock(GroupService::class.java)
    private val guard = GroupAccessGuard(groupService)

    private val groupId = ObjectId()
    private val group = MoneyGroup(
        id = groupId,
        name = "Семья",
        inviteToken = "abc123xyz",
        ownerId = MEMBER_ID,
        memberIds = setOf(MEMBER_ID),
        type = GroupType.SHARED,
    )

    @Test
    fun `returns group to its member`() {
        `when`(groupService.getGroup(groupId)).thenReturn(group)

        assertThat(guard.requireMember(user(MEMBER_ID), groupId)).isEqualTo(group)
    }

    @Test
    fun `denies access to a group the user is not in`() {
        `when`(groupService.getGroup(groupId)).thenReturn(group)

        val error = catchThrowableOfType(ApiException::class.java) { guard.requireMember(user(STRANGER_ID), groupId) }

        assertThat(error.code).isEqualTo(ApiErrorCode.FORBIDDEN)
    }

    @Test
    fun `denies access when user has no telegram id`() {
        `when`(groupService.getGroup(groupId)).thenReturn(group)

        val error = catchThrowableOfType(ApiException::class.java) { guard.requireMember(user(null), groupId) }

        assertThat(error.code).isEqualTo(ApiErrorCode.FORBIDDEN)
    }

    @Test
    fun `reports missing group as not found`() {
        `when`(groupService.getGroup(groupId)).thenReturn(null)

        val error = catchThrowableOfType(ApiException::class.java) { guard.requireMember(user(MEMBER_ID), groupId) }

        assertThat(error.code).isEqualTo(ApiErrorCode.NOT_FOUND)
    }

    private fun user(telegramUserId: Long?) = UserInfo(
        id = ObjectId(),
        username = null,
        firstName = "Amir",
        lastName = null,
        telegramUserId = telegramUserId,
        languageCode = "ru",
    )
}
