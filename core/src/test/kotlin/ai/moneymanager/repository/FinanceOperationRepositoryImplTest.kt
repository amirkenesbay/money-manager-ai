package ai.moneymanager.repository

import ai.moneymanager.domain.model.CategoryType
import ai.moneymanager.domain.model.OperationFilter
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.time.LocalDate

class FinanceOperationRepositoryImplTest {
    private val groupId = ObjectId()
    private val from = LocalDate.of(2026, 7, 1)
    private val to = LocalDate.of(2026, 7, 31)

    @Test
    fun `restricts to the group only when no filters are given`() {
        val query = operationsQuery(OperationFilter(groupId)).queryObject

        assertThat(query).isEqualTo(Document("groupId", groupId))
    }

    @Test
    fun `combines type, category and period filters`() {
        val categoryId = ObjectId()
        val filter = OperationFilter(groupId, from, to, CategoryType.EXPENSE, categoryId)

        val query = operationsQuery(filter).queryObject

        assertThat(query["groupId"]).isEqualTo(groupId)
        assertThat(query["type"]).isEqualTo(CategoryType.EXPENSE)
        assertThat(query["categoryId"]).isEqualTo(categoryId)
        assertThat(query["operationDate"]).isEqualTo(Document("\$gte", from).append("\$lte", to))
    }

    @Test
    fun `supports an open-ended period`() {
        val query = operationsQuery(OperationFilter(groupId, from = from)).queryObject

        assertThat(query["operationDate"]).isEqualTo(Document("\$gte", from))
    }

    @Test
    fun `counts all matches but fetches one page, newest first`() {
        val mongoTemplate = mock(MongoTemplate::class.java)
        val repository = FinanceOperationRepositoryImpl(mongoTemplate)

        repository.findPage(OperationFilter(groupId), PageRequest.of(2, 20))

        val queries = mockingDetails(mongoTemplate).invocations.associate { it.method.name to it.arguments[0] as Query }
        assertThat(queries.getValue("count").skip).isZero()
        assertThat(queries.getValue("count").limit).isZero()
        val pageQuery = queries.getValue("find")
        assertThat(pageQuery.skip).isEqualTo(40)
        assertThat(pageQuery.limit).isEqualTo(20)
        assertThat(pageQuery.sortObject).isEqualTo(Document("operationDate", -1).append("auditInfo.createdAt", -1))
    }
}
