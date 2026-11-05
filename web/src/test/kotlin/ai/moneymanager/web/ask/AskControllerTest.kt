package ai.moneymanager.web.ask

import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.UserInfo
import ai.moneymanager.domain.model.nlp.BotCommand
import ai.moneymanager.domain.model.question.AnswerCard
import ai.moneymanager.domain.model.question.AskSuggestions
import ai.moneymanager.domain.model.question.AskTopic
import ai.moneymanager.domain.model.question.AskTopicSuggestions
import ai.moneymanager.domain.model.question.FinanceIntent
import ai.moneymanager.domain.model.question.FinanceQuestion
import ai.moneymanager.service.AiPromptService
import ai.moneymanager.service.AiRateLimitResult
import ai.moneymanager.service.AiRateLimitService
import ai.moneymanager.service.CategoryRuleService
import ai.moneymanager.service.CategoryService
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.nlp.CommandParserService
import ai.moneymanager.service.question.AskService
import ai.moneymanager.service.question.AskSuggestionService
import ai.moneymanager.service.question.FinanceAnswerService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.ai.AiRequestGuard
import ai.moneymanager.web.anyOf
import ai.moneymanager.web.json
import ai.moneymanager.web.mockInvoked
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.time.UserCalendar
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

private const val GROUP_CONTEXT = "context"
private const val QUESTION = "сколько на такси в этом месяце?"
private const val EXAMPLE = "Сколько на такси в этом месяце?"
private const val PARSE_TEXT = "parseCommands"
private const val FREE_AI_LIMIT = 10

@WebMvcTest(controllers = [AskController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, UserCalendar::class, AiRequestGuard::class, AskAnswers::class)
class AskControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

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

    private val groupId = ObjectId()
    private val group = testGroup(groupId)
    private val taxi = Category(ObjectId(), "Такси", "🚕", CategoryType.EXPENSE, groupId)
    private val suggestions = AskSuggestions(listOf(EXAMPLE), listOf(AskTopicSuggestions(AskTopic.SUM, "Сколько", listOf(EXAMPLE))))

    @BeforeEach
    fun givenGroup() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(group)
        `when`(categoryService.getCategoriesByGroup(groupId)).thenReturn(listOf(taxi))
        `when`(aiPromptService.groupContext(group.currency, listOf(taxi))).thenReturn(GROUP_CONTEXT)
        `when`(aiRateLimitService.tryConsume(TEST_USER_ID, false)).thenReturn(AiRateLimitResult.Allowed)
        `when`(askSuggestionService.suggestions(anyOf(MoneyGroup::class.java), nullable(String::class.java), anyOf(LocalDate::class.java))).thenReturn(suggestions)
        `when`(askSuggestionService.examples(suggestions)).thenReturn(listOf(EXAMPLE))
    }

    @Test
    fun `answers a typed question with a card`() {
        val question = FinanceQuestion(FinanceIntent.SUM, categories = listOf("Такси"))
        `when`(commandParserService.parseCommands(QUESTION, GROUP_CONTEXT)).thenReturn(listOf(BotCommand.AskFinance(question)))
        `when`(askService.answer(anyOf(UserInfo::class.java), anyOf(MoneyGroup::class.java), anyOf(FinanceQuestion::class.java), anyOf(LocalDate::class.java)))
            .thenReturn(AnswerCard("🚕 Такси", "12 000 ₸", followUps = listOf("А в прошлом месяце?")))

        mockMvc.perform(ask("""{"text":"$QUESTION"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("🚕 Такси"))
            .andExpect(jsonPath("$.headline").value("12 000 ₸"))
            .andExpect(jsonPath("$.followUps[0]").value("А в прошлом месяце?"))
            .andExpect(jsonPath("$.understood").value(true))
    }

    @Test
    fun `offers examples when the text is not a question about money`() {
        `when`(commandParserService.parseCommands(QUESTION, GROUP_CONTEXT)).thenReturn(listOf(BotCommand.OutOfContext(QUESTION)))
        `when`(financeAnswerService.notUnderstood(testUser().language, listOf(EXAMPLE)))
            .thenReturn(AnswerCard("Пока не умею это 🙂", followUps = listOf(EXAMPLE), understood = false))

        mockMvc.perform(ask("""{"text":"$QUESTION"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.understood").value(false))
            .andExpect(jsonPath("$.followUps[0]").value(EXAMPLE))
    }

    @Test
    fun `refuses an empty or too long question before spending the ai quota`() {
        mockMvc.perform(ask("""{"text":"   "}""")).andExpect(status().isBadRequest)
        mockMvc.perform(ask("""{"text":"${"а".repeat(MAX_QUESTION_LENGTH + 1)}"}""")).andExpect(status().isBadRequest)

        check(!mockInvoked(aiRateLimitService, "tryConsume")) { "an invalid question must not spend the quota" }
    }

    @Test
    fun `stops at the daily ai limit`() {
        `when`(aiRateLimitService.tryConsume(TEST_USER_ID, false)).thenReturn(AiRateLimitResult.Exceeded(FREE_AI_LIMIT, 7200))

        mockMvc.perform(ask("""{"text":"$QUESTION"}"""))
            .andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.code").value("AI_LIMIT_REACHED"))

        check(!mockInvoked(commandParserService, PARSE_TEXT)) { "the question must not reach Gemini over the limit" }
    }

    @Test
    fun `lists personal question suggestions by topic`() {
        mockMvc.perform(get("/api/v1/groups/$groupId/ask/suggestions").withTelegramAuth())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.questions[0]").value(EXAMPLE))
            .andExpect(jsonPath("$.topics[0].topic").value("SUM"))
            .andExpect(jsonPath("$.topics[0].title").value("Сколько"))
    }

    private fun ask(body: String) = post("/api/v1/groups/$groupId/ask").withTelegramAuth().json(body)
}
