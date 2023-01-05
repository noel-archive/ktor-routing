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

@file:Suppress("UNUSED")

package org.noelware.ktor.testing

import dev.floofy.utils.slf4j.logging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.endpoints.Get
import org.noelware.ktor.endpoints.Post
import org.noelware.ktor.plugin.NoelKtorRouting
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasicUsageTests {
    private val log by logging<BasicUsageTests>()

    @DisplayName("If routing works with multiple prefixes (/, /api)")
    @Test
    fun test0(): Unit = testApplication {
        install(NoelKtorRouting) {
            endpoints(object: AbstractEndpoint("/", "/api") {
                @Get
                suspend fun main(call: ApplicationCall): Unit = call.respond("Hello, world!")
            })
        }

        for (route in listOf("/", "/api")) {
            val res = client.get(route)

            assertEquals(200, res.status.value)
            assertFalse(res.status.value == 400)

            val body = res.bodyAsText()
            assertEquals("Hello, world!", body)
        }

        val res = client.get("/wuff")
        assertEquals(404, res.status.value)
        assertFalse(res.status.value == 200)
    }

    @DisplayName("basePrefix and includeTrailingSlash testing")
    @Test
    fun test2(): Unit = testApplication {
        var wasLocalScopedCalled = false

        install(NoelKtorRouting) {
            includeTrailingSlash = true
            basePrefix = "/api"
            endpoints(
                object: AbstractEndpoint() {
                    init {
                        val globalScopedPlugin = createRouteScopedPlugin("owo da uwu") {
                            onCall {
                                log.debug("global!")
                            }
                        }

                        val localScopedPlugin = createRouteScopedPlugin("uwu da owo") {
                            onCall {
                                wasLocalScopedCalled = true
                                log.debug("local")
                            }
                        }

                        install("/uwu", localScopedPlugin)
                        install(globalScopedPlugin)
                    }

                    @Get
                    suspend fun main(call: ApplicationCall) {
                        call.respond(HttpStatusCode.OK, "hello world!")
                    }

                    @Post("/uwu")
                    suspend fun uwu(call: ApplicationCall) {
                        val body: String = call.receive()
                        call.respond(HttpStatusCode.OK, body)
                    }
                },
            )
        }

        val res1 = client.get("/api")
        assertEquals(200, res1.status.value)
        assertEquals("hello world!", res1.bodyAsText())

        val res2 = client.post("/api/uwu") {
            setBody(">w<")
        }

        assertTrue(wasLocalScopedCalled)
        assertEquals(200, res2.status.value)
        assertEquals(">w<", res2.bodyAsText())

        val res3 = client.get("/api/")
        assertEquals(200, res3.status.value)
        assertEquals("hello world!", res3.bodyAsText())
    }
}
