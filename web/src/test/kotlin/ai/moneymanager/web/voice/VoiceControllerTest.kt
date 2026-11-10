package ai.moneymanager.web.voice

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.question.AnswerCard
import ai.moneymanager.domain.model.question.FinanceIntent
import ai.moneymanager.domain.model.question.FinanceQuestion
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.AiRateLimitResult
import ai.moneymanager.service.AiRateLimitService
import ai.moneymanager.service.CategoryRuleService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.question.AskService
import ai.moneymanager.service.question.AskSuggestionService
import ai.moneymanager.service.question.FinanceAnswerService
import ai.moneymanager.service.nlp.CommandParserService
import ai.moneymanager.service.nlp.WAV_MIME_TYPE
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.ai.AiRequestGuard
import ai.moneymanager.web.ask.AskAnswers
import ai.moneymanager.web.loan.VoiceLoans
import ai.moneymanager.service.loan.LoanService
import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.web.anyOf
import ai.moneymanager.web.mockInvoked
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.time.UserCalendar
import ai.moneymanager.web.withTelegramAuth
import ai.moneymanager.service.nlp.ForeignCommands
import ai.moneymanager.service.fx.ExchangeRateService
import ai.moneymanager.domain.model.Currency
import java.math.BigDecimal
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import ai.moneymanager.domain.model.nlp.enum.AiSurface

private const val GROUP_CONTEXT = "context"
private const val FREE_AI_LIMIT = 10
private const val PARSE_VOICE = "parseVoiceCommands"

@WebMvcTest(controllers = [VoiceController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, UserCalendar::class, AiRequestGuard::class, AskAnswers::class, VoiceLoans::class, ForeignCommands::class)
class VoiceControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userCalendar: UserCalendar

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var categoryService: CategoryService

    @MockitoBean
    private lateinit var categoryRuleService: CategoryRuleService

    @MockitoBean
    private lateinit var aiRateLimitService: AiRateLimitService

    @MockitoBean
    private lateinit var aiPromptService: AiPromptService

    @MockitoBean
    private lateinit var commandParserService: CommandParserService

    @MockitoBean
    private lateinit var askService: AskService

    @MockitoBean
    private lateinit var askSuggestionService: AskSuggestionService

    @MockitoBean
    private lateinit var financeAnswerService: FinanceAnswerService

    @MockitoBean
    private lateinit var loanService: LoanService

    @MockitoBean
    private lateinit var exchangeRateService: ExchangeRateService

    private val groupId = ObjectId()
    private val group = testGroup(groupId)
    private val taxi = Category(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId)
    private val audio = wav()

    @BeforeEach
    fun givenGroup() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(group)
        `when`(categoryService.getCategoriesByGroup(groupId)).thenReturn(listOf(taxi))
        `when`(aiPromptService.groupContext(userCalendar.today(testUser()), group.currency, listOf(taxi), language = testUser().language)).thenReturn(GROUP_CONTEXT)
        `when`(aiRateLimitService.tryConsume(TEST_USER_ID, false)).thenReturn(AiRateLimitResult.Allowed)
    }

    @Test
    fun `turns a phrase into operation drafts with matched and new categories`() {
        `when`(commandParserService.parseVoiceCommands(audio, GROUP_CONTEXT, WAV_MIME_TYPE, AiSurface.MINI_APP)).thenReturn(
            listOf(
                BotCommand.AddExpense(2000.0, "такси", null, null, null),
                BotCommand.AddExpense(1500.0, "Кафе", "в центре", "☕", null),
                BotCommand.ShowBalance,
            )
        )

        mockMvc.perform(voice(audio))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.operations.length()").value(2))
            .andExpect(jsonPath("$.operations[0].categoryId").value(taxi.id!!.toHexString()))
            .andExpect(jsonPath("$.operations[0].isNewCategory").value(false))
            .andExpect(jsonPath("$.operations[1].categoryName").value("Кафе"))
            .andExpect(jsonPath("$.operations[1].categoryIcon").value("☕"))
            .andExpect(jsonPath("$.operations[1].isNewCategory").value(true))
            .andExpect(jsonPath("$.operations[1].description").value("в центре"))
    }

    @Test
    fun `converts an amount said in another currency and keeps it`() {
        val today = userCalendar.today(testUser())
        `when`(commandParserService.parseVoiceCommands(audio, GROUP_CONTEXT, WAV_MIME_TYPE, AiSurface.MINI_APP))
            .thenReturn(listOf(BotCommand.AddExpense(5.0, "такси", null, null, null, currency = Currency.USD)))
        `when`(exchangeRateService.convert(BigDecimal.valueOf(5.0), Currency.USD, group.currency, today)).thenReturn(BigDecimal("2421.5"))

        mockMvc.perform(voice(audio))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.operations[0].amount").value("2421.5"))
            .andExpect(jsonPath("$.operations[0].original.amount").value("5.0"))
            .andExpect(jsonPath("$.operations[0].original.currency").value("USD"))
    }

    @Test
    fun `answers with no operations when the phrase is not about money`() {
        `when`(commandParserService.parseVoiceCommands(audio, GROUP_CONTEXT, WAV_MIME_TYPE, AiSurface.MINI_APP)).thenReturn(listOf(BotCommand.OutOfContext("[voice message]")))

        mockMvc.perform(voice(audio))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.operations.length()").value(0))
    }

    @Test
    fun `answers a spoken question with a card`() {
        val question = FinanceQuestion(FinanceIntent.SUM, categories = listOf("Такси"))
        `when`(commandParserService.parseVoiceCommands(audio, GROUP_CONTEXT, WAV_MIME_TYPE, AiSurface.MINI_APP)).thenReturn(listOf(BotCommand.AskFinance(question)))
        `when`(askService.answer(anyOf(UserInfo::class.java), anyOf(MoneyGroup::class.java), anyOf(FinanceQuestion::class.java), anyOf(LocalDate::class.java))).thenReturn(AnswerCard("🚕 Такси", "12 000 ₸"))

        mockMvc.perform(voice(audio))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.operations.length()").value(0))
            .andExpect(jsonPath("$.answer.title").value("🚕 Такси"))
            .andExpect(jsonPath("$.answer.headline").value("12 000 ₸"))
    }

    @Test
    fun `turns a spoken loan into a loan draft`() {
        `when`(commandParserService.parseVoiceCommands(audio, GROUP_CONTEXT, WAV_MIME_TYPE, AiSurface.MINI_APP))
            .thenReturn(listOf(BotCommand.AddLoan(LoanDirection.LENT, "Ерлан", java.math.BigDecimal("20000"), null, "на ремонт")))

        mockMvc.perform(voice(audio))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.loan.kind").value("NEW"))
            .andExpect(jsonPath("$.loan.person").value("Ерлан"))
            .andExpect(jsonPath("$.loan.note").value("на ремонт"))
    }

    @Test
    fun `refuses anything but a wav recording before spending the ai quota`() {
        mockMvc.perform(voice("not a wav at all, just some bytes padding padding".toByteArray()))
            .andExpect(status().isBadRequest)

        check(!mockInvoked(aiRateLimitService, "tryConsume")) { "invalid audio must not spend the quota" }
    }

    @Test
    fun `stops at the daily ai limit like the bot`() {
        `when`(aiRateLimitService.tryConsume(TEST_USER_ID, false)).thenReturn(AiRateLimitResult.Exceeded(FREE_AI_LIMIT, 7200))

        mockMvc.perform(voice(audio))
            .andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.code").value("AI_LIMIT_REACHED"))
            .andExpect(jsonPath("$.details.limit").value(FREE_AI_LIMIT))

        check(!mockInvoked(commandParserService, PARSE_VOICE)) { "the recording must not reach Gemini over the limit" }
    }

    @Test
    fun `tells the client to retry later when gemini is busy`() {
        `when`(commandParserService.parseVoiceCommands(audio, GROUP_CONTEXT, WAV_MIME_TYPE, AiSurface.MINI_APP)).thenReturn(listOf(BotCommand.RateLimitError(30)))

        mockMvc.perform(voice(audio))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("AI_BUSY"))
            .andExpect(jsonPath("$.details.retryAfterSeconds").value(30))
    }

    private fun voice(content: ByteArray) =
        post("/api/v1/groups/$groupId/voice").withTelegramAuth().contentType(WAV_MIME_TYPE).content(content)

    private fun wav(): ByteArray {
        val bytes = ByteArray(64)
        "RIFF".toByteArray().copyInto(bytes, 0)
        "WAVE".toByteArray().copyInto(bytes, 8)
        return bytes
    }
}
