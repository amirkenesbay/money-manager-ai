package ai.moneymanager.web.access

import ai.moneymanager.service.GroupService
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import ai.moneymanager.web.security.TEST_USER_ID
import ai.moneymanager.web.testGroup
import ai.moneymanager.web.testUser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

private const val STRANGER_ID = 7L

class GroupAccessGuardTest {
    private val groupService = mock(GroupService::class.java)
    private val guard = GroupAccessGuard(groupService)

    private val groupId = ObjectId()
    private val group = testGroup(groupId, members = setOf(TEST_USER_ID))

    @Test
    fun `returns group to its member`() {
        `when`(groupService.getGroup(groupId)).thenReturn(group)

        assertThat(guard.requireMember(user(TEST_USER_ID), groupId)).isEqualTo(group)
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

        val error = catchThrowableOfType(ApiException::class.java) { guard.requireMember(user(TEST_USER_ID), groupId) }

        assertThat(error.code).isEqualTo(ApiErrorCode.NOT_FOUND)
    }

    private fun user(telegramUserId: Long?) = testUser().copy(telegramUserId = telegramUserId)
}
