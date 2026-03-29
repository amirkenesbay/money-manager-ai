package ai.moneymanager.domain.model.financeOperation

enum class DayType (val day: String){
    BEFORE_YESTERDAY("before_yesterday"),
    YESTERDAY("yesterday"),
    TODAY("today");

    companion object {
        fun from(day: String): DayType? =
            entries.firstOrNull { it.day.equals(day, ignoreCase = true) }
    }
}