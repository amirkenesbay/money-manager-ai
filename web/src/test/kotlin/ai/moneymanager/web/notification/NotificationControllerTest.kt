package ai.moneymanager.web.notification

import ai.moneymanager.domain.model.DEFAULT_NOTIFICATION_ICON
import ai.moneymanager.domain.model.FrequencyType
import ai.moneymanager.domain.model.NotificationCreationResult
import ai.moneymanager.domain.model.NotificationToggleResult
import ai.moneymanager.repository.entity.NotificationEntity
import ai.moneymanager.service.NotificationService
import ai.moneymanager.service.SubscriptionLimitsService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.json
import ai.moneymanager.web.mockInvoked
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.DayOfWeek
import java.time.LocalDateTime

private const val NOTIFICATIONS_PATH = "/api/v1/notifications"
private const val TIMEZONE = "Asia/Almaty"
private const val FREE_ACTIVE_LIMIT = 3
private const val CREATE_NOTIFICATION = "createNotification"
private const val DELETE_NOTIFICATION = "deleteNotification"

@WebMvcTest(controllers = [NotificationController::class])
@ApiWebLayerSetup
@Import(SubscriptionLimitsService::class)
class NotificationControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var notificationService: NotificationService

    private val evening = reminder(id = ObjectId(), owner = TEST_USER_ID)
    private val foreign = reminder(id = ObjectId(), owner = TEST_USER_ID + 1)

    @BeforeEach
    fun givenReminders() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser().copy(timezone = TIMEZONE))
        `when`(notificationService.getNotifications(TEST_USER_ID)).thenReturn(listOf(evening))
        `when`(notificationService.getNotification(evening.id!!)).thenReturn(evening)
        `when`(notificationService.getNotification(foreign.id!!)).thenReturn(foreign)
    }

    @Test
    fun `lists reminders of the user with a fallback icon`() {
        mockMvc.perform(get(NOTIFICATIONS_PATH).withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Записать траты"))
            .andExpect(jsonPath("$[0].icon").value(DEFAULT_NOTIFICATION_ICON))
            .andExpect(jsonPath("$[0].frequencyType").value("DAILY"))
            .andExpect(jsonPath("$[0].nextFireTime").exists())
    }

    @Test
    fun `creates a weekly reminder and drops fields the schedule does not use`() {
        val weekly = reminder(id = ObjectId(), owner = TEST_USER_ID).copy(frequencyType = FrequencyType.WEEKLY, dayOfWeek = DayOfWeek.SUNDAY)
        `when`(
            notificationService.createNotification(
                TEST_USER_ID, "Итоги недели", "📊", FrequencyType.WEEKLY, null, 20, 0, DayOfWeek.SUNDAY, null, null, TIMEZONE, FREE_ACTIVE_LIMIT
            )
        ).thenReturn(NotificationCreationResult.Created(weekly))

        mockMvc.perform(post(NOTIFICATIONS_PATH).withTelegramAuth().json(body("WEEKLY", dayOfWeek = "SUNDAY", dayOfMonth = 5, customInterval = 4)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.dayOfWeek").value("SUNDAY"))
    }

    @Test
    fun `rejects a schedule without its day`() {
        mockMvc.perform(post(NOTIFICATIONS_PATH).withTelegramAuth().json(body("MONTHLY")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Расписание заполнено не полностью. Проверьте время и день."))
        mockMvc.perform(post(NOTIFICATIONS_PATH).withTelegramAuth().json(body("CUSTOM_DAYS", customInterval = 366)))
            .andExpect(status().isBadRequest)

        check(!mockInvoked(notificationService, CREATE_NOTIFICATION)) { "an invalid schedule must not be saved" }
    }

    @Test
    fun `offers premium when the free limit of active reminders is reached`() {
        `when`(
            notificationService.createNotification(
                TEST_USER_ID, "Итоги недели", "📊", FrequencyType.DAILY, null, 20, 0, null, null, null, TIMEZONE, FREE_ACTIVE_LIMIT
            )
        ).thenReturn(NotificationCreationResult.LimitReached(FREE_ACTIVE_LIMIT))

        mockMvc.perform(post(NOTIFICATIONS_PATH).withTelegramAuth().json(body("DAILY")))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.code").value("NOTIFICATION_LIMIT_REACHED"))
    }

    @Test
    fun `does not turn on a reminder beyond the free limit`() {
        `when`(notificationService.setActive(evening.id!!, true, TIMEZONE, FREE_ACTIVE_LIMIT))
            .thenReturn(NotificationToggleResult.LimitReached(FREE_ACTIVE_LIMIT))

        mockMvc.perform(patch("$NOTIFICATIONS_PATH/${evening.id}").withTelegramAuth().json("""{"isActive":true}"""))
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.details.limit").value(FREE_ACTIVE_LIMIT))
    }

    @Test
    fun `hides reminders of other users`() {
        mockMvc.perform(delete("$NOTIFICATIONS_PATH/${foreign.id}").withTelegramAuth())
            .andExpect(status().isNotFound)

        check(!mockInvoked(notificationService, DELETE_NOTIFICATION)) { "a foreign reminder must stay" }
    }

    @Test
    fun `asks for a time zone before planning a reminder`() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())

        mockMvc.perform(post(NOTIFICATIONS_PATH).withTelegramAuth().json(body("DAILY")))
            .andExpect(status().isBadRequest)
    }

    private fun body(
        frequencyType: String,
        dayOfWeek: String? = null,
        dayOfMonth: Int? = null,
        customInterval: Int? = null,
    ): String {
        val weekday = dayOfWeek?.let { "\"$it\"" } ?: "null"
        return """{"name":"Итоги недели","icon":"📊","frequencyType":"$frequencyType","customInterval":$customInterval,""" +
            """"hour":20,"minute":0,"dayOfWeek":$weekday,"dayOfMonth":$dayOfMonth,"monthOfYear":null}"""
    }

    private fun reminder(id: ObjectId, owner: Long) = NotificationEntity(
        id = id,
        telegramUserId = owner,
        name = "Записать траты",
        frequencyType = FrequencyType.DAILY,
        hour = 21,
        minute = 0,
        nextFireTime = LocalDateTime.of(2026, 10, 27, 21, 0),
    )
}
