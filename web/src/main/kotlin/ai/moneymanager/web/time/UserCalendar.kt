package ai.moneymanager.web.time

import ai.moneymanager.domain.model.UserInfo
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private val EARLIEST_ZONE_ON_EARTH: ZoneId = ZoneOffset.MAX

@Component
class UserCalendar(
    private val clock: Clock,
) {
    fun today(user: UserInfo): LocalDate = LocalDate.now(clock.withZone(zoneOf(user)))

    private fun zoneOf(user: UserInfo): ZoneId =
        user.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: EARLIEST_ZONE_ON_EARTH
}
