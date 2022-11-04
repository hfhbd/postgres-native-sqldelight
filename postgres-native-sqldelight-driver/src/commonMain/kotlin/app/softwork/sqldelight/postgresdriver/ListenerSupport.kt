package app.softwork.sqldelight.postgresdriver

import io.ktor.network.selector.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import libpq.*
import kotlin.time.*

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

        internal fun remoteListener(conn: CPointer<PGconn>): Flow<String> = channelFlow {
            val selector = SelectorManager()

            try {
                val socket = PQsocket(conn)
                check(socket >= 0) {
                    "Error while connecting to the PostgreSql socket"
                }
                val selectable = object : Selectable {
                    override val descriptor: Int = socket
                }

                while (isActive) {
                    selector.select(selectable, SelectInterest.READ)
                    PQconsumeInput(conn)
                    var notification: PGnotify? = null
                    while (PQnotifies(conn)?.pointed?.also { notification = it } != null) {
                        notification?.let {
                            val tableName = it.relname!!.toKString()
                            PQfreemem(it.ptr)
                            send(tableName)
                        }
                    }
                }
            } finally {
                selector.close()
            }
        }.shareIn(notificationScope, SharingStarted.WhileSubscribed(replayExpiration = Duration.ZERO))
    }

    public object None : ListenerSupport
}
