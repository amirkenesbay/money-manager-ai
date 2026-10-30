package ai.moneymanager.chat.config

import com.mongodb.client.result.UpdateResult
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

class ChatStateClassMigrationTest {
    private val mongoTemplate = mock(MongoTemplate::class.java).also {
        `when`(it.updateMulti(any(Query::class.java), any(Update::class.java), anyString()))
            .thenReturn(UpdateResult.acknowledged(1, 1, null))
    }

    @Test
    fun `renames legacy state and context classes inside chat dialogs`() {
        ChatStateClassMigration(mongoTemplate).run(DefaultApplicationArguments())

        val updates = mockingDetails(mongoTemplate).invocations.map { invocation ->
            val update = invocation.arguments[1] as Update
            assertThat(invocation.arguments[2]).isEqualTo("chats")
            update.updateObject["\$set"] as Document
        }

        assertThat(updates).containsExactly(
            Document("dialogs.$[dialog].stateClass", "ai.moneymanager.chat.state.MoneyManagerState"),
            Document("dialogs.$[dialog].context._class", "ai.moneymanager.chat.state.MoneyManagerContext"),
        )
    }
}
