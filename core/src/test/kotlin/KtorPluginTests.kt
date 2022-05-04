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
