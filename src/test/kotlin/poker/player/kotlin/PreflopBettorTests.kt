package poker.player.kotlin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PreflopBettorTests {

    private val bettor = PreflopBettor()

    // Helper to shorten card creation
    private fun c(rank: String, suit: String) = Card(rank, suit)

    @Test
    fun `generatePocketPairs produces 6 combos`() {
        val pairs = bettor.generatePocketPairs("A")
        assertEquals(6, pairs.size, "Pocket pairs should have 6 suit combinations")
        // All pairs must have same rank
        assertTrue(pairs.all { it.first.rank == "A" && it.second.rank == "A" })
    }

    @Test
    fun `generateSuitedCombos produces 4 suited combos`() {
        val combos = bettor.generateSuitedCombos("A", "K")
        assertEquals(4, combos.size, "Suited combos should have 4 suit combinations")
        // All combos must be suited and ranks preserved
        assertTrue(combos.all {
            it.first.suit == it.second.suit &&
                    setOf(it.first.rank, it.second.rank) == setOf("A", "K")
        })
    }

    @Test
    fun `generateOffsuitCombos produces 12 offsuit combos`() {
        val combos = bettor.generateOffsuitCombos("A", "K")
        assertEquals(12, combos.size, "Offsuit combos should have 12 suit combinations")
        // All combos must be offsuit and ranks preserved
        assertTrue(combos.all {
            it.first.suit != it.second.suit &&
                    setOf(it.first.rank, it.second.rank) == setOf("A", "K")
        })
    }

    @Test
    fun `expandSingle parses AA to 6 combos`() {
        val combos = bettor.expandSingle("AA")
        assertEquals(6, combos.size)
        assertTrue(combos.all { it.first.rank == "A" && it.second.rank == "A" })
    }

    @Test
    fun `expandSingle parses AKs to 4 suited combos`() {
        val combos = bettor.expandSingle("AKs")
        assertEquals(4, combos.size)
        assertTrue(combos.all {
            it.first.suit == it.second.suit &&
                    setOf(it.first.rank, it.second.rank) == setOf("A", "K")
        })
    }

    @Test
    fun `expandSingle parses AKo to 12 offsuit combos`() {
        val combos = bettor.expandSingle("AKo")
        assertEquals(12, combos.size)
        assertTrue(combos.all {
            it.first.suit != it.second.suit &&
                    setOf(it.first.rank, it.second.rank) == setOf("A", "K")
        })
    }

    @Test
    fun `expandPlus for pocket pairs 77+ expands to expected count`() {
        // 77, 88, 99, TT, JJ, QQ, KK, AA = 8 pocket pairs; each has 6 combos => 48
        val combos = bettor.expandPlus("77+")
        assertEquals(48, combos.size)
        assertTrue(combos.all { it.first.rank == it.second.rank })
    }

    @Test
    fun `expandPlus for suited aces A2s+ expands to expected count`() {
        // A2s through AKs => 12 ranks (2..K), each has 4 suited combos => 48
        val combos = bettor.expandPlus("A2s+")
        print(combos)
        assertEquals(48, combos.size)
        assertTrue(combos.all {
            it.first.suit == it.second.suit &&
                    setOf(it.first.rank, it.second.rank).containsAll(listOf("A"))
        })
    }

    @Test
    fun `parseHandRange aggregates multiple patterns`() {
        val combos = bettor.parseHandRange("AA, AKs, AKo")
        // AA -> 6, AKs -> 4, AKo -> 12 => total 22
        assertEquals(22, combos.size)
    }

    @Test
    fun `containsHand works for pocket pair`() {
        assertTrue(bettor.containsHand("AA", c("A","c"), c("A","d")))
        assertFalse(bettor.containsHand("AA", c("K","c"), c("A","d")))
    }

    @Test
    fun `containsHand enforces suitedness for AKs`() {
        assertTrue(bettor.containsHand("AKs", c("A","s"), c("K","s")))
        assertFalse(bettor.containsHand("AKs", c("A","s"), c("K","d")))
    }

    @Test
    fun `containsHand enforces offsuit for AKo`() {
        assertTrue(bettor.containsHand("AKo", c("A","h"), c("K","s")))
        assertFalse(bettor.containsHand("AKo", c("A","h"), c("K","h")))
    }

    @Test
    fun `containsHand supports pair plus range 77+`() {
        assertTrue(bettor.containsHand("77+", c("7","c"), c("7","d")))
        assertTrue(bettor.containsHand("77+", c("A","c"), c("A","d"))) // AA is included
        assertFalse(bettor.containsHand("77+", c("6","c"), c("6","d")))
    }

    @Test
    fun `containsHand supports suited ace plus range A2s+`() {
        assertTrue(bettor.containsHand("A2s+", c("A","c"), c("2","c")))
        assertTrue(bettor.containsHand("A2s+", c("A","d"), c("K","d")))
        assertFalse(bettor.containsHand("A2s+", c("A","h"), c("Q","s"))) // offsuit should fail
        assertFalse(bettor.containsHand("A2s+", c("A","h"), c("2","s"))) // offsuit should fail
    }
}