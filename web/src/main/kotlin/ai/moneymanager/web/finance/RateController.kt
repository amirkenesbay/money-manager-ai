package ai.moneymanager.web.finance

import ai.moneymanager.domain.model.Currency
import ai.moneymanager.domain.model.ExchangeRate
import ai.moneymanager.service.fx.ExchangeRateService
import ai.moneymanager.web.API_V1_PREFIX
import ai.moneymanager.web.error.API_ERROR_KEY_PREFIX
import ai.moneymanager.web.error.ApiErrorCode
import ai.moneymanager.web.error.ApiException
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

private const val RATE_UNAVAILABLE_KEY = "${API_ERROR_KEY_PREFIX}rate_unavailable"

@RestController
@RequestMapping("$API_V1_PREFIX/rates")
class RateController(
    private val exchangeRateService: ExchangeRateService,
) {
    @GetMapping
    fun rate(
        @RequestParam from: Currency,
        @RequestParam to: Currency,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): ExchangeRate = exchangeRateService.rate(from, to, date) ?: throw ApiException(ApiErrorCode.NOT_FOUND, RATE_UNAVAILABLE_KEY)
}
