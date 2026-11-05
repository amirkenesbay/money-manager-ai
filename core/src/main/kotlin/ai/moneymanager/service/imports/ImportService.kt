package ai.moneymanager.service.imports

import ai.moneymanager.domain.event.OperationsChanged
import ai.moneymanager.domain.model.Category
import ai.moneymanager.domain.model.CategoryCreationResult
import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.MoneyGroup
import ai.moneymanager.domain.model.imports.CategoryDecision
import ai.moneymanager.domain.model.imports.ColumnMapping
import ai.moneymanager.domain.model.imports.ColumnRole
import ai.moneymanager.domain.model.imports.ImportCategory
import ai.moneymanager.domain.model.imports.ImportColumn
import ai.moneymanager.domain.model.imports.ImportPreview
import ai.moneymanager.domain.model.imports.ImportResult
import ai.moneymanager.domain.model.imports.ImportRow
import ai.moneymanager.domain.model.imports.ImportSource
import ai.moneymanager.domain.model.imports.SkipReason
import ai.moneymanager.domain.text.matchesEntityName
import ai.moneymanager.repository.FinanceOperationRepository
import ai.moneymanager.repository.ImportBatchRepository
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.ImportBatchEntity
import ai.moneymanager.service.CategoryService
import org.bson.types.ObjectId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

private const val SAMPLE_VALUES = 2
private const val FINGERPRINT_SEPARATOR = "|"
private val OTHER_CATEGORY_NAMES = setOf("другое", "other", "басқа", "басқалар")

sealed interface ImportCommit {
    data class Done(val result: ImportResult) : ImportCommit
    data class LimitReached(val limit: Int) : ImportCommit
    data object Expired : ImportCommit
}

private data class Prepared(val rows: List<ImportRow>, val skipped: Map<SkipReason, Int>)

private data class Resolved(val categories: Map<String, Category>, val created: Int)

@Service
class ImportService(
    private val importDrafts: ImportDrafts,
    private val importColumnAi: ImportColumnAi,
    private val financeOperationRepository: FinanceOperationRepository,
    private val importBatchRepository: ImportBatchRepository,
    private val categoryService: CategoryService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun read(userId: Long, group: MoneyGroup, bytes: ByteArray): ImportPreview {
        val table = readTable(bytes)
        val known = knownMapping(table)
        val mapping = known?.second ?: guessMapping(table)
        val source = known?.first ?: mapping?.let { ImportSource.DETECTED }
        return preview(importDrafts.create(userId, checkNotNull(group.id), table, source, mapping), group)
    }

    fun guessWithAi(token: String, userId: Long, group: MoneyGroup): ImportPreview? {
        val draft = importDrafts.find(token, userId, checkNotNull(group.id)) ?: return null
        val mapping = importColumnAi.guess(draft.table) ?: return preview(draft, group)
        return preview(importDrafts.update(draft.copy(source = ImportSource.AI, mapping = mapping)), group)
    }

    fun remap(token: String, userId: Long, group: MoneyGroup, roles: Map<ColumnRole, Int>): ImportPreview? {
        val draft = importDrafts.find(token, userId, checkNotNull(group.id)) ?: return null
        val mapping = ColumnMapping(roles.filterValues { it in draft.table.headers.indices })
        return preview(importDrafts.update(draft.copy(source = ImportSource.MANUAL, mapping = mapping)), group)
    }

    fun find(token: String, userId: Long, group: MoneyGroup): ImportPreview? =
        importDrafts.find(token, userId, checkNotNull(group.id))?.let { preview(it, group) }

    fun importsUsed(userId: Long): Int = importBatchRepository.countByUserIdAndUndoneFalse(userId).toInt()

    fun commit(
        token: String,
        userId: Long,
        group: MoneyGroup,
        decisions: Map<String, CategoryDecision>,
        maxImports: Int?,
        maxCategoriesPerType: Int,
    ): ImportCommit {
        val groupId = checkNotNull(group.id)
        val draft = importDrafts.find(token, userId, groupId)?.takeIf { it.mapping?.complete == true } ?: return ImportCommit.Expired
        if (maxImports != null && importsUsed(userId) >= maxImports) return ImportCommit.LimitReached(maxImports)
        val rows = prepare(draft, group).rows
        val categories = categoryService.getCategoriesByGroup(groupId)
        val resolved = resolveCategories(rows, categories, decisions, userId, group, maxCategoriesPerType)
        val kept = rows.mapNotNull { row -> resolved.categories[row.categoryKey]?.let { row to it } }
        val batch = importBatchRepository.save(ImportBatchEntity(userId = userId, groupId = groupId, source = draft.source, count = kept.size))
        financeOperationRepository.saveAll(kept.map { (row, category) -> operationOf(row, category, userId, groupId, checkNotNull(batch.id)) })
        importDrafts.remove(token)
        eventPublisher.publishEvent(OperationsChanged(groupId))
        val dates = kept.map { it.first.date }
        return ImportCommit.Done(ImportResult(checkNotNull(batch.id), kept.size, resolved.created, dates.minOrNull(), dates.maxOrNull()))
    }

    fun undo(userId: Long, group: MoneyGroup, importId: ObjectId): Boolean {
        val batch = importBatchRepository.findByIdAndUserId(importId, userId)?.takeIf { !it.undone && it.groupId == group.id } ?: return false
        financeOperationRepository.deleteByGroupIdAndImportId(batch.groupId, importId)
        importBatchRepository.save(batch.copy(undone = true))
        eventPublisher.publishEvent(OperationsChanged(batch.groupId))
        return true
    }

    private fun preview(draft: ImportDraft, group: MoneyGroup): ImportPreview {
        val mapping = draft.mapping
        val prepared = if (mapping?.complete == true) prepare(draft, group) else Prepared(emptyList(), emptyMap())
        val categories = categoryService.getCategoriesByGroup(checkNotNull(group.id))
        val roles = mapping?.roles.orEmpty().entries.associate { (role, index) -> index to role }
        return ImportPreview(
            token = draft.token,
            source = draft.source,
            columns = draft.table.headers.mapIndexed { index, header ->
                ImportColumn(header, draft.table.rows.take(SAMPLE_VALUES).map { draft.table.cell(it, index) }, roles[index])
            },
            count = prepared.rows.size,
            from = prepared.rows.minOfOrNull { it.date },
            to = prepared.rows.maxOfOrNull { it.date },
            skipped = prepared.skipped,
            categories = prepared.rows.groupBy { it.categoryKey }.map { (key, rows) ->
                val first = rows.first()
                ImportCategory(key, first.category, first.type, rows.size, existingOf(categories, first)?.id)
            }.sortedByDescending { it.count },
        )
    }

    private fun prepare(draft: ImportDraft, group: MoneyGroup): Prepared {
        val parsed = parseRows(draft.table, checkNotNull(draft.mapping), group.currency)
        val from = parsed.rows.minOfOrNull { it.date } ?: return Prepared(parsed.rows, parsed.skipped)
        val to = checkNotNull(parsed.rows.maxOfOrNull { it.date })
        val existing = financeOperationRepository.findByGroupIdAndOperationDateBetweenOrderByOperationDateDesc(checkNotNull(group.id), from, to)
            .groupingBy { fingerprint(it.operationDate, it.type, it.amount, it.description) }.eachCount().toMutableMap()
        val fresh = parsed.rows.filter { row ->
            val key = fingerprint(row.date, row.type, row.amount, row.comment)
            val left = existing[key] ?: 0
            if (left > 0) existing[key] = left - 1
            left == 0
        }
        val duplicates = parsed.rows.size - fresh.size
        val skipped = if (duplicates > 0) parsed.skipped + (SkipReason.DUPLICATE to duplicates) else parsed.skipped
        return Prepared(fresh, skipped)
    }

    private fun resolveCategories(
        rows: List<ImportRow>,
        categories: List<Category>,
        decisions: Map<String, CategoryDecision>,
        userId: Long,
        group: MoneyGroup,
        maxCategoriesPerType: Int,
    ): Resolved {
        val known = categories.toMutableList()
        var created = 0
        val resolved = rows.distinctBy { it.categoryKey }.mapNotNull { row ->
            val category = when (val decision = decisions[row.categoryKey]) {
                CategoryDecision.Skip -> null
                is CategoryDecision.Existing -> known.firstOrNull { it.id == decision.categoryId && it.type == row.type }
                    ?: existingOf(known, row) ?: fallbackOf(known, row.type)
                CategoryDecision.Create, null -> existingOf(known, row) ?: when (val result = categoryService.createCategory(userId, row.category, null, row.type, checkNotNull(group.id), maxCategoriesPerType)) {
                    is CategoryCreationResult.Created -> result.category.also { known.add(it); created++ }
                    else -> fallbackOf(known, row.type)
                }
            }
            category?.let { row.categoryKey to it }
        }.toMap()
        return Resolved(resolved, created)
    }

    private fun existingOf(categories: List<Category>, row: ImportRow): Category? =
        categories.firstOrNull { it.type == row.type && matchesEntityName(it.name, row.category) }

    private fun fallbackOf(categories: List<Category>, type: CategoryType): Category? {
        val ofType = categories.filter { it.type == type }
        return ofType.firstOrNull { it.name.trim().lowercase() in OTHER_CATEGORY_NAMES } ?: ofType.firstOrNull()
    }

    private fun operationOf(row: ImportRow, category: Category, userId: Long, groupId: ObjectId, importId: ObjectId) = FinanceOperationEntity(
        groupId = groupId,
        creatorId = userId,
        type = row.type,
        amount = row.amount,
        categoryId = checkNotNull(category.id),
        categoryName = category.name,
        categoryIcon = category.icon,
        operationDate = row.date,
        description = row.comment,
        importId = importId,
    )

    private fun fingerprint(date: LocalDate, type: CategoryType, amount: BigDecimal, comment: String?): String =
        listOf(date, type, amount.stripTrailingZeros().toPlainString(), comment?.trim()?.lowercase().orEmpty()).joinToString(FINGERPRINT_SEPARATOR)
}

