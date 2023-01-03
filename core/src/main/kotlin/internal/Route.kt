/*
 * 📭 ktor-routing: Extensions to Ktor’s routing system to add object-oriented routing and much more.
 * Copyright (c) 2022-2023 Noelware <team@noelware.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.noelware.ktor.internal

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import org.noelware.ktor.endpoints.AbstractEndpoint
import kotlin.reflect.KCallable
import kotlin.reflect.full.callSuspend
import io.ktor.server.routing.Route as KtorRoute

/**
 * Represents a basic route that is registered in a [AbstractEndpoint].
 */
internal class Route(
    val path: String,
    val methods: List<HttpMethod> = listOf(HttpMethod.Get),
    val isWebSocketRoute: Boolean = false,
    private val runner: KCallable<*>,
    private val parent: AbstractEndpoint
) {
    val plugins: MutableList<Pair<Plugin<KtorRoute, *, *>, Any.() -> Unit>> = mutableListOf()

    @Suppress("UNCHECKED_CAST")
    fun <C: Any, B: Any> install(plugin: Plugin<io.ktor.server.routing.Route, C, B>, configure: C.() -> Unit = {}) {
        val plu = Pair(
            plugin,
            configure as Any.() -> Unit
        )

        if (!plugins.contains(plu)) {
            plugins.add(plu)
        }
    }

    suspend fun run(call: ApplicationCall): Any? =
        if (runner.isSuspend) runner.callSuspend(parent, call) else runner.call(parent, call)

    suspend fun websocketCall(session: DefaultWebSocketServerSession): Any? =
        if (runner.isSuspend) runner.callSuspend(parent, session) else runner.call(parent, session)
}
