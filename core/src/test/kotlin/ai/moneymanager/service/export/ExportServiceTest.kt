package ai.moneymanager.service.export

import ai.moneymanager.domain.event.ExportReady
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.ExportFormat
import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.UserInfoService
import ai.moneymanager.testing.TEST_USER_ID
import ai.moneymanager.testing.testGroup
import ai.moneymanager.testing.testLocalizationService
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private const val RUSSIAN = "ru"
private val FROM: LocalDate = LocalDate.of(2026, 10, 1)
private val TO: LocalDate = LocalDate.of(2026, 10, 31)
private const val ZIP_SIGNATURE = "PK"

class ExportServiceTest {
    private val group = testGroup()
    private val repository = mock(FinanceOperationRepository::class.java)
    private val userInfoService = mock(UserInfoService::class.java)
    private val events = mutableListOf<Any>()
    private val clock = Clock.fixed(TO.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val service = ExportService(repository, userInfoService, testLocalizationService(), { events.add(it) }, clock)
    private val filter = OperationFilter(checkNotNull(group.id), FROM, TO)

    @Test
    fun `sends a csv excel opens without broken letters`() {
        givenOperations(operation("3500", LocalDate.of(2026, 10, 12), "Магнум; \"сыр\""), operation("1200", LocalDate.of(2026, 10, 3), null))

        val count = service.export(TEST_USER_ID, RUSSIAN, group, FROM, TO, ExportFormat.CSV)

        val ready = events.single() as ExportReady
        val text = ready.content.toString(Charsets.UTF_8)
        assertThat(count).isEqualTo(2)
        assertThat(ready.fileName).isEqualTo("operations_2026-10-01_2026-10-31.csv")
        assertThat(text).startsWith("﻿Дата;Тип;Категория;Сумма;Валюта;Комментарий;Кто записал\r\n2026-10-03;Расход;Продукты;1200;KZT;;Амир")
        assertThat(text).contains("\"Магнум; \"\"сыр\"\"\"")
    }

    @Test
    fun `builds a real excel file`() {
        givenOperations(operation("3500", LocalDate.of(2026, 10, 12), "Магнум"))

        service.export(TEST_USER_ID, RUSSIAN, group, FROM, TO, ExportFormat.XLSX)

        val ready = events.single() as ExportReady
        assertThat(ready.fileName).endsWith(".xlsx")
        assertThat(String(ready.content.copyOfRange(0, ZIP_SIGNATURE.length))).isEqualTo(ZIP_SIGNATURE)
    }

    @Test
    fun `sends nothing for an empty period`() {
        givenOperations()

        assertThat(service.export(TEST_USER_ID, RUSSIAN, group, FROM, TO, ExportFormat.CSV)).isZero()
        assertThat(events).isEmpty()
    }

    private fun givenOperations(vararg operations: FinanceOperationEntity) {
        `when`(repository.findAll(filter)).thenReturn(operations.toList())
        `when`(userInfoService.displayNames(setOf(TEST_USER_ID))).thenReturn(mapOf(TEST_USER_ID to "Амир"))
    }

    private fun operation(amount: String, date: LocalDate, description: String?) = FinanceOperationEntity(
        id = ObjectId(), groupId = checkNotNull(group.id), creatorId = TEST_USER_ID, type = CategoryType.EXPENSE, amount = BigDecimal(amount),
        categoryId = ObjectId(), categoryName = "Продукты", categoryIcon = "🛒", operationDate = date, description = description,
    )
}
