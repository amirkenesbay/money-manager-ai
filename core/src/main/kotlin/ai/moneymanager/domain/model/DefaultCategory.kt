package ai.moneymanager.domain.model

const val ORIGINAL_DEFAULTS_VERSION = 1
const val CURRENT_DEFAULTS_VERSION = 2

enum class DefaultCategory(private val key: String, val icon: String, val type: CategoryType, val since: Int, val isOther: Boolean = false) {
    GROCERIES("groceries", "🛒", CategoryType.EXPENSE, ORIGINAL_DEFAULTS_VERSION),
    CAFES("cafes", "🍽️", CategoryType.EXPENSE, CURRENT_DEFAULTS_VERSION),
    TRANSPORT("transport", "🚗", CategoryType.EXPENSE, ORIGINAL_DEFAULTS_VERSION),
    HOME("home", "🏠", CategoryType.EXPENSE, CURRENT_DEFAULTS_VERSION),
    CONNECTION("connection", "📱", CategoryType.EXPENSE, CURRENT_DEFAULTS_VERSION),
    SUBSCRIPTIONS("subscriptions", "📺", CategoryType.EXPENSE, CURRENT_DEFAULTS_VERSION),
    HEALTH("health", "💊", CategoryType.EXPENSE, ORIGINAL_DEFAULTS_VERSION),
    BEAUTY("beauty", "💇", CategoryType.EXPENSE, CURRENT_DEFAULTS_VERSION),
    ENTERTAINMENT("entertainment", "🎬", CategoryType.EXPENSE, ORIGINAL_DEFAULTS_VERSION),
    CLOTHES("clothes", "👕", CategoryType.EXPENSE, ORIGINAL_DEFAULTS_VERSION),
    EDUCATION("education", "📚", CategoryType.EXPENSE, ORIGINAL_DEFAULTS_VERSION),
    OTHER_EXPENSE("other", "📦", CategoryType.EXPENSE, ORIGINAL_DEFAULTS_VERSION, isOther = true),
    SALARY("salary", "💰", CategoryType.INCOME, ORIGINAL_DEFAULTS_VERSION),
    SIDE_INCOME("side", "💼", CategoryType.INCOME, CURRENT_DEFAULTS_VERSION),
    CASHBACK("cashback", "💳", CategoryType.INCOME, CURRENT_DEFAULTS_VERSION),
    INVESTMENTS("investments", "📈", CategoryType.INCOME, CURRENT_DEFAULTS_VERSION),
    GIFTS("gifts", "🎁", CategoryType.INCOME, ORIGINAL_DEFAULTS_VERSION),
    OTHER_INCOME("other", "📦", CategoryType.INCOME, ORIGINAL_DEFAULTS_VERSION, isOther = true);

    val nameKey: String get() = "category.default.${type.name.lowercase()}.$key.name"

    companion object {
        fun of(seedKey: String?): DefaultCategory? = entries.firstOrNull { it.name == seedKey }
    }
}
