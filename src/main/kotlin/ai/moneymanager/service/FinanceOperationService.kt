package ai.moneymanager.service

import ai.moneymanager.dto.CreateFinanceOperationRequestDto
import ai.moneymanager.dto.HistoryFinanceOperationDto
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.LocalDateTime

interface FinanceOperationService {
    /**
     * Создать финансовую операцию
     */
    fun create(createDto: CreateFinanceOperationRequestDto): FinanceOperationEntity

    /**
     * Получить полную сумму всей группы
     */
    fun getBalanceGroupFromGroupEntity(groupId: ObjectId, telegramUserId: Long): BigDecimal

    /**
     * Получить историю финансовых операций у группы в конкретный период
     */
    fun getHistoryFinanceOperationByPeriodFromGroupEntity(groupId: ObjectId, telegramUserId: Long, fromDate: LocalDateTime,
                                                          toDate: LocalDateTime): List<HistoryFinanceOperationDto>

    /**
     * Получить всю историю финансовых операций группы
     */
    fun getAllHistoryFinanceOperationFromGroupEntity(groupId: ObjectId, telegramUserId: Long): List<HistoryFinanceOperationDto>

    /**
     * Получить баланс группы за всю историю доходов группы
     */
    fun getIncomeOperationBalanceForAllTimeFromGroup(groupId: ObjectId, telegramUserId: Long): BigDecimal

    /**
     * Получить баланс группы за всю историю расходов группы
     */
    fun getExpenseOperationBalanceForAllTimeFromGroupEntity(groupId: ObjectId, telegramUserId: Long): BigDecimal

    /**
     * Получить сумму доходов по категории за весь период
     */
    fun getIncomeBalanceByCategoryId(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId): BigDecimal

    /**
     * Получить сумму расходов по категории за весь период
     */
    fun getExpenseBalanceByCategoryId(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId): BigDecimal

    /**
     * Получить баланс категории за всю историю существование категории
     */
    fun getBalanceCategoryFromCategoryEntity(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId): BigDecimal

    /**
     * Получить историю финансовых операций у категории в конкретный период
     */
    fun getHistoryFinanceOperationByPeriodFromCategoryEntity(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId,
                                                             fromDate: LocalDateTime, toDate: LocalDateTime): List<HistoryFinanceOperationDto>

    /**
     * Получить всю историю финансовых операций категории
     */
    fun getAllHistoryFinanceOperationFromCategoryEntity(groupId: ObjectId, telegramUserId: Long, categoryId: ObjectId): List<HistoryFinanceOperationDto>
}