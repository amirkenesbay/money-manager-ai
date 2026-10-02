package ai.moneymanager.config

import ai.moneymanager.repository.entity.UserInfoEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.bson.Document
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.IndexOperations

private const val CREATED_INDEX_NAME = "telegramUserId_unique"

class UserTelegramIdIndexTest {
    private val indexOperations = mock(IndexOperations::class.java)
    private val mongoTemplate = mock(MongoTemplate::class.java).also {
        `when`(it.indexOps(UserInfoEntity::class.java)).thenReturn(indexOperations)
    }
    private val runner = UserTelegramIdIndex(mongoTemplate)

    @Test
    fun `creates a unique index over numeric telegram ids only`() {
        `when`(indexOperations.createIndex(any(Index::class.java))).thenReturn(CREATED_INDEX_NAME)

        runner.run(DefaultApplicationArguments())

        val index = mockingDetails(indexOperations).invocations.single().arguments.single() as Index
        assertThat(index.indexKeys).isEqualTo(Document("telegramUserId", 1))
        assertThat(index.indexOptions["unique"]).isEqualTo(true)
        assertThat(index.indexOptions["partialFilterExpression"]).isNotNull()
    }

    @Test
    fun `keeps the application starting when duplicates already exist`() {
        `when`(indexOperations.createIndex(any(Index::class.java))).thenThrow(DuplicateKeyException("E11000"))

        assertThatCode { runner.run(DefaultApplicationArguments()) }.doesNotThrowAnyException()
    }
}
