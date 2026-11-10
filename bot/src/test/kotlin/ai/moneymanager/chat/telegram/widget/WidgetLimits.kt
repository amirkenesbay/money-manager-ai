package ai.moneymanager.chat.telegram.widget

import org.assertj.core.api.Assertions.assertThat
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

private const val MAX_COMPRESSED_BYTES = 4096
private const val MAX_HTML_BYTES = 32768

fun assertFitsWidgetLimits(html: String) {
    val bytes = ByteArrayOutputStream()
    DeflaterOutputStream(bytes, Deflater(Deflater.BEST_COMPRESSION)).use { it.write(html.toByteArray()) }
    assertThat(html.toByteArray().size).isLessThanOrEqualTo(MAX_HTML_BYTES)
    assertThat(bytes.size()).isLessThanOrEqualTo(MAX_COMPRESSED_BYTES)
}
