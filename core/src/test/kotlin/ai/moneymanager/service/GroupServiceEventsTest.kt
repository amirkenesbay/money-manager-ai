package ai.moneymanager.service

import ai.moneymanager.domain.event.GroupDeleted
import ai.moneymanager.domain.event.GroupSettingChange
import ai.moneymanager.domain.event.GroupSettingChanged
import ai.moneymanager.domain.event.MemberJoined
import ai.moneymanager.domain.model.GroupType
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.MoneyGroupEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.Optional

private const val OWNER_ID = 1L
private const val NEWCOMER_ID = 2L
private const val INVITE_TOKEN = "ABC123XYZ"

class GroupServiceEventsTest {
    private val groupId = ObjectId()
    private val family = MoneyGroupEntity(groupId, "Семья", INVITE_TOKEN, OWNER_ID, setOf(OWNER_ID), GroupType.SHARED)
    private val groupRepository = mock(MoneyGroupRepository::class.java)
    private val publishedEvents = mutableListOf<Any>()
    private val service = GroupService(groupRepository, mock(UserInfoRepository::class.java), mock(CategoryService::class.java)) { publishedEvents.add(it) }

    @BeforeEach
    fun givenFamilyGroup() {
        `when`(groupRepository.findById(groupId)).thenReturn(Optional.of(family))
        `when`(groupRepository.findByInviteToken(INVITE_TOKEN)).thenReturn(family)
        `when`(groupRepository.save(any(MoneyGroupEntity::class.java))).thenAnswer { it.getArgument<MoneyGroupEntity>(0) }
    }

    @Test
    fun `tells the group about a new name but not about the same one`() {
        service.updateGroupName(OWNER_ID, groupId, "Дом")
        service.updateGroupName(OWNER_ID, groupId, "Семья")

        assertThat(publishedEvents).containsExactly(GroupSettingChanged(groupId, OWNER_ID, GroupSettingChange.Renamed("Семья", "Дом")))
    }

    @Test
    fun `tells the group who changed the starting balance`() {
        service.setInitialBalance(NEWCOMER_ID, groupId, BigDecimal("150000"))

        assertThat(publishedEvents)
            .containsExactly(GroupSettingChanged(groupId, NEWCOMER_ID, GroupSettingChange.InitialBalanceChanged(BigDecimal.ZERO, BigDecimal("150000"))))
    }

    @Test
    fun `announces a newcomer only once`() {
        service.joinGroup(NEWCOMER_ID, INVITE_TOKEN)
        service.joinGroup(OWNER_ID, INVITE_TOKEN)

        assertThat(publishedEvents).containsExactly(MemberJoined(groupId, NEWCOMER_ID))
    }

    @Test
    fun `remembers who was in a deleted group`() {
        service.deleteGroup(OWNER_ID, groupId)

        assertThat(publishedEvents).containsExactly(GroupDeleted(groupId, OWNER_ID, "Семья", setOf(OWNER_ID)))
    }
}
