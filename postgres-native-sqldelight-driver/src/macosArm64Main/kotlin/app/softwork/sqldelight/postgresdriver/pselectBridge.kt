package app.softwork.sqldelight.postgresdriver

import kotlinx.cinterop.*
import platform.posix.*

internal actual fun pselectBridge(
    descriptor: Int,
    readSet: CPointer<fd_set>,
    writeSet: CPointer<fd_set>,
    errorSet: CPointer<fd_set>
): Int = pselect(descriptor, readSet, writeSet, errorSet, null, null)
