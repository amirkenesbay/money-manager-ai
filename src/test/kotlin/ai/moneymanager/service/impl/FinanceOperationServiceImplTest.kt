package ai.moneymanager.service.impl

import ai.moneymanager.dto.HistoryFinanceOperationDto
import ai.moneymanager.repository.CategoryRepository
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.MoneyGroupRepository
import ai.moneymanager.repository.UserInfoRepository
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class FinanceOperationServiceImplTest{

    @Mock
    private lateinit var userInfoRepository: UserInfoRepository

    @Mock
    private lateinit var moneyGroupRepository: MoneyGroupRepository

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @Mock
    private lateinit var financeOperationRepository: FinanceOperationRepository

    @InjectMocks
    private lateinit var financeOperationServiceImpl: FinanceOperationServiceImpl

    /**
     * Получить полную сумму всей группы
     */
    @Test
    fun getBalanceGroupFromGroupEntity(){
        val groupId = ObjectId()
        val telegramUserId = 1L
        val balance = BigDecimal("123.45")

        whenever(moneyGroupRepository.existsByIdAndOwnerOrMemberTelegramUserId(groupId, telegramUserId))
            .thenReturn(true)

        whenever(financeOperationRepository.getBalanceByGroupIdFromGroupEntity(groupId))
            .thenReturn(balance)

        val result = financeOperationServiceImpl.getBalanceGroupFromGroupEntity(groupId, telegramUserId)
        assertEquals(balance, result)

        verify(moneyGroupRepository).existsByIdAndOwnerOrMemberTelegramUserId(groupId, telegramUserId)
        verify(financeOperationRepository).getBalanceByGroupIdFromGroupEntity(groupId)
    }

    /**
     * Получить историю финансовых операций у группы в конкретный период
     */
    @Test
    fun getHistoryFinanceOperationByPeriodFromGroupEntity(){
        val groupId= ObjectId()
        val telegramUserId = 1L
        val fromDate = LocalDateTime.of(2026, 1, 1, 0, 0)
        val toDate = fromDate.plusDays(30)
        val financeOperations = emptyList<HistoryFinanceOperationDto>()

        whenever(moneyGroupRepository.existsByIdAndOwnerOrMemberTelegramUserId(groupId, telegramUserId))
            .thenReturn(true)

        whenever(financeOperationRepository.getHistoryFinanceOperationByPeriodFromGroupEntity(groupId, fromDate, toDate))
            .thenReturn(financeOperations)

        val result = financeOperationServiceImpl.getHistoryFinanceOperationByPeriodFromGroupEntity(groupId, telegramUserId, fromDate, toDate)

        assertEquals(financeOperations,result)

        verify(moneyGroupRepository).existsByIdAndOwnerOrMemberTelegramUserId(groupId, telegramUserId)
        verify(financeOperationRepository).getHistoryFinanceOperationByPeriodFromGroupEntity(groupId, fromDate, toDate)
    }

    /**
     * Получить историю финансовых операций у группы в конкретный период - проверяем его if
     */
    @Test
    fun getHistoryFinanceOperationByPeriodFromGroupEntityChekIf(){
        val groupId = ObjectId()
        val telegramUserId = 1L
        val fromDate = LocalDateTime.of(2026, 1, 1, 0, 0)
        val toDate = fromDate.minusDays(2)

        whenever(
            moneyGroupRepository.existsByIdAndOwnerOrMemberTelegramUserId(groupId, telegramUserId)
        ).thenReturn(true)

        val exception = assertThrows<IllegalStateException> {
            financeOperationServiceImpl.getHistoryFinanceOperationByPeriodFromGroupEntity(
                groupId, telegramUserId, fromDate, toDate
            )
        }

        assertEquals("fromDate must be <= toDate", exception.message)

        verify(financeOperationRepository, never())
            .getHistoryFinanceOperationByPeriodFromGroupEntity(groupId, fromDate, toDate)
    }

    /**
     * Получить историю финансовых операций у группы - у юзера нет доступа так как он не является участником группы
     */
    @Test
    fun getHistoryFinanceOperationByPeriodFromGroupEntityThrowsWhenUserHasNoAccess() {

        val groupId = ObjectId()
        val telegramUserId = 1L
        val fromDate = LocalDateTime.of(2026, 1, 1, 0, 0)
        val toDate = fromDate.plusDays(30)

        whenever(
            moneyGroupRepository.existsByIdAndOwnerOrMemberTelegramUserId(groupId, telegramUserId)
        ).thenReturn(false)

        val exception = assertThrows<IllegalStateException> {
            financeOperationServiceImpl.getHistoryFinanceOperationByPeriodFromGroupEntity(
                groupId,
                telegramUserId,
                fromDate,
                toDate
            )
        }

        assertEquals("User is not a member of the group", exception.message)

        verify(financeOperationRepository, never())
            .getHistoryFinanceOperationByPeriodFromGroupEntity(groupId, fromDate, toDate)

        verify(moneyGroupRepository)
            .existsByIdAndOwnerOrMemberTelegramUserId(groupId, telegramUserId)
    }

}