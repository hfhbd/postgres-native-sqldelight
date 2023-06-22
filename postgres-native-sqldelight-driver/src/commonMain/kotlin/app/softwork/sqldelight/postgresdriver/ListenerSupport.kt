package app.softwork.sqldelight.postgresdriver

import io.ktor.network.selector.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

public sealed interface ListenerSupport {

    public companion object {
        public fun Local(notificationScope: CoroutineScope): Local {
            val notifications = MutableSharedFlow<String>()
            return Local(notificationScope, notifications) { notifications.emit(it) }
        }
    }

    public class Local(
        notificationScope: CoroutineScope,
        internal val notifications: Flow<String>,
        internal val notify: suspend (String) -> Unit
    ) : ScopedListenerSupport {
        override val notificationScope: CoroutineScope = notificationScope + Job()
    }

    public class Remote(
        notificationScope: CoroutineScope,
        internal val notificationName: (String) -> String = { it }
    ) : ScopedListenerSupport {
        override val notificationScope: CoroutineScope = notificationScope + Job()

        internal fun remoteListener(conn: PGConnection): Flow<String> = channelFlow {
            
        }.shareIn(notificationScope, SharingStarted.WhileSubscribed(replayExpiration = Duration.ZERO))
    }

    public object None : ListenerSupport
}
