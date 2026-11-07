package ai.moneymanager.metrics

import ai.moneymanager.domain.event.NewUserRegistered
import ai.moneymanager.domain.event.SubscriptionUpdated
import ai.moneymanager.domain.model.SubscriptionChange
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

    private fun gauge(name: String, vararg tags: String): AtomicLong =
        values.computeIfAbsent(name + tags.joinToString()) {
            AtomicLong().also { value -> Gauge.builder(PREFIX + name, value) { it.get().toDouble() }.tags(*tags).register(registry) }
        }

    @Scheduled(fixedRate = REFRESH_INTERVAL_MS)
    fun refresh() {
        val now = LocalDateTime.now()
        val today = LocalDate.now().atStartOfDay()
        gauge("users.count").set(count<UserInfoEntity>(Criteria.where("telegramUserId").ne(null)))
        gauge("users.new").set(count<UserInfoEntity>(Criteria.where(CREATED_AT).gte(today)))
        gauge("users.premium").set(count<UserInfoEntity>(Criteria.where("subscriptionTier").`is`(SubscriptionTier.PAID).and("subscriptionExpiresAt").gt(now)))
        UserAccess.entries.filter { it != UserAccess.REGULAR }.forEach { access ->
            gauge("users.access", "access", access.name.lowercase()).set(count<UserInfoEntity>(Criteria.where("access").`is`(access)))
        }
        Window.entries.forEach { window ->
            val since = now.minusDays(window.days)
            val active = mongoTemplate.findDistinct(Query(Criteria.where(CREATED_AT).gte(since)), "creatorId", FinanceOperationEntity::class.java, Long::class.javaObjectType)
            gauge("users.active", WINDOW_TAG, window.tag).set(active.size.toLong())
        }
        OperationSource.entries.forEach { source ->
            gauge("operations.today", SOURCE_TAG, source.tag).set(count<FinanceOperationEntity>(Criteria().andOperator(Criteria.where(CREATED_AT).gte(today), source.criteria())))
        }
        gauge("reminders.active").set(count<NotificationEntity>(Criteria.where("isActive").`is`(true)))
    }

    @EventListener
    fun onNewUser(event: NewUserRegistered) {
        registry.counter(PREFIX + "users.registered").increment()
    }

    @EventListener
    fun onSubscription(event: SubscriptionUpdated) {
        val subscription = event.subscription
        val period = subscription.period?.name?.lowercase() ?: NO_PERIOD
        registry.counter(PREFIX + "subscriptions.events", CHANGE_TAG, subscription.change.name.lowercase(), PERIOD_TAG, period).increment()
        if (subscription.change != SubscriptionChange.CANCELLED) {
            registry.counter(PREFIX + "subscriptions.revenue", CURRENCY_TAG, subscription.currency.lowercase(), PERIOD_TAG, period).increment(subscription.price.toDouble())
        }
    }

    private inline fun <reified T> count(criteria: Criteria): Long = mongoTemplate.count(Query(criteria), T::class.java)
}
