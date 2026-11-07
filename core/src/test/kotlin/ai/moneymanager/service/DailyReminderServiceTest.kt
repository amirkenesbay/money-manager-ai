package ai.moneymanager.service

import ai.moneymanager.domain.model.FALLBACK_TIMEZONE
import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.domain.model.UserAccess
import ai.moneymanager.repository.NotificationRepository
import ai.moneymanager.repository.UserInfoRepository
import ai.moneymanager.repository.entity.NotificationEntity
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testLocalizationService
import ai.moneymanager.testing.testSubscriptionLimits
import ai.moneymanager.testing.testUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalDateTime

private const val EVENING_HOUR = 21

class DailyReminderServiceTest {
    private val saved = mutableListOf<NotificationEntity>()
    private val seeded = mutableListOf<Long>()
    private val notificationRepository = mock(NotificationRepository::class.java)
    private val userInfoService = object : UserInfoService(mock(UserInfoRepository::class.java), {}) {
        override fun markDailyReminderSeeded(telegramUserId: Long) {
            seeded += telegramUserId
        }
    }
    private val service = DailyReminderService(
        userInfoService,
        NotificationService(notificationRepository),
        testLocalizationService(),
        testSubscriptionLimits(LocalDate.now()),
    )

    @BeforeEach
    fun givenRepository() {
        `when`(notificationRepository.save(any(NotificationEntity::class.java))).thenAnswer {
            it.getArgument<NotificationEntity>(0).also { entity -> saved += entity }
        }
        `when`(notificationRepository.findByTelegramUserIdOrderByAuditInfoCreatedAtAsc(anyLong())).thenReturn(emptyList())
    }

    @Test
    fun `reminds a user to log spending every evening in Almaty when the zone is unknown`() {
        val created = service.ensureFor(testUser())

        val reminder = saved.single()
        assertThat(created).isTrue()
        assertThat(reminder.frequencyType).isEqualTo(FrequencyType.DAILY)
        assertThat(reminder.hour).isEqualTo(EVENING_HOUR)
        assertThat(reminder.name).isEqualTo("Записать траты за день")
        assertThat(seeded).containsExactly(TEST_USER_ID)
        assertThat(service.ensureFor(testUser().copy(timezone = FALLBACK_TIMEZONE, dailyReminderSeeded = true))).isFalse()
    }

    @Test
    fun `does not add a second daily reminder but remembers the user was handled`() {
        `when`(notificationRepository.findByTelegramUserIdOrderByAuditInfoCreatedAtAsc(TEST_USER_ID)).thenReturn(listOf(NOTIFICATION))

        assertThat(service.ensureFor(testUser())).isFalse()
        assertThat(saved).isEmpty()
        assertThat(seeded).containsExactly(TEST_USER_ID)
    }

    @Test
    fun `leaves blocked users alone`() {
        assertThat(service.ensureFor(testUser().copy(access = UserAccess.BLOCKED))).isFalse()
        assertThat(saved).isEmpty()
        assertThat(seeded).isEmpty()
    }
}

private val NOTIFICATION = NotificationEntity(
    telegramUserId = TEST_USER_ID, name = "Записать траты", frequencyType = FrequencyType.DAILY,
    hour = EVENING_HOUR, minute = 0, nextFireTime = LocalDateTime.now(),
)
