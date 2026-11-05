package ai.moneymanager.testing

import ai.moneymanager.service.SubscriptionLimitsService
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

const val TEST_FREE_HISTORY_DAYS = 30
private const val FREE_AI_REQUESTS = 10
private const val PAID_AI_REQUESTS = 100
private const val FREE_CATEGORIES = 10
private const val PAID_CATEGORIES = 1000
private const val FREE_ITEMS = 3

fun testSubscriptionLimits(today: LocalDate): SubscriptionLimitsService = SubscriptionLimitsService(
    freeMaxAiRequestsPerDay = FREE_AI_REQUESTS,
    paidMaxAiRequestsPerDay = PAID_AI_REQUESTS,
    freeMaxCategoriesPerType = FREE_CATEGORIES,
    paidMaxCategoriesPerType = PAID_CATEGORIES,
    freeMaxHistoryDaysBack = TEST_FREE_HISTORY_DAYS,
    freeMaxOwnedSharedGroups = FREE_ITEMS,
    freeMaxActiveNotifications = FREE_ITEMS,
    freeMaxBudgets = FREE_ITEMS,
    freeMaxRecurringPayments = FREE_ITEMS,
    freeMaxImports = FREE_ITEMS,
    clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC),
)
