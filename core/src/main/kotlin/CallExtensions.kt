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

@file:JvmName("NoelKtorApplicationCallExtensionsKt")

package org.noelware.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadOnlyProperty

/**
 * [ReadOnlyProperty] to transform the request body to [T].
 * @return this [ReadOnlyProperty] to get the body.
 */
@Deprecated(
    "Please use the `receive` method instead. You will need to replace the ReadOnlyProperty reference with a variable reference.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.receive()", "io.ktor.server.request.receive")
)
public inline fun <reified T: Any> ApplicationCall.body(): ReadOnlyProperty<Any?, T> = ReadOnlyProperty { _, _ ->
    runBlocking {
        this@body.receive()
    }
}

/**
 * Returns the true, real IP if the application is running behind the proxy. If not, it'll
 * return the origin host from [request.origin][io.ktor.server.request.ApplicationRequest.origin]
 */
public val ApplicationCall.realIP: String
    get() {
        val headers = request.headers
        return if (headers.contains("True-Client-IP")) {
            headers["True-Client-IP"]!!
        } else if (headers.contains("X-Real-IP")) {
            headers["X-Real-IP"]!!
        } else if (headers.contains(HttpHeaders.XForwardedFor)) {
            var index = headers[HttpHeaders.XForwardedFor]!!.indexOf(", ")
            if (index == -1) {
                index = headers[HttpHeaders.XForwardedFor]!!.length
            }

            if (index == headers[HttpHeaders.XForwardedFor]!!.length) {
                headers[HttpHeaders.XForwardedFor]!!
            } else {
                headers[HttpHeaders.XForwardedFor]!!.slice(0..index)
            }
        } else {
            request.origin.remoteHost
        }
    }
