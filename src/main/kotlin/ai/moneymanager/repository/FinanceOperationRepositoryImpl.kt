package ai.moneymanager.repository

import ai.moneymanager.domain.model.OperationFilter
import ai.moneymanager.repository.entity.FinanceOperationEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

private const val GROUP_ID = "groupId"
private const val TYPE = "type"
private const val CATEGORY_ID = "categoryId"
private const val OPERATION_DATE = "operationDate"
private const val CREATED_AT = "auditInfo.createdAt"

private val NEWEST_FIRST: Sort = Sort.by(Sort.Direction.DESC, OPERATION_DATE, CREATED_AT)

@Repository
class FinanceOperationRepositoryImpl(
    private val mongoTemplate: MongoTemplate
) : FinanceOperationRepositoryCustom {
    override fun findPage(filter: OperationFilter, pageable: Pageable): Page<FinanceOperationEntity> {
        val total = mongoTemplate.count(operationsQuery(filter), FinanceOperationEntity::class.java)
        val pageQuery = operationsQuery(filter).with(pageable).with(NEWEST_FIRST)
        val items = mongoTemplate.find(pageQuery, FinanceOperationEntity::class.java)
        return PageImpl(items, pageable, total)
    }
}

internal fun operationsQuery(filter: OperationFilter): Query {
    val criteria = Criteria.where(GROUP_ID).`is`(filter.groupId)
    filter.type?.let { criteria.and(TYPE).`is`(it) }
    filter.categoryId?.let { criteria.and(CATEGORY_ID).`is`(it) }
    if (filter.from != null || filter.to != null) {
        val operationDate = criteria.and(OPERATION_DATE)
        filter.from?.let { operationDate.gte(it) }
        filter.to?.let { operationDate.lte(it) }
    }
    return Query(criteria)
}
