package ai.moneymanager.repository.entity.common

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime

@EnableMongoAuditing
data class AuditInfo(
    @CreatedDate
    @Field("created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Field("modified_at")
    var modifiedAt: LocalDateTime? = null
)