/*
 * Copyright (C) 2022 JetBrains s.r.o and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.softwork.sqldelight.postgresdriver

import io.ktor.network.interop.*
import io.ktor.network.selector.*
import io.ktor.network.util.*
import io.ktor.util.*
import io.ktor.util.collections.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.*
import kotlinx.atomicfu.*
import kotlinx.atomicfu.locks.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.coroutines.*
import kotlin.math.*

// COPIED FROM KTOR
// License Apache-2.0
// Changes: Make all Ktor internal classes private
// Reason: https://youtrack.jetbrains.com/issue/KTOR-5035/Remove-check-for-internal-class-in-Select

internal fun SelectorManager(): SelectorManager = WorkerSelectorManager()

@OptIn(ExperimentalCoroutinesApi::class)
private class WorkerSelectorManager : SelectorManager {
    private val selectorContext = newSingleThreadContext("WorkerSelectorManager")
    private val job = Job()
    override val coroutineContext: CoroutineContext = selectorContext + job

    private val selector = SelectorHelper()

    init {
        selector.start(this)
    }

    override fun notifyClosed(selectable: Selectable) {
        selector.notifyClosed(selectable.descriptor)
    }

    override suspend fun select(
        selectable: Selectable,
        interest: SelectInterest
    ) {
        return suspendCancellableCoroutine { continuation ->
            val selectorState = EventInfo(selectable.descriptor, interest, continuation)
            if (!selector.interest(selectorState)) {
                continuation.resumeWithException(CancellationException("Selector closed."))
            }
        }
    }

    override fun close() {
        selector.requestTermination()
        selectorContext.close()
    }
}


@OptIn(InternalAPI::class)
private class SelectorHelper {
    private val wakeupSignal = SignalPoint()
    private val interestQueue = LockFreeMPSCQueue<EventInfo>()
    private val closeQueue = LockFreeMPSCQueue<Int>()

    private val wakeupSignalEvent = EventInfo(
        wakeupSignal.selectionDescriptor,
        SelectInterest.READ,
        Continuation(EmptyCoroutineContext) {
        }
    )

    fun interest(event: EventInfo): Boolean {
        if (interestQueue.addLast(event)) {
            wakeupSignal.signal()
            return true
        }

        return false
    }

    fun start(scope: CoroutineScope) {
        scope.launch(CoroutineName("selector")) {
            selectionLoop()
        }.invokeOnCompletion {
            cleanup()
        }
    }

    fun requestTermination() {
        interestQueue.close()
        wakeupSignal.signal()
    }

    private fun cleanup() {
        wakeupSignal.close()
    }

    fun notifyClosed(descriptor: Int) {
        closeQueue.addLast(descriptor)
        wakeupSignal.signal()
    }

    private fun selectionLoop(): Unit = memScoped {
        val readSet = alloc<fd_set>()
        val writeSet = alloc<fd_set>()
        val errorSet = alloc<fd_set>()

        val completed = mutableSetOf<EventInfo>()
        val watchSet = mutableSetOf<EventInfo>()
        val closeSet = mutableSetOf<Int>()

        while (!interestQueue.isClosed) {
            watchSet.add(wakeupSignalEvent)
            var maxDescriptor = fillHandlers(watchSet, readSet, writeSet, errorSet)
            if (maxDescriptor == 0) continue

            maxDescriptor = max(maxDescriptor + 1, wakeupSignalEvent.descriptor + 1)

            try {
                pselectBridge(maxDescriptor + 1, readSet.ptr, writeSet.ptr, errorSet.ptr).check()
            } catch (_: PosixException.BadFileDescriptorException) {
                // Thrown if the descriptor was closed.
            }

            processSelectedEvents(watchSet, closeSet, completed, readSet, writeSet, errorSet)
        }

        val exception = CancellationException("Selector closed")
        while (!interestQueue.isEmpty) {
            interestQueue.removeFirstOrNull()?.fail(exception)
        }

        for (item in watchSet) {
            item.fail(exception)
        }
    }

    private fun fillHandlers(
        watchSet: MutableSet<EventInfo>,
        readSet: fd_set,
        writeSet: fd_set,
        errorSet: fd_set
    ): Int {
        var maxDescriptor = 0

        select_fd_clear(readSet.ptr)
        select_fd_clear(writeSet.ptr)
        select_fd_clear(errorSet.ptr)

        while (true) {
            val event = interestQueue.removeFirstOrNull() ?: break
            watchSet.add(event)
        }

        for (event in watchSet) {
            addInterest(event, readSet, writeSet, errorSet)
            maxDescriptor = max(maxDescriptor, event.descriptor)
        }

        return maxDescriptor
    }

    private fun addInterest(
        event: EventInfo,
        readSet: fd_set,
        writeSet: fd_set,
        errorSet: fd_set
    ) {
        val set = descriptorSetByInterestKind(event, readSet, writeSet)

        select_fd_add(event.descriptor, set.ptr)
        select_fd_add(event.descriptor, errorSet.ptr)

        check(select_fd_isset(event.descriptor, set.ptr) != 0)
        check(select_fd_isset(event.descriptor, errorSet.ptr) != 0)
    }

    private fun processSelectedEvents(
        watchSet: MutableSet<EventInfo>,
        closeSet: MutableSet<Int>,
        completed: MutableSet<EventInfo>,
        readSet: fd_set,
        writeSet: fd_set,
        errorSet: fd_set
    ) {
        while (true) {
            val event = closeQueue.removeFirstOrNull() ?: break
            closeSet.add(event)
        }

        for (event in watchSet) {
            if (event.descriptor in closeSet) {
                completed.add(event)
                continue
            }

            val set = descriptorSetByInterestKind(event, readSet, writeSet)

            if (select_fd_isset(event.descriptor, errorSet.ptr) != 0) {
                completed.add(event)
                event.fail(IOException("Fail to select descriptor ${event.descriptor} for ${event.interest}"))
                continue
            }

            if (select_fd_isset(event.descriptor, set.ptr) == 0) continue

            if (event.descriptor == wakeupSignal.selectionDescriptor) {
                wakeupSignal.check()
                continue
            }

            completed.add(event)
            event.complete()
        }

        for (descriptor in closeSet) {
            close(descriptor)
        }
        closeSet.clear()

        watchSet.removeAll(completed)
        completed.clear()
    }

    private fun descriptorSetByInterestKind(
        event: EventInfo,
        readSet: fd_set,
        writeSet: fd_set
    ): fd_set = when (event.interest) {
        SelectInterest.READ -> readSet
        SelectInterest.WRITE -> writeSet
        SelectInterest.ACCEPT -> readSet
        else -> error("Unsupported interest ${event.interest}.")
    }
}

private data class EventInfo(
    val descriptor: Int,
    val interest: SelectInterest,
    private val continuation: Continuation<Unit>
) {

    fun complete() {
        continuation.resume(Unit)
    }

    fun fail(cause: Throwable) {
        continuation.resumeWithException(cause)
    }

    override fun toString(): String = "EventInfo[$descriptor, $interest]"
}

private class SignalPoint : Closeable {
    private val readDescriptor: Int
    private val writeDescriptor: Int
    private var remaining: Int by atomic(0)
    private val lock = SynchronizedObject()
    private var closed = false

    val selectionDescriptor: Int
        get() = readDescriptor

    init {
        val (read, write) = memScoped {
            val pipeDescriptors = allocArray<IntVar>(2)
            pipe(pipeDescriptors).check()

            repeat(2) { index ->
                makeNonBlocking(pipeDescriptors[index])
            }

            Pair(pipeDescriptors[0], pipeDescriptors[1])
        }

        readDescriptor = read
        writeDescriptor = write
    }

    fun check() {
        synchronized(lock) {
            if (closed) return@synchronized
            while (remaining > 0) {
                remaining -= readFromPipe()
            }
        }
    }

    @OptIn(UnsafeNumber::class)
    fun signal() {
        synchronized(lock) {
            if (closed) return@synchronized

            if (remaining > 0) return

            memScoped {
                val array = allocArray<ByteVar>(1)
                array[0] = 7
                // note: here we ignore the result of write intentionally
                // we simply don't care whether the buffer is full or the pipe is already closed
                val result = write(writeDescriptor, array, 1.convert())
                if (result < 0) return

                remaining += result.toInt()
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return@synchronized
            closed = true

            close(writeDescriptor)
            readFromPipe()
            close(readDescriptor)
        }
    }

    @OptIn(UnsafeNumber::class)
    private fun readFromPipe(): Int {
        var count = 0

        memScoped {
            val buffer = allocArray<ByteVar>(1024)

            do {
                val result = read(readDescriptor, buffer, 1024.convert()).convert<Int>()
                if (result < 0) {
                    when (val error = PosixException.forErrno()) {
                        is PosixException.TryAgainException -> {}
                        else -> throw error
                    }

                    break
                }

                if (result == 0) {
                    break
                }

                count += result
            } while (true)
        }

        return count
    }

    private fun makeNonBlocking(descriptor: Int) {
        fcntl(descriptor, F_SETFL, fcntl(descriptor, F_GETFL) or O_NONBLOCK).check()
    }
}

private inline fun Int.check(
    block: (Int) -> Boolean = { it >= 0 }
): Int {
    if (!block(this)) {
        throw PosixException.forErrno()
    }

    return this
}

internal expect fun pselectBridge(
    descriptor: Int,
    readSet: CPointer<fd_set>,
    writeSet: CPointer<fd_set>,
    errorSet: CPointer<fd_set>
): Int
