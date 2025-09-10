package poker.player.kotlin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.json.JSONObject


fun Application.installRoutes() {
    val playerDecision = PlayerDecision()

    routing {
        get("/") { call.respondText("OK", ContentType.Text.Plain) }
        get("/version") { call.respondText(playerDecision.version(), ContentType.Text.Plain) }
        post("/bet") {
            val body = call.receiveText()
            try {
                val json = JSONObject(body)
                // Here we call getState so that different phases (pre-flop / flop / later) are used
                val decision = makeBet(parseGameState(json))
                call.respondText(decision.toString(), ContentType.Text.Plain)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "0")
            }
        }
        post("/showdown") {
            call.respondText("OK", ContentType.Text.Plain)
        }
    }
}


fun main() {
    val playerDecision = PlayerDecision()
    embeddedServer(Netty, getPort()) {
        routing {
            // Basic health endpoint
            get("/") {
                call.respondText("OK", ContentType.Text.Plain)
            }

            // LeanPoker REST API endpoints
            get("/version") {
                call.respondText(playerDecision.version(), ContentType.Text.Plain)
            }

            post("/bet") {
                val body = call.receiveText()
                try {
                    val json = JSONObject(body)
                    val bet = playerDecision.betRequest(json)
                    // LeanPoker expects plain number text
                    call.respondText(bet.toString(), ContentType.Text.Plain)
//                    val bet = playerDecision.betRequest(json)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "0")
                }
            }

            post("/showdown") {
                // Consume the body for compatibility, even if Player.showdown ignores it
                val body = call.receiveText()
                try {
                    // Validate JSON format if provided
                    if (body.isNotBlank()) {
                        JSONObject(body)
                    }
                    playerDecision.showdown()
                    call.respondText("OK", ContentType.Text.Plain)
                } catch (e: Exception) {
                    // Invalid JSON, but still respond OK to not break tournament flow
                    playerDecision.showdown()
                    call.respondText("OK", ContentType.Text.Plain)
                }
            }

            // Backward-compatible action-based endpoint used by older runners
            post {
                val formParameters = call.receiveParameters()
                val result = when (val action = formParameters["action"].toString()) {
                    "bet_request" -> {
                        val gameState = formParameters["game_state"]
                        if (gameState == null) {
                            "Missing game_state!"
                        } else {
                            val json = JSONObject(gameState)
                            playerDecision.betRequest(json).toString()
                        }
                    }

                    "showdown" -> {
                        playerDecision.showdown()
                        "OK"
                    }

                    "version" -> playerDecision.version()
                    else -> "Unknown action '$action'!"
                }

                call.respondText(result)
            }
        }
    }.start(wait = true)
}

private fun getPort(): Int {
    val port = System.getenv("PORT") ?: "8080"
    return Integer.parseInt(port)
}
