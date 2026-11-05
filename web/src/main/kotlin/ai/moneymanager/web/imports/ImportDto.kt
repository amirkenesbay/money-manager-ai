package ai.moneymanager.web.imports

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.imports.ColumnRole
import ai.moneymanager.domain.model.imports.ImportPreview
import ai.moneymanager.domain.model.imports.ImportSource
import ai.moneymanager.domain.model.imports.SkipReason
import org.bson.types.ObjectId
import java.time.LocalDate

data class ImportColumnResponse(
    val header: String,
    val samples: List<String>,
    val role: ColumnRole?,
)

data class ImportCategoryResponse(
    val key: String,
    val name: String,
    val type: CategoryType,
    val count: Int,
    val existingId: ObjectId?,
)

data class ImportPreviewResponse(
    val token: String,
    val source: ImportSource?,
    val columns: List<ImportColumnResponse>,
    val needsColumns: Boolean,
    val count: Int,
    val from: LocalDate?,
    val to: LocalDate?,
    val skipped: Map<SkipReason, Int>,
    val categories: List<ImportCategoryResponse>,
    val importsLeft: Int?,
)

data class ImportColumnsRequest(
    val roles: Map<ColumnRole, Int>,
)

enum class CategoryAction {
    CREATE,
    SKIP,
    EXISTING,
}

data class CategoryChoiceRequest(
    val action: CategoryAction,
    val categoryId: ObjectId? = null,
)

data class ImportCommitRequest(
    val categories: Map<String, CategoryChoiceRequest> = emptyMap(),
)

data class ImportResultResponse(
    val importId: ObjectId,
    val count: Int,
    val createdCategories: Int,
    val from: LocalDate?,
    val to: LocalDate?,
    val historyLimited: Boolean,
)

fun ImportPreview.toResponse(importsLeft: Int?) = ImportPreviewResponse(
    token = token,
    source = source,
    columns = columns.map { ImportColumnResponse(it.header, it.samples, it.role) },
    needsColumns = columns.none { it.role == ColumnRole.DATE },
    count = count,
    from = from,
    to = to,
    skipped = skipped,
    categories = categories.map { ImportCategoryResponse(it.key, it.name, it.type, it.count, it.existingId) },
    importsLeft = importsLeft,
)
