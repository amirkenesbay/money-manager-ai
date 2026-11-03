package ai.moneymanager.domain.text

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MerchantKeysTest {
    @Test
    fun `turns the same place written differently into one key`() {
        assertThat(merchantKey("  МАГНУМ!! ")).isEqualTo("магнум")
        assertThat(merchantKey("Магнум")).isEqualTo("магнум")
    }

    @Test
    fun `drops digits and punctuation and keeps the words apart`() {
        assertThat(merchantKey("Small, ул. Абая 12")).isEqualTo("small ул абая")
    }

    @Test
    fun `treats yo as ye`() {
        assertThat(merchantKey("Ёлка")).isEqualTo("елка")
    }

    @Test
    fun `has no key without letters`() {
        assertThat(merchantKey("12 345")).isNull()
        assertThat(merchantKey(null)).isNull()
    }
}
