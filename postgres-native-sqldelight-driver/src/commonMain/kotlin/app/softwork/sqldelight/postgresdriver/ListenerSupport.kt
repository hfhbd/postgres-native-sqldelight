package app.softwork.sqldelight.postgresdriver

import io.ktor.network.selector.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import libpq.*
import kotlin.time.*

public sealed interface ListenerSupport {
    public sealed interface ScopedListenerSupport : ListenerSupport {
        public val notificationScope: CoroutineScope
    }

    public companion object {
        public fun Local(notificationScope: CoroutineScope): Local {
            val notifications = MutableSharedFlow<String>()
            return Local(notificationScope, notifications) { notifications.emit(it) }
        }
    }

    public class Local(
        override val notificationScope: CoroutineScope,
        public val notifications: Flow<String>,
        public val notify: suspend (String) -> Unit
    ) : ScopedListenerSupport

    public class Remote(
        override val notificationScope: CoroutineScope,
        public val notificationName: (String) -> String = { it }
    ) : ScopedListenerSupport {
        internal fun remoteListener(conn: CPointer<PGconn>): Flow<String> = channelFlow {
            val selector = SelectorManager()

            try {
                val socket = PQsocket(conn)
                check(socket >= 0) {
                    "Error while connecting to the PostgreSql socket"
                }
                val selectable = object: Selectable {
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
