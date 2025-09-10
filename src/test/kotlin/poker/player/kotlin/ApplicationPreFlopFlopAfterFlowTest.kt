package poker.player.kotlin

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplicationPreFlopFlopAfterFlowTest {

    // Pre-flop: no community cards.
    @Test
    fun `pre-flop decision returns call bet according to pre-flop strategy`() = testApplication {
        application { installRoutes() }

        // Pre-flop sample: No community cards.
        val gameState = JSONObject(
            """
            {
              "players": [
                {"id": 0, "name": "me", "status": "active", "version": "v", "stack": 1000, "bet": 20, "hole_cards": [
                  {"rank": "A", "suit": "hearts"},
                  {"rank": "K", "suit": "spades"}
                ]}
              ],
              "tournament_id": "T",
              "game_id": "G",
              "round": 0,
              "bet_index": 0,
              "small_blind": 10,
              "current_buy_in": 100,
              "pot": 50,
              "minimum_raise": 20,
              "dealer": 0,
              "orbits": 0,
              "in_action": 0,
              "community_cards": []
            }
            """.trimIndent()
        )
        val response = client.post("/bet") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gameState.toString())
        }
        // In pre-flop, the decision may be to call the bet. We simply ensure a numeric value was returned.
        val decision = response.bodyAsText().toInt()
        // For example, decision could be equal to the required call (100 - 20 = 80) or different based on your strategy.
        assertTrue(decision >= 0)
    }

    // Flop: 3 community cards.
    @Test
    fun `flop decision returns call or raise based on evaluated hand`() = testApplication {
        application { installRoutes() }

        // Flop sample: 3 community cards.
        val gameState = JSONObject(
            """
            {
              "players": [
                {"id": 0, "name": "me", "status": "active", "version": "v", "stack": 800, "bet": 40, "hole_cards": [
                  {"rank": "Q", "suit": "hearts"},
                  {"rank": "Q", "suit": "diamonds"}
                ]}
              ],
              "tournament_id": "T",
              "game_id": "G",
              "round": 1,
              "bet_index": 0,
              "small_blind": 10,
              "current_buy_in": 80,
              "pot": 100,
              "minimum_raise": 20,
              "dealer": 0,
              "orbits": 1,
              "in_action": 0,
              "community_cards": [
                {"rank": "Q", "suit": "clubs"},
                {"rank": "2", "suit": "spades"},
                {"rank": "7", "suit": "hearts"}
              ]
            }
            """.trimIndent()
        )
        val response = client.post("/bet") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gameState.toString())
        }
        val decision = response.bodyAsText().toInt()
        // With trips on flop, the decision might be to raise (or go all-in if strong enough).
        assertTrue(decision >= 80) // 80 is the required call amount
    }

    // Turn/River: 4 or 5 community cards.
    @Test
    fun `after flow decision returns aggressive bet on strong hand`() = testApplication {
        application { installRoutes() }

        // Turn/River sample: 5 community cards.
        val gameState = JSONObject(
            """
            {
              "players": [
                {"id": 0, "name": "me", "status": "active", "version": "v", "stack": 1200, "bet": 50, "hole_cards": [
                  {"rank": "K", "suit": "hearts"},
                  {"rank": "K", "suit": "diamonds"}
                ]}
              ],
              "tournament_id": "T",
              "game_id": "G",
              "round": 2,
              "bet_index": 0,
              "small_blind": 10,
              "current_buy_in": 120,
              "pot": 200,
              "minimum_raise": 30,
              "dealer": 0,
              "orbits": 2,
              "in_action": 0,
              "community_cards": [
                {"rank": "K", "suit": "clubs"},
                {"rank": "2", "suit": "hearts"},
                {"rank": "7", "suit": "spades"},
                {"rank": "9", "suit": "clubs"},
                {"rank": "J", "suit": "hearts"}
              ]
            }
            """.trimIndent()
        )
        val response = client.post("/bet") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gameState.toString())
        }
        val decision = response.bodyAsText().toInt()
        // With a very strong hand (quads/full house, etc.) the decision is to go all-in.
        assertTrue(decision == 1200) // all-in decision, i.e. player's full stack
    }
}