package ai.moneymanager.web.loan

import ai.moneymanager.domain.model.loan.LoanDirection
import ai.moneymanager.domain.model.loan.LoanDraft
import ai.moneymanager.repository.entity.LoanEntity
import ai.moneymanager.repository.entity.LoanRepayment
import ai.moneymanager.service.GroupService
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.service.loan.LoanService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testUser
import ai.moneymanager.web.ApiWebLayerSetup
import ai.moneymanager.web.access.GroupAccessGuard
import ai.moneymanager.web.json
import ai.moneymanager.web.security.TestInitData
import ai.moneymanager.web.time.UserCalendar
import ai.moneymanager.web.withTelegramAuth
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@WebMvcTest(controllers = [LoanController::class])
@ApiWebLayerSetup
@Import(GroupAccessGuard::class, UserCalendar::class)
class LoanControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userCalendar: UserCalendar

    @MockitoBean
    private lateinit var userInfoService: UserInfoService

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var loanService: LoanService

    private val groupId = ObjectId()

    @BeforeEach
    fun givenGroup() {
        `when`(userInfoService.getOrCreate(TestInitData.profile())).thenReturn(testUser())
        `when`(groupService.getGroup(groupId)).thenReturn(testGroup(groupId))
    }

    @Test
    fun `lends money to a person with a due date`() {
        val today = userCalendar.today(testUser())
        val draft = LoanDraft(LoanDirection.LENT, "Ерлан", BigDecimal("20000"), today, today.plusDays(25), "на ремонт")
        `when`(loanService.create(groupId, TEST_USER_ID, draft)).thenReturn(loan(draft))

        mockMvc.perform(post("/api/v1/groups/$groupId/loans").withTelegramAuth()
            .json("""{"direction":"LENT","person":" Ерлан ","amount":20000,"dueDate":"${today.plusDays(25)}","note":"на ремонт"}"""))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.person").value("Ерлан"))
            .andExpect(jsonPath("$.remaining").value("20000"))
            .andExpect(jsonPath("$.open").value(true))
    }

    @Test
    fun `refuses a loan without a person`() {
        mockMvc.perform(post("/api/v1/groups/$groupId/loans").withTelegramAuth().json("""{"direction":"LENT","person":"  ","amount":20000}"""))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(loanService)
    }

    @Test
    fun `takes a repayment and tells what is left`() {
        val today = userCalendar.today(testUser())
        val loanId = ObjectId()
        val repaid = loan(LoanDraft(LoanDirection.LENT, "Ерлан", BigDecimal("20000"), today, null, null))
            .copy(id = loanId, repayments = listOf(LoanRepayment(amount = BigDecimal("5000"), date = today)))
        `when`(loanService.repay(groupId, TEST_USER_ID, loanId, BigDecimal("5000"), today)).thenReturn(repaid)

        mockMvc.perform(post("/api/v1/groups/$groupId/loans/$loanId/repayments").withTelegramAuth().json("""{"amount":5000}"""))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.remaining").value("15000"))
            .andExpect(jsonPath("$.repayments.length()").value(1))
    }

    private fun loan(draft: LoanDraft) = LoanEntity(
        id = ObjectId(), groupId = groupId, creatorId = TEST_USER_ID, direction = draft.direction, person = draft.person,
        amount = draft.amount, lentOn = draft.date, dueDate = draft.dueDate, note = draft.note,
    )
}
