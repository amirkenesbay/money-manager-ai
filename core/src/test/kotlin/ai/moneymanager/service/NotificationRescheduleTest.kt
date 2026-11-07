package ai.moneymanager.service

import ai.moneymanager.domain.event.TimezoneChanged
import ai.moneymanager.domain.model.FALLBACK_TIMEZONE
import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.repository.NotificationRepository
import ai.moneymanager.repository.entity.NotificationEntity
import ai.moneymanager.testing.TEST_USER_ID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.time.ZoneId

private const val EVENING_HOUR = 21
private const val MOSCOW = "Europe/Moscow"

class NotificationRescheduleTest {
    private val repository = mock(NotificationRepository::class.java)
    private val service = NotificationService(repository)

    @Test
    fun `moves active reminders to the same local hour in the new time zone`() {
        val daily = reminder(isActive = true)
        val paused = reminder(isActive = false)
        `when`(repository.findByTelegramUserIdOrderByAuditInfoCreatedAtAsc(TEST_USER_ID)).thenReturn(listOf(daily, paused))
        val saved = mutableListOf<NotificationEntity>()
        `when`(repository.saveAll(anyList<NotificationEntity>())).thenAnswer { invocation -> invocation.getArgument<List<NotificationEntity>>(0).also { saved.addAll(it) } }

        service.onTimezoneChanged(TimezoneChanged(TEST_USER_ID, MOSCOW))

        val moved = saved.single()
        val localTime = moved.nextFireTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of(MOSCOW))
        assertThat(localTime.hour).isEqualTo(EVENING_HOUR)
        assertThat(localTime.minute).isZero()
        assertThat(moved.nextFireTime).isAfter(LocalDateTime.now())
    }

    private fun reminder(isActive: Boolean) = NotificationEntity(
        telegramUserId = TEST_USER_ID,
        name = "Записать траты за день",
        frequencyType = FrequencyType.DAILY,
        hour = EVENING_HOUR,
        minute = 0,
        isActive = isActive,
        nextFireTime = service.computeNextFireTime(FrequencyType.DAILY, null, EVENING_HOUR, 0, null, null, null, FALLBACK_TIMEZONE),
    )
}
