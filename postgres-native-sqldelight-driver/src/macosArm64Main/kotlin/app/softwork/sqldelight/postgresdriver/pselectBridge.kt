package app.softwork.sqldelight.postgresdriver

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

import kotlinx.cinterop.*
import platform.posix.*

// COPIED FROM KTOR
// License Apache-2.0
// Changes: Make all Ktor internal classes private
// Reason: https://youtrack.jetbrains.com/issue/KTOR-5035/Remove-check-for-internal-class-in-Select

internal actual fun pselectBridge(
    descriptor: Int,
    readSet: CPointer<fd_set>,
    writeSet: CPointer<fd_set>,
    errorSet: CPointer<fd_set>
): Int = pselect(descriptor, readSet, writeSet, errorSet, null, null)
