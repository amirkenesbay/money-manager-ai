package ai.moneymanager.service.fx

import ai.moneymanager.domain.model.Currency
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

private const val RATES_URL = "https://nationalbank.kz/rss/get_rates.cfm?fdate="
private val REQUEST_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val TIMEOUT: Duration = Duration.ofSeconds(5)
private const val ITEM_TAG = "item"
private const val CODE_TAG = "title"
private const val RATE_TAG = "description"
private const val QUANTITY_TAG = "quant"
private const val DISALLOW_DOCTYPE_FEATURE = "http://apache.org/xml/features/disallow-doctype-decl"

@Component
class NationalBankRatesSource : KztRatesSource {
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()

    override fun fetch(date: LocalDate): Map<Currency, BigDecimal> {
        val request = HttpRequest.newBuilder(URI.create(RATES_URL + date.format(REQUEST_DATE_FORMAT))).timeout(TIMEOUT).GET().build()
        return parseNationalBankRates(client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body())
    }
}

fun parseNationalBankRates(xml: ByteArray): Map<Currency, BigDecimal> {
    val items = secureDocumentBuilder().parse(ByteArrayInputStream(xml)).getElementsByTagName(ITEM_TAG)
    return (0 until items.length).mapNotNull { index -> rateOf(items.item(index) as Element) }.toMap()
}

private fun rateOf(item: Element): Pair<Currency, BigDecimal>? {
    val currency = Currency.entries.find { it.name == item.text(CODE_TAG) } ?: return null
    val rate = item.text(RATE_TAG)?.toBigDecimalOrNull() ?: return null
    val quantity = item.text(QUANTITY_TAG)?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
    return currency to rate.divide(quantity, RATE_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
}

private fun Element.text(tag: String): String? = getElementsByTagName(tag).item(0)?.textContent?.trim()

private fun secureDocumentBuilder() = DocumentBuilderFactory.newInstance().apply {
    setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    setFeature(DISALLOW_DOCTYPE_FEATURE, true)
    isExpandEntityReferences = false
}.newDocumentBuilder()
