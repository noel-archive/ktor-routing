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

package org.noelware.ktor.tests.reflections

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.assertions.ktor.client.shouldNotHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.noelware.ktor.NoelKtorRouting
import org.noelware.ktor.loader.reflections.ReflectionsEndpointLoader

class ReflectionsTest: DescribeSpec({
    describe("org.noelware.ktor.tests.reflections") {
        it("should register successfully") {
            testApplication {
                routing {}
                install(NoelKtorRouting) {
                    endpointLoader(ReflectionsEndpointLoader("com.example.myapi"))
                }

                val res = client.get("/")
                res shouldHaveStatus HttpStatusCode.OK
                res shouldNotHaveStatus HttpStatusCode.BadRequest
                res.body<String>() shouldBe "Hello, world!"
            }
        }
    }
})
