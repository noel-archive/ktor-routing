@file:Suppress("UNUSED")
package owo.da.uwu

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.noelware.ktor.AbstractEndpoint
import org.noelware.ktor.annotations.Get

class OtherEndpoint: AbstractEndpoint() {
    @Get
    suspend fun main(call: ApplicationCall) {
        call.respond(HttpStatusCode.OK, "hewo world")
    }
}
