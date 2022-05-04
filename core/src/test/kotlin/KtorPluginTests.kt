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

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.noelware.ktor.NoelKtorRoutingPlugin
import owo.da.uwu.*

class KtorPluginTests: DescribeSpec({
    describe("org.noelware.ktor.tests.KtorPluginTests") {
        it("should throw a error if `routing` plugin was not initialized") {
            val exception = shouldThrow<IllegalStateException> {
                testApplication {
                    install(NoelKtorRoutingPlugin)
                }
            }

            exception.message shouldBe "Missing `routing {}` plugin in application module"
        }

        it("should not throw an error if `routing` plugin WAS initialized") {
            shouldNotThrow<IllegalStateException> {
                testApplication {
                    routing {}
                    install(NoelKtorRoutingPlugin)
                }
            }
        }

        it("should be able to run `GET /`") {
            testApplication {
                routing {}
                install(NoelKtorRoutingPlugin) {
                    endpoints(OtherEndpoint())
                }

                val client = createClient {}
                val res = client.get("/")

                res.body<String>() shouldBe "hewo world"
                res.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
