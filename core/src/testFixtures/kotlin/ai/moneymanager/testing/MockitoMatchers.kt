package ai.moneymanager.testing

import org.mockito.Mockito

@Suppress("UNCHECKED_CAST")
fun <T> anyOf(type: Class<T>): T {
    Mockito.any(type)
    return null as T
}
