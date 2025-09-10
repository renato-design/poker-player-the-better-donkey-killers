package poker.player.kotlin

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameStateParserTest {

    @Test
    fun `parse sample game state from docs`() {
        val json = JSONObject(
            // Trimmed but structurally faithful to LeanPoker's docs
            // (players array, in_action, minimum_raise, etc.)
            """
            {
              "tournament_id":"T",
              "game_id":"G",
              "round":0,
              "bet_index":0,
              "small_blind":10,
              "current_buy_in":320,
              "pot":400,
              "minimum_raise":240,
              "dealer":1,
              "orbits":7,
              "in_action":1,
              "players":[
                {"id":0,"name":"Albert","status":"active","version":"v","stack":1010,"bet":320},
                {"id":1,"name":"Bob","status":"active","version":"v","stack":1590,"bet":80,
                 "hole_cards":[{"rank":"6","suit":"hearts"},{"rank":"K","suit":"spades"}]
                },
                {"id":2,"name":"Chuck","status":"out","version":"v","stack":0,"bet":0}
              ],
              "community_cards":[{"rank":"4","suit":"spades"},{"rank":"A","suit":"hearts"},{"rank":"6","suit":"clubs"}]
            }
            """.trimIndent()
        )

        val gs = parseGameState(json)

        assertEquals("T", gs.tournament_id)
        assertEquals("G", gs.game_id)
        assertEquals(320, gs.current_buy_in)
        assertEquals(240, gs.minimum_raise)  // critical field for raise logic
        assertEquals(1, gs.in_action)
        assertEquals(3, gs.players.size)
        assertEquals(3, gs.community_cards.size)

        val me = gs.players[1]
        assertEquals("Bob", me.name)
        assertEquals(80, me.bet)
        assertEquals(2, me.hole_cards?.size)
        assertEquals("hearts", me.hole_cards!![0].suit)
    }

    @Test
    fun `parse player without hole_cards returns null`() {
        val json = JSONObject(
            """
            {
              "tournament_id":"T","game_id":"G","round":0,"bet_index":0,"small_blind":10,
              "current_buy_in":0,"pot":0,"minimum_raise":10,"dealer":0,"orbits":0,"in_action":0,
              "players":[{"id":0,"name":"P","status":"active","version":"v","stack":1000,"bet":0}],
              "community_cards":[]
            }
            """.trimIndent()
        )

        val gs = parseGameState(json)
        assertNull(gs.players[0].hole_cards)
    }

    @Test
    fun `parse empty community cards`() {
        val json = JSONObject(
            """
            {
              "tournament_id":"T","game_id":"G","round":0,"bet_index":0,"small_blind":10,
              "current_buy_in":0,"pot":0,"minimum_raise":10,"dealer":0,"orbits":0,"in_action":0,
              "players":[{"id":0,"name":"P","status":"active","version":"v","stack":1000,"bet":0}],
              "community_cards":[]
            }
            """.trimIndent()
        )

        val gs = parseGameState(json)
        assertTrue(gs.community_cards.isEmpty())
    }

    @Test
    fun `parse multiple players with different statuses`() {
        val json = JSONObject(
            """
            {
              "tournament_id":"T","game_id":"G","round":0,"bet_index":0,"small_blind":10,
              "current_buy_in":100,"pot":200,"minimum_raise":50,"dealer":0,"orbits":1,"in_action":1,
              "players":[
                {"id":0,"name":"Active","status":"active","version":"v","stack":1000,"bet":50},
                {"id":1,"name":"Folded","status":"folded","version":"v","stack":800,"bet":0},
                {"id":2,"name":"Out","status":"out","version":"v","stack":0,"bet":0}
              ],
              "community_cards":[{"rank":"A","suit":"spades"}]
            }
            """.trimIndent()
        )

        val gs = parseGameState(json)
        assertEquals(3, gs.players.size)
        assertEquals("active", gs.players[0].status)
        assertEquals("folded", gs.players[1].status)
        assertEquals("out", gs.players[2].status)
        assertEquals(1, gs.community_cards.size)
        assertEquals("A", gs.community_cards[0].rank)
        assertEquals("spades", gs.community_cards[0].suit)
    }

    @Test
    fun `parse game state with all required fields`() {
        val json = JSONObject(
            """
            {
              "tournament_id":"TOURNAMENT_123",
              "game_id":"GAME_456",
              "round":5,
              "bet_index":3,
              "small_blind":25,
              "current_buy_in":200,
              "pot":500,
              "minimum_raise":100,
              "dealer":2,
              "orbits":10,
              "in_action":1,
              "players":[{"id":0,"name":"Player","status":"active","version":"1.0","stack":2000,"bet":100}],
              "community_cards":[{"rank":"K","suit":"hearts"}]
            }
            """.trimIndent()
        )

        val gs = parseGameState(json)
        assertEquals("TOURNAMENT_123", gs.tournament_id)
        assertEquals("GAME_456", gs.game_id)
        assertEquals(5, gs.round)
        assertEquals(3, gs.bet_index)
        assertEquals(25, gs.small_blind)
        assertEquals(200, gs.current_buy_in)
        assertEquals(500, gs.pot)
        assertEquals(100, gs.minimum_raise)
        assertEquals(2, gs.dealer)
        assertEquals(10, gs.orbits)
        assertEquals(1, gs.in_action)
        assertEquals(1, gs.players.size)
        assertEquals(1, gs.community_cards.size)
    }
}