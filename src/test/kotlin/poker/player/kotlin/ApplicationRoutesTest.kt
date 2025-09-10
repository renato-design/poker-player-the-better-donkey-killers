package poker.player.kotlin

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplicationRoutesTest {

    // Minimal wiring that mirrors your routing in Application.kt
    private fun Application.installRoutes() {
        val playerDecision = PlayerDecision()
        routing {
            get("/") { call.respondText("OK", ContentType.Text.Plain) }
            get("/version") { call.respondText(playerDecision.version(), ContentType.Text.Plain) }
            post("/bet") {
                val body = call.receiveText()
                try {
                    val json = JSONObject(body)
                    val bet = playerDecision.betRequest(json)
                    call.respondText(bet.toString(), ContentType.Text.Plain)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "0")
                }
            }
            post("/showdown") {
                val body = call.receiveText()
                try {
                    if (body.isNotBlank()) {
                        JSONObject(body)
                    }
                    playerDecision.showdown()
                    call.respondText("OK", ContentType.Text.Plain)
                } catch (e: Exception) {
                    playerDecision.showdown()
                    call.respondText("OK", ContentType.Text.Plain)
                }
            }
            post {
                val params = call.receiveParameters()
                val action = params["action"]
                val result = when (action) {
                    "bet_request" -> {
                        val gameState = params["game_state"]
                        if (gameState == null) {
                            "Missing game_state!"
                        } else {
                            val json = JSONObject(gameState)
                            playerDecision.betRequest(json).toString()
                        }
                    }
                    "showdown" -> { playerDecision.showdown(); "OK" }
                    "version" -> playerDecision.version()
                    else -> "Unknown action '$action'!"
                }
                call.respondText(result)
            }
        }
    }

    @Test
    fun `GET version returns donkeykilla`() = testApplication {
        application { installRoutes() }
        val res = client.get("/version")
        assertEquals("donkeykilla", res.bodyAsText())
    }

    @Test
    fun `POST bet processes JSON and returns bet decision`() = testApplication {
        application { installRoutes() }
        
        // Build a minimal valid game state
        val gameState = JSONObject("""
            {"players":[{"id":0,"name":"me","status":"active","version":"v","stack":1000,"bet":0}],
             "tournament_id":"T","game_id":"G","round":0,"bet_index":0,"small_blind":10,
             "current_buy_in":10,"pot":0,"minimum_raise":10,"dealer":0,"orbits":0,"in_action":0,
             "community_cards":[]}
        """.trimIndent())
        
        val res = client.post("/bet") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gameState.toString())
        }
        
        // Should return a numeric bet decision
        res.bodyAsText().toInt() // This will throw if not numeric
    }

    @Test
    fun `legacy action bet_request delegates to decision engine`() = testApplication {
        application { installRoutes() }

        // Build a tiny state where calling is cheap
        val gameState = JSONObject(
            """
            {"players":[{"id":0,"name":"me","status":"active","version":"v","stack":1000,"bet":0}],
             "tournament_id":"T","game_id":"G","round":0,"bet_index":0,"small_blind":10,
             "current_buy_in":10,"pot":0,"minimum_raise":10,"dealer":0,"orbits":0,"in_action":0,
             "community_cards":[]}
            """.trimIndent()
        ).toString()

        val res = client.post("/") {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(listOf("action" to "bet_request", "game_state" to gameState).formUrlEncode())
        }

        // Depending on stubbed strength, could be 0/call/raise; here we just assert numeric format
        res.bodyAsText().toInt() // parses -> ok
    }

    @Test
    fun `POST showdown returns OK`() = testApplication {
        application { installRoutes() }
        val res = client.post("/showdown") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("{\"game_state\":{}}")
        }
        assertEquals("OK", res.bodyAsText())
    }

    @Test
    fun `POST bet with invalid JSON returns BadRequest`() = testApplication {
        application { installRoutes() }
        val res = client.post("/bet") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("invalid json")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
        assertEquals("0", res.bodyAsText())
    }

    @Test
    fun `legacy action with missing game_state returns error message`() = testApplication {
        application { installRoutes() }
        val res = client.post("/") {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(listOf("action" to "bet_request").formUrlEncode())
        }
        assertEquals("Missing game_state!", res.bodyAsText())
    }

    @Test
    fun `legacy action with unknown action returns error message`() = testApplication {
        application { installRoutes() }
        val res = client.post("/") {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(listOf("action" to "unknown_action").formUrlEncode())
        }
        assertEquals("Unknown action 'unknown_action'!", res.bodyAsText())
    }
}