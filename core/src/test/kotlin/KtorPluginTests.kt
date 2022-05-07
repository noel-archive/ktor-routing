/*
 * 📭 ktor-routing: Extensions to Ktor’s routing system to add object-oriented routing and much more.
 * Copyright (c) 2022 Noelware <team@noelware.org>
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

package org.noelware.ktor.tests

import dev.floofy.utils.slf4j.logging
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.assertions.ktor.client.shouldNotHaveStatus
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.noelware.ktor.NoelKtorRoutingPlugin
import org.noelware.ktor.body
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.endpoints.Get
import org.noelware.ktor.endpoints.Post
import owo.da.uwu.*

class KtorPluginTests: DescribeSpec({
    val log by logging<KtorPluginTests>()

    describe("org.noelware.ktor.tests.KtorPluginTests") {
        it("should throw an error if the `routing` plguin was not initialized.") {
            val exception = shouldThrow<IllegalStateException> {
                testApplication {
                    install(NoelKtorRoutingPlugin)
                }
            }

            exception.message shouldBe "Missing `routing {}` plugin in application module"
        }

        it("should not throw an error if it was initialized") {
            shouldNotThrow<IllegalStateException> {
                testApplication {
                    install(Routing)
                    install(NoelKtorRoutingPlugin)
                }
            }
        }

        it("should run `GET /`, `GET /api`, and `POST /api/uwu`") {
            testApplication {
                install(Routing)
                install(NoelKtorRoutingPlugin) {
                    endpoints(
                        OtherEndpoint(),
                        object: AbstractEndpoint("/api") {
                            init {
                                val globalScopedPlugin = createRouteScopedPlugin("owo da uwu") {
                                    onCall { log.debug("global!") }
                                }

                                val localScopedPlugin = createRouteScopedPlugin("uwu da owo") {
                                    onCall { log.debug("local") }
                                }

                                install("/api/uwu", localScopedPlugin)
                                install(globalScopedPlugin)
                            }

                            @Get
                            suspend fun main(call: ApplicationCall) {
                                call.respond(HttpStatusCode.OK, "hello world!")
                            }

                            @Post("/uwu")
                            suspend fun uwu(call: ApplicationCall) {
                                val body by call.body<String>()
                                call.respond(HttpStatusCode.OK, body)
                            }
                        }
                    )
                }

                val res1 = client.get("/")
                res1 shouldHaveStatus HttpStatusCode.OK
                res1 shouldNotHaveStatus HttpStatusCode.BadRequest
                res1.body<String>() shouldBe "hewo world"

                val res2 = client.get("/api")
                res2 shouldHaveStatus HttpStatusCode.OK
                res2 shouldNotHaveStatus HttpStatusCode.BadRequest
                res2.body<String>() shouldBe "hello world!"

                val res3 = client.post("/api/uwu") {
                    setBody("owo da uwu")
                }

                res3 shouldHaveStatus HttpStatusCode.OK
                res3 shouldNotHaveStatus HttpStatusCode.BadRequest
                res3.body<String>() shouldBe "owo da uwu"
            }
        }
    }
})
