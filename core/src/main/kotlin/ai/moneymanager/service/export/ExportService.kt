package ai.moneymanager.service.export

import ai.moneymanager.domain.event.ExportReady
import ai.moneymanager.domain.model.ExportFormat
import ai.moneymanager.domain.model.ExportRow
import ai.moneymanager.domain.model.ExportTable
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.service.LocalizationService
import ai.moneymanager.service.UserInfoService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

private val COLUMN_KEYS = listOf("date", "type", "category", "amount", "currency", "description", "author").map { "export.column.$it" }
private const val FILE_NAME_PREFIX = "operations"
private const val FILE_NAME_SEPARATOR = "_"
private const val ALL_TIME = "all"
private const val EXTENSION_SEPARATOR = "."

@Service
class ExportService(
    private val financeOperationRepository: FinanceOperationRepository,
    private val userInfoService: UserInfoService,
    private val localizationService: LocalizationService,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    fun export(telegramUserId: Long, language: String?, group: MoneyGroup, from: LocalDate?, to: LocalDate?, format: ExportFormat): Int {
        val operations = financeOperationRepository.findAll(OperationFilter(checkNotNull(group.id), from, to)).sortedBy { it.operationDate }
        if (operations.isEmpty()) return 0
        val content = writeExport(table(operations, group, language), format)
        eventPublisher.publishEvent(ExportReady(telegramUserId, group.name, from, to, operations.size, fileName(from, to, format), content))
        return operations.size
    }

    fun table(operations: List<FinanceOperationEntity>, group: MoneyGroup, language: String?): ExportTable {
        val authors = userInfoService.displayNames(operations.map { it.creatorId }.toSet())
        val rows = operations.map { operation ->
            ExportRow(
                date = operation.operationDate,
                type = localizationService.t("export.type.${operation.type.name.lowercase()}", language),
                category = operation.categoryName,
                amount = operation.amount,
                currency = group.currency.name,
                description = operation.description.orEmpty(),
                author = authors.getValue(operation.creatorId),
            )
        }
        return ExportTable(COLUMN_KEYS.map { localizationService.t(it, language) }, rows)
    }

    private fun fileName(from: LocalDate?, to: LocalDate?, format: ExportFormat): String =
        listOf(FILE_NAME_PREFIX, from?.toString() ?: ALL_TIME, (to ?: LocalDate.now(clock)).toString())
            .joinToString(FILE_NAME_SEPARATOR) + EXTENSION_SEPARATOR + format.extension
}
