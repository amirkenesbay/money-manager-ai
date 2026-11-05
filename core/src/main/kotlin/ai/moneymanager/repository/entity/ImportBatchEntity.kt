package ai.moneymanager.repository.entity

import ai.moneymanager.domain.model.imports.ImportSource
import ai.moneymanager.repository.entity.common.AuditInfo
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "import_batch")
data class ImportBatchEntity(
    val id: ObjectId? = null,
    @Indexed
    val userId: Long,
    val groupId: ObjectId,
    val source: ImportSource?,
    val count: Int,
    val undone: Boolean = false,
    val auditInfo: AuditInfo = AuditInfo(),
)
