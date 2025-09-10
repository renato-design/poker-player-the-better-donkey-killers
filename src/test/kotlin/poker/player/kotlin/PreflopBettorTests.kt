package poker.player.kotlin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PreflopBettorTests {

    private val bettor = PreflopBettor()

    @Test
    fun `parsePreflopRange parses single pair 22`() {
        val result = bettor.parsePreflopRange("22")
        assertEquals(1, result.size)
        assertEquals("22", result[0].rank)
        assertFalse(result[0].isSuited, "Pairs are not suited")
    }

    @Test
    fun `parsePreflopRange parses 22+ into all pocket pairs 22AA`() {
        val result = bettor.parsePreflopRange("22+")
        val expectedPairs = listOf("22","33","44","55","66","77","88","99","TT","JJ","QQ","KK","AA")
        assertEquals(expectedPairs.size, result.size)
        assertEquals(expectedPairs.toSet(), result.map { it.rank }.toSet())
        assertTrue(result.all { !it.isSuited }, "All pairs should be marked as non-suited")
    }

    @Test
    fun `parsePreflopRange parses AK (no suffix) as both suited and offsuit`() {
        val result = bettor.parsePreflopRange("AK")
        val akSuited = result.count { it.rank == "AK" && it.isSuited }
        val akOffsuit = result.count { it.rank == "AK" && !it.isSuited }
        assertEquals(2, akSuited + akOffsuit)
        assertEquals(1, akSuited, "Expected exactly one suited AK entry")
        assertEquals(1, akOffsuit, "Expected exactly one offsuit AK entry")
    }

    @Test
    fun `parsePreflopRange parses AKs as suited only`() {
        val result = bettor.parsePreflopRange("AKs")
        assertEquals(1, result.size)
        assertEquals("AK", result[0].rank)
        assertTrue(result[0].isSuited, "AKs should be suited only")
    }

    @Test
    fun `parsePreflopRange parses AKo as offsuit only`() {
        val result = bettor.parsePreflopRange("AKo")
        assertEquals(1, result.size)
        assertEquals("AK", result[0].rank)
        assertFalse(result[0].isSuited, "AKo should be offsuit only")
    }

    @Test
    fun `parsePreflopRange parses A2s+ as suited A2AK`() {
        val result = bettor.parsePreflopRange("A2s+")
        val expected = listOf("A2","A3","A4","A5","A6","A7","A8","A9","AT","AJ","AQ","AK")
        assertEquals(expected.size, result.size, "Expected suited ace wheel through AK")
        assertEquals(expected.toSet(), result.map { it.rank }.toSet())
        assertTrue(result.all { it.isSuited }, "All entries should be suited for s+")
    }

    @Test
    fun `parsePreflopRange parses A2o+ as offsuit A2AK`() {
        val result = bettor.parsePreflopRange("A2o+")
        val expected = listOf("A2","A3","A4","A5","A6","A7","A8","A9","AT","AJ","AQ","AK")
        assertEquals(expected.size, result.size, "Expected offsuit ace wheel through AK")
        assertEquals(expected.toSet(), result.map { it.rank }.toSet())
        assertTrue(result.all { !it.isSuited }, "All entries should be offsuit for o+")
    }

    @Test
    fun `parsePreflopRange parses K9+ as both suited and offsuit K9,KT,KJ,KQ`() {
        val result = bettor.parsePreflopRange("K9+")
        val expected = listOf("K9","KT","KJ","KQ")
        val byHand = result.groupBy { it.rank }
        assertEquals(expected.toSet(), byHand.keys)
        expected.forEach { hand ->
            val suited = byHand[hand]?.count { it.isSuited } ?: 0
            val offsuit = byHand[hand]?.count { !it.isSuited } ?: 0
            assertEquals(1, suited, "Expected one suited entry for $hand")
            assertEquals(1, offsuit, "Expected one offsuit entry for $hand")
        }
        assertEquals(expected.size * 2, result.size)
    }

    @Test
    fun `parsePreflopRange handles comma-separated list and whitespace`() {
        val result = bettor.parsePreflopRange(" 77+ ,  AKs,   76o ")
        val hands = result.map { it.rank }
        val expectedPairs = setOf("77","88","99","TT","JJ","QQ","KK","AA")

        // Pairs part
        assertTrue(expectedPairs.all { it in hands })
        assertTrue(result.filter { it.rank in expectedPairs }.all { !it.isSuited })

        // AKs part
        val akEntries = result.filter { it.rank == "AK" }
        assertEquals(1, akEntries.size)
        assertTrue(akEntries[0].isSuited, "AKs should be suited only")

        // 76o part
        val seventySixEntries = result.filter { it.rank == "76" }
        assertEquals(1, seventySixEntries.size)
        assertFalse(seventySixEntries[0].isSuited, "76o should be offsuit only")
    }

    @Test
    fun `parsePreflopRange ignores empty or invalid tokens`() {
        assertTrue(bettor.parsePreflopRange("").isEmpty())
        assertTrue(bettor.parsePreflopRange(" , , ").isEmpty())
        // Invalid tokens like single rank or malformed string should be ignored
        assertTrue(bettor.parsePreflopRange("A").isEmpty())
        assertTrue(bettor.parsePreflopRange("XYZ").isEmpty())
    }

    // Helper method to create test GameState
    private fun createTestGameState(
        inAction: Int = 0,
        dealer: Int = 0,
        smallBlind: Int = 10,
        playerStacks: List<Int> = listOf(1000, 1000),
        holeCards: List<Card>? = null,
        numPlayers: Int = 2
    ): GameState {
        val players = (0 until numPlayers).map { index ->
            Player(
                id = index,
                name = "Player$index",
                status = "active",
                version = "1.0",
                stack = if (index < playerStacks.size) playerStacks[index] else 1000,
                bet = 0,
                hole_cards = if (index == inAction) holeCards else null
            )
        }
        
        return GameState(
            tournament_id = "test",
            game_id = "test",
            round = 1,
            bet_index = 0,
            small_blind = smallBlind,
            current_buy_in = 0,
            pot = 0,
            minimum_raise = smallBlind * 2,
            dealer = dealer,
            orbits = 0,
            in_action = inAction,
            players = players,
            community_cards = emptyList()
        )
    }

    // Test makeBet for BUTTON position
    @Test
    fun `makeBet BUTTON position with 15+ BB and strong hand goes all-in`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(300), // 15 BB
            holeCards = listOf(Card("A", "spades"), Card("A", "hearts")) // AA - strong hand
        )
        
        val result = bettor.makeBet(15, Position.BUTTON, gameState)
        assertEquals(300, result, "Should go all-in with AA on button with 15+ BB")
    }

    @Test
    fun `makeBet BUTTON position with 15+ BB and weak hand folds`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(300),
            holeCards = listOf(Card("7", "spades"), Card("2", "clubs")) // 72o - weak hand
        )
        
        val result = bettor.makeBet(15, Position.BUTTON, gameState)
        assertEquals(0, result, "Should fold with 72o on button")
    }

    @Test
    fun `makeBet BUTTON position with 10-14 BB and medium hand goes all-in`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(120), // 12 BB
            holeCards = listOf(Card("K", "hearts"), Card("Q", "hearts")) // KQs - in range
        )
        
        val result = bettor.makeBet(12, Position.BUTTON, gameState)
        assertEquals(120, result, "Should go all-in with KQs on button with 10-14 BB")
    }

    @Test
    fun `makeBet BUTTON position with 5-9 BB widens range`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(70), // 7 BB
            holeCards = listOf(Card("A", "hearts"), Card("3", "clubs")) // A3o - in wider range
        )
        
        val result = bettor.makeBet(7, Position.BUTTON, gameState)
        assertEquals(70, result, "Should go all-in with A3o on button with 5-9 BB")
    }

    @Test
    fun `makeBet BUTTON position with under 5 BB uses widest range`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(40), // 4 BB
            holeCards = listOf(Card("K", "diamonds"), Card("6", "clubs")) // K6o - in widest range
        )
        
        val result = bettor.makeBet(4, Position.BUTTON, gameState)
        assertEquals(40, result, "Should go all-in with K6o on button with <5 BB")
    }

    // Test makeBet for CUTOFF position
    @Test
    fun `makeBet CUTOFF position with 15+ BB and strong hand goes all-in`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 1, // Player 0 is cutoff when dealer is 1 in 2-player game
            smallBlind = 10,
            playerStacks = listOf(200),
            holeCards = listOf(Card("K", "spades"), Card("K", "hearts")) // KK
        )
        
        val result = bettor.makeBet(20, Position.CUTOFF, gameState)
        assertEquals(200, result, "Should go all-in with KK in cutoff")
    }

    @Test
    fun `makeBet CUTOFF position with 10-14 BB and marginal hand goes all-in`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 1,
            smallBlind = 10,
            playerStacks = listOf(110),
            holeCards = listOf(Card("A", "clubs"), Card("T", "clubs")) // ATs - in range
        )
        
        val result = bettor.makeBet(11, Position.CUTOFF, gameState)
        assertEquals(110, result, "Should go all-in with ATs in cutoff with 10-14 BB")
    }

    @Test
    fun `makeBet CUTOFF position with 5-9 BB widens range`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 1,
            smallBlind = 10,
            playerStacks = listOf(60),
            holeCards = listOf(Card("Q", "hearts"), Card("J", "diamonds")) // QJo - in wider range
        )
        
        val result = bettor.makeBet(6, Position.CUTOFF, gameState)
        assertEquals(60, result, "Should go all-in with QJo in cutoff with 5-9 BB")
    }

    @Test
    fun `makeBet CUTOFF position with under 5 BB uses empty range`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 1,
            smallBlind = 10,
            playerStacks = listOf(30),
            holeCards = listOf(Card("7", "hearts"), Card("6", "clubs")) // 76o
        )
        
        val result = bettor.makeBet(3, Position.CUTOFF, gameState)
        assertEquals(0, result, "Should fold with empty range in cutoff with <5 BB")
    }

    // Test makeBet for BIG_BLIND position
    @Test
    fun `makeBet BIG_BLIND position with 15+ BB and premium hand goes all-in`() {
        val gameState = createTestGameState(
            inAction = 1,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(1000, 200), // BB has 200 (20 BB)
            holeCards = listOf(Card("A", "diamonds"), Card("K", "spades")) // AKo
        )
        
        val result = bettor.makeBet(20, Position.BIG_BLIND, gameState)
        assertEquals(200, result, "Should go all-in with AKo in big blind")
    }

    @Test
    fun `makeBet BIG_BLIND position with 10-14 BB and suited connector goes all-in`() {
        val gameState = createTestGameState(
            inAction = 1,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(1000, 120),
            holeCards = listOf(Card("T", "hearts"), Card("9", "hearts")) // T9s
        )
        
        val result = bettor.makeBet(12, Position.BIG_BLIND, gameState)
        assertEquals(120, result, "Should go all-in with T9s in big blind with 10-14 BB")
    }

    @Test
    fun `makeBet BIG_BLIND position with under 10 BB uses very wide range`() {
        val gameState = createTestGameState(
            inAction = 1,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(1000, 50),
            holeCards = listOf(Card("8", "clubs"), Card("6", "diamonds")) // 86o - in very wide range
        )
        
        val result = bettor.makeBet(5, Position.BIG_BLIND, gameState)
        assertEquals(50, result, "Should go all-in with 86o in big blind with very short stack")
    }

    // Test makeBet for SMALL_BLIND position
    @Test
    fun `makeBet SMALL_BLIND position with 15+ BB and strong hand goes all-in`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 1,
            smallBlind = 10,
            playerStacks = listOf(180), // 18 BB
            holeCards = listOf(Card("Q", "clubs"), Card("Q", "diamonds")), // QQ
            numPlayers = 3
        )
        
        val result = bettor.makeBet(18, Position.SMALL_BLIND, gameState)
        assertEquals(180, result, "Should go all-in with QQ in small blind")
    }

    @Test
    fun `makeBet SMALL_BLIND position with weak hand folds`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 1,
            smallBlind = 10,
            playerStacks = listOf(150),
            holeCards = listOf(Card("J", "hearts"), Card("4", "clubs")), // J4o - not in range
            numPlayers = 3
        )
        
        val result = bettor.makeBet(15, Position.SMALL_BLIND, gameState)
        assertEquals(0, result, "Should fold with J4o in small blind")
    }

    // Test edge cases
    @Test
    fun `makeBet with null hole cards returns 0`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 0,
            holeCards = null // No hole cards
        )
        
        val result = bettor.makeBet(10, Position.BUTTON, gameState)
        assertEquals(0, result, "Should return 0 when no hole cards available")
    }

    @Test
    fun `makeBet with single hole card returns 0`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 0,
            holeCards = listOf(Card("A", "spades")) // Only one card
        )
        
        val result = bettor.makeBet(10, Position.BUTTON, gameState)
        assertEquals(0, result, "Should return 0 when only one hole card available")
    }

    @Test
    fun `makeBet boundary test - exactly 15 BB uses correct range`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(150), // Exactly 15 BB
            holeCards = listOf(Card("A", "hearts"), Card("5", "clubs")) // A5o - should be in range for 15+ BB
        )
        
        val result = bettor.makeBet(15, Position.BUTTON, gameState)
        assertEquals(150, result, "Should go all-in with A5o on button with exactly 15 BB")
    }

    @Test
    fun `makeBet boundary test - exactly 10 BB uses correct range`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(100), // Exactly 10 BB
            holeCards = listOf(Card("8", "diamonds"), Card("7", "diamonds")) // 87s - should be in 10+ BB range
        )
        
        val result = bettor.makeBet(10, Position.BUTTON, gameState)
        assertEquals(100, result, "Should go all-in with 87s on button with exactly 10 BB")
    }

    @Test
    fun `makeBet boundary test - exactly 5 BB uses correct range`() {
        val gameState = createTestGameState(
            inAction = 0,
            dealer = 0,
            smallBlind = 10,
            playerStacks = listOf(50), // Exactly 5 BB
            holeCards = listOf(Card("T", "clubs"), Card("9", "spades")) // T9o - should be in 5+ BB range
        )
        
        val result = bettor.makeBet(5, Position.BUTTON, gameState)
        assertEquals(50, result, "Should go all-in with T9o on button with exactly 5 BB")
    }

    // Test mapCardsToPreflopCards method
    @Test
    fun `mapCardsToPreflopCards maps pocket pairs correctly`() {
        val aces = listOf(Card("A", "spades"), Card("A", "hearts"))
        val result = bettor.mapCardsToPreflopCards(aces)
        assertNotNull(result)
        assertEquals("AA", result!!.rank)
        assertFalse(result.isSuited, "Pairs should not be marked as suited")
    }

    @Test
    fun `mapCardsToPreflopCards maps suited hands correctly`() {
        val akSuited = listOf(Card("A", "hearts"), Card("K", "hearts"))
        val result = bettor.mapCardsToPreflopCards(akSuited)
        assertNotNull(result)
        assertEquals("AK", result!!.rank)
        assertTrue(result.isSuited, "AKs should be marked as suited")
    }

    @Test
    fun `mapCardsToPreflopCards maps offsuit hands correctly`() {
        val akOffsuit = listOf(Card("A", "spades"), Card("K", "hearts"))
        val result = bettor.mapCardsToPreflopCards(akOffsuit)
        assertNotNull(result)
        assertEquals("AK", result!!.rank)
        assertFalse(result.isSuited, "AKo should be marked as offsuit")
    }

    @Test
    fun `mapCardsToPreflopCards orders ranks correctly`() {
        // Test that lower rank first gets reordered to higher rank first
        val kingAce = listOf(Card("K", "spades"), Card("A", "hearts"))
        val result = bettor.mapCardsToPreflopCards(kingAce)
        assertNotNull(result)
        assertEquals("AK", result!!.rank, "Should order as AK, not KA")
        assertFalse(result.isSuited)
    }

    @Test
    fun `mapCardsToPreflopCards handles medium pairs`() {
        val eights = listOf(Card("8", "clubs"), Card("8", "diamonds"))
        val result = bettor.mapCardsToPreflopCards(eights)
        assertNotNull(result)
        assertEquals("88", result!!.rank)
        assertFalse(result.isSuited)
    }

    @Test
    fun `mapCardsToPreflopCards handles suited connectors`() {
        val suitedConnector = listOf(Card("9", "hearts"), Card("8", "hearts"))
        val result = bettor.mapCardsToPreflopCards(suitedConnector)
        assertNotNull(result)
        assertEquals("98", result!!.rank)
        assertTrue(result.isSuited)
    }

    @Test
    fun `mapCardsToPreflopCards returns null for invalid input`() {
        // Test with only one card
        val oneCard = listOf(Card("A", "spades"))
        assertNull(bettor.mapCardsToPreflopCards(oneCard))
        
        // Test with no cards
        val noCards = emptyList<Card>()
        assertNull(bettor.mapCardsToPreflopCards(noCards))
        
        // Test with three cards
        val threeCards = listOf(Card("A", "spades"), Card("K", "hearts"), Card("Q", "clubs"))
        assertNull(bettor.mapCardsToPreflopCards(threeCards))
    }

    @Test
    fun `mapCardsToPreflopCards handles invalid ranks`() {
        val invalidRank = listOf(Card("X", "spades"), Card("K", "hearts"))
        assertNull(bettor.mapCardsToPreflopCards(invalidRank))
    }
}