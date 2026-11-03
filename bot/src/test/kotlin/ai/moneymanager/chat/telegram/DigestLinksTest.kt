package ai.moneymanager.chat.telegram

import ai.moneymanager.domain.model.DigestKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DigestLinksTest {
    @Test
    fun `reads the digest to mute from the start command of its button`() {
        DigestKind.entries.forEach { kind ->
            assertThat(mutedDigestKind("/start ${digestMuteParameter(kind)}")).isEqualTo(kind)
        }
    }

    @Test
    fun `ignores other start commands`() {
        assertThat(mutedDigestKind("/start finance")).isNull()
        assertThat(mutedDigestKind("/start")).isNull()
        assertThat(mutedDigestKind(null)).isNull()
    }
}
