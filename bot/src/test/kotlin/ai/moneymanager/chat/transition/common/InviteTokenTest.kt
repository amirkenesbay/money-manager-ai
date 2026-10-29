package ai.moneymanager.chat.transition.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InviteTokenTest {
    @Test
    fun `reads the token from an invite start command`() {
        assertThat(inviteTokenOf("/start join_ABC123XYZ")).isEqualTo("ABC123XYZ")
    }

    @Test
    fun `finds no token in a plain start command`() {
        assertThat(inviteTokenOf("/start")).isNull()
    }

    @Test
    fun `finds no token in another start parameter`() {
        assertThat(inviteTokenOf("/start finance")).isNull()
    }

    @Test
    fun `finds no token without a message`() {
        assertThat(inviteTokenOf(null)).isNull()
    }
}
