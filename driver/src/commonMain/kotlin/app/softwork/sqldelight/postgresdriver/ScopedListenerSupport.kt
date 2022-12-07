package app.softwork.sqldelight.postgresdriver

import kotlinx.coroutines.*

internal sealed interface ScopedListenerSupport : ListenerSupport {
    val notificationScope: CoroutineScope
}
