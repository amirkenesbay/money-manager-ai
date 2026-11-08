package ai.moneymanager.metrics

import ai.moneymanager.domain.event.BotMembershipChanged
import ai.moneymanager.domain.event.NewUserRegistered
import ai.moneymanager.domain.event.SubscriptionUpdated
import ai.moneymanager.domain.model.SubscriptionChange
import ai.moneymanager.domain.model.SubscriptionPeriod
import ai.moneymanager.domain.model.SubscriptionTier
import ai.moneymanager.domain.model.UserAccess
import ai.moneymanager.repository.entity.FinanceOperationEntity
import ai.moneymanager.repository.entity.NotificationEntity
import ai.moneymanager.repository.entity.UserInfoEntity
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private const val REFRESH_INTERVAL_MS = 300_000L
private const val PREFIX = "moneymanager."
private const val WINDOW_TAG = "window"
private const val SOURCE_TAG = "source"
private const val CHANGE_TAG = "change"
private const val PERIOD_TAG = "period"
private const val CURRENCY_TAG = "currency"
private const val NO_PERIOD = "none"
private const val CREATED_AT = "auditInfo.createdAt"
private const val USERS_REGISTERED = "users.registered"
private const val BOT_MEMBERSHIP = "bot.membership"
private const val BOT_BLOCKED = "users.bot_blocked"
private const val ACTIVATED_TAG = "activated"
private const val BOT_BLOCKED_AT = "botBlockedAt"
private const val TELEGRAM_USER_ID = "telegramUserId"
private const val CREATOR_ID = "creatorId"
private const val REVENUE_CURRENCY = "usd"
private val PAID_PERIODS = SubscriptionPeriod.entries.map { it.name.lowercase() }

private enum class MembershipChange(val tag: String) {
    BLOCKED("blocked"),
    UNBLOCKED("unblocked");

    companion object {
        fun of(blocked: Boolean) = if (blocked) BLOCKED else UNBLOCKED
    }
}

private enum class Window(val tag: String, val days: Long) {
    DAY("day", 1),
    WEEK("week", 7),
    MONTH("month", 30),
}

private enum class OperationSource(val tag: String, val criteria: () -> Criteria) {
    RECORDED("recorded", { Criteria.where("importId").`is`(null) }),
    IMPORTED("imported", { Criteria.where("importId").ne(null) }),
}

@Component
class BusinessMetrics(
    private val mongoTemplate: MongoTemplate,
    private val registry: MeterRegistry,
) {
    private val values = ConcurrentHashMap<String, AtomicLong>()

    init {
        registry.counter(PREFIX + USERS_REGISTERED)
        MembershipChange.entries.forEach { botMembership(it) }
        SubscriptionChange.entries.forEach { change -> PAID_PERIODS.forEach { period -> subscriptionEvents(change.name.lowercase(), period) } }
        PAID_PERIODS.forEach { period -> revenue(REVENUE_CURRENCY, period) }
    }

    private fun gauge(name: String, vararg tags: String): AtomicLong =
        values.computeIfAbsent(name + tags.joinToString()) {
            AtomicLong().also { value -> Gauge.builder(PREFIX + name, value) { it.get().toDouble() }.tags(*tags).register(registry) }
        }

    @Scheduled(fixedRate = REFRESH_INTERVAL_MS)
    fun refresh() {
        val now = LocalDateTime.now()
        val today = LocalDate.now().atStartOfDay()
        gauge("users.count").set(count<UserInfoEntity>(Criteria.where(TELEGRAM_USER_ID).ne(null)))
        gauge("users.new").set(count<UserInfoEntity>(Criteria.where(CREATED_AT).gte(today)))
        gauge("users.premium").set(count<UserInfoEntity>(Criteria.where("subscriptionTier").`is`(SubscriptionTier.PAID).and("subscriptionExpiresAt").gt(now)))
        UserAccess.entries.filter { it != UserAccess.REGULAR }.forEach { access ->
            gauge("users.access", "access", access.name.lowercase()).set(count<UserInfoEntity>(Criteria.where("access").`is`(access)))
        }
        Window.entries.forEach { window ->
            val since = now.minusDays(window.days)
            val active = mongoTemplate.findDistinct(Query(Criteria.where(CREATED_AT).gte(since)), CREATOR_ID, FinanceOperationEntity::class.java, Long::class.javaObjectType)
            gauge("users.active", WINDOW_TAG, window.tag).set(active.size.toLong())
        }
        OperationSource.entries.forEach { source ->
            gauge("operations.today", SOURCE_TAG, source.tag).set(count<FinanceOperationEntity>(Criteria().andOperator(Criteria.where(CREATED_AT).gte(today), source.criteria())))
        }
        gauge("reminders.active").set(count<NotificationEntity>(Criteria.where("isActive").`is`(true)))
        refreshBotBlocked()
    }

    private fun refreshBotBlocked() {
        val blocked = mongoTemplate.findDistinct(Query(Criteria.where(BOT_BLOCKED_AT).ne(null)), TELEGRAM_USER_ID, UserInfoEntity::class.java, Long::class.javaObjectType)
        val activated = mongoTemplate.findDistinct(Query(Criteria.where(CREATOR_ID).`in`(blocked)), CREATOR_ID, FinanceOperationEntity::class.java, Long::class.javaObjectType).size.toLong()
        gauge(BOT_BLOCKED, ACTIVATED_TAG, true.toString()).set(activated)
        gauge(BOT_BLOCKED, ACTIVATED_TAG, false.toString()).set(blocked.size - activated)
    }

    @EventListener
    fun onNewUser(event: NewUserRegistered) {
        registry.counter(PREFIX + USERS_REGISTERED).increment()
    }

    @EventListener
    fun onBotMembership(event: BotMembershipChanged) {
        botMembership(MembershipChange.of(event.blocked)).increment()
    }

    @EventListener
    fun onSubscription(event: SubscriptionUpdated) {
        val subscription = event.subscription
        val period = subscription.period?.name?.lowercase() ?: NO_PERIOD
        subscriptionEvents(subscription.change.name.lowercase(), period).increment()
        if (subscription.change != SubscriptionChange.CANCELLED) {
            revenue(subscription.currency.lowercase(), period).increment(subscription.price.toDouble())
        }
    }

    private fun botMembership(change: MembershipChange) = registry.counter(PREFIX + BOT_MEMBERSHIP, CHANGE_TAG, change.tag)

    private fun subscriptionEvents(change: String, period: String) =
        registry.counter(PREFIX + "subscriptions.events", CHANGE_TAG, change, PERIOD_TAG, period)

    private fun revenue(currency: String, period: String) =
        registry.counter(PREFIX + "subscriptions.revenue", CURRENCY_TAG, currency, PERIOD_TAG, period)

    private inline fun <reified T> count(criteria: Criteria): Long = mongoTemplate.count(Query(criteria), T::class.java)
}
