package ai.moneymanager.service

import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.domain.model.NotificationToggleResult
import ai.moneymanager.repository.NotificationRepository
import ai.moneymanager.repository.entity.NotificationEntity
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional

private const val OWNER_ID = 42L
private const val FREE_ACTIVE_LIMIT = 3
private const val TIMEZONE = "Asia/Almaty"

class NotificationServiceTest {
    private val repository = mock(NotificationRepository::class.java)
    private val service = NotificationService(repository)
    private val paused = NotificationEntity(
        id = ObjectId(),
        telegramUserId = OWNER_ID,
        name = "Записать траты",
        frequencyType = FrequencyType.DAILY,
        hour = 21,
        minute = 0,
        isActive = false,
        nextFireTime = LocalDateTime.of(2026, 10, 1, 21, 0),
    )

    @BeforeEach
    fun givenPausedReminder() {
        `when`(repository.findById(paused.id!!)).thenReturn(Optional.of(paused))
        `when`(repository.save(any(NotificationEntity::class.java))).thenAnswer { it.getArgument<NotificationEntity>(0) }
    }

    @Test
    fun `turns a reminder on and plans its next run`() {
        `when`(repository.countByTelegramUserIdAndIsActiveTrue(OWNER_ID)).thenReturn(2)

        val result = service.setActive(paused.id!!, true, TIMEZONE, FREE_ACTIVE_LIMIT)

        val updated = (result as NotificationToggleResult.Updated).notification
        assertThat(updated.isActive).isTrue()
        assertThat(updated.nextFireTime).isAfter(paused.nextFireTime)
    }

    @Test
    fun `does not turn on a reminder beyond the free limit`() {
        `when`(repository.countByTelegramUserIdAndIsActiveTrue(OWNER_ID)).thenReturn(FREE_ACTIVE_LIMIT.toLong())

        assertThat(service.setActive(paused.id!!, true, TIMEZONE, FREE_ACTIVE_LIMIT))
            .isEqualTo(NotificationToggleResult.LimitReached(FREE_ACTIVE_LIMIT))
    }

    @Test
    fun `turns a reminder on without a limit for premium`() {
        `when`(repository.countByTelegramUserIdAndIsActiveTrue(OWNER_ID)).thenReturn(10)

        assertThat(service.setActive(paused.id!!, true, TIMEZONE, null)).isInstanceOf(NotificationToggleResult.Updated::class.java)
    }
}
