package ai.moneymanager.service.fx

import ai.moneymanager.domain.model.Currency
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

private val RATES_XML = """
    <?xml version="1.0" encoding="utf-8"?>
    <rates>
        <title>Official exchange rates of National Bank of Republic Kazakhstan</title>
        <date>10.11.2026</date>
        <item><fullname>ДОЛЛАР США</fullname><title>USD</title><description>484.3</description><quant>1</quant></item>
        <item><fullname>АРМЯНСКИЙ ДРАМ</fullname><title>AMD</title><description>12.45</description><quant>10</quant></item>
        <item><fullname>ИРАНСКИЙ РИАЛ</fullname><title>IRR</title><description>5.25</description><quant>1000</quant></item>
    </rates>
""".trimIndent()

class NationalBankRatesParserTest {
    @Test
    fun `reads the rate per one unit and skips unknown currencies`() {
        val rates = parseNationalBankRates(RATES_XML.toByteArray())

        assertThat(rates).containsOnlyKeys(Currency.USD, Currency.AMD)
        assertThat(rates[Currency.USD]).isEqualByComparingTo("484.3")
        assertThat(rates[Currency.AMD]).isEqualByComparingTo("1.245")
    }

    @Test
    fun `an unpublished day has no rates`() {
        assertThat(parseNationalBankRates("<rates><date>12.11.2026</date></rates>".toByteArray())).isEmpty()
    }

    @Test
    fun `rejects documents with a doctype`() {
        val hostile = """<?xml version="1.0"?><!DOCTYPE rates [<!ENTITY x SYSTEM "file:///etc/passwd">]><rates><item><title>USD</title><description>&x;</description></item></rates>"""

        assertThatThrownBy { parseNationalBankRates(hostile.toByteArray()) }.isInstanceOf(Exception::class.java)
    }
}
