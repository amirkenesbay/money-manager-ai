package ai.moneymanager.domain.event

import ai.moneymanager.domain.model.Currency
import org.bson.types.ObjectId
import java.math.BigDecimal

data class GroupSettingChanged(
    val groupId: ObjectId,
    val actorId: Long,
    val change: GroupSettingChange,
)

sealed interface GroupSettingChange {
    data class Renamed(val oldName: String, val newName: String) : GroupSettingChange
    data class CurrencyChanged(val oldCurrency: Currency, val newCurrency: Currency) : GroupSettingChange
    data class InitialBalanceChanged(val oldAmount: BigDecimal, val newAmount: BigDecimal) : GroupSettingChange
    data class MonthStartChanged(val oldDay: Int, val newDay: Int) : GroupSettingChange
}
