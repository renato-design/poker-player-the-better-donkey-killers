package poker.player.kotlin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Ep1OpenRangeComboTest {

    private fun c(rank: String, suit: String) = Card(rank, suit)

    @Test
    fun `ace king suited should normalize to AKs`() {
        val hand1 = listOf(c("A", "hearts"), c("K", "hearts"))
        val hand2 = listOf(c("K", "hearts"), c("A", "hearts")) // reversed order

        assertTrue(PreFlopEvaluator().isOpenForRaise(hand1), "AKs should be in the range")
        assertTrue(PreFlopEvaluator().isOpenForRaise(hand2), "KAs should normalize to AKs")
    }

    @Test
    fun `ace queen suited should normalize to AQs`() {
        val hand1 = listOf(c("A", "clubs"), c("Q", "clubs"))
        val hand2 = listOf(c("Q", "clubs"), c("A", "clubs")) // reversed order

        assertTrue(PreFlopEvaluator().isOpenForRaise(hand1), "AQs should be in the range")
        assertTrue(PreFlopEvaluator().isOpenForRaise(hand2), "QAs should normalize to AQs")
    }

    @Test
    fun `ace king offsuit should normalize to AKo`() {
        val hand1 = listOf(c("A", "hearts"), c("K", "spades"))
        val hand2 = listOf(c("K", "spades"), c("A", "hearts"))

        assertTrue(PreFlopEvaluator().isOpenForRaise(hand1), "AKo should be in the range")
        assertTrue(PreFlopEvaluator().isOpenForRaise(hand2), "Kao should normalize to AKo")
    }

    @Test
    fun `hands not in range should return false`() {
        val hand = listOf(c("2", "hearts"), c("7", "spades"))
        assertFalse(PreFlopEvaluator().isOpenForRaise(hand), "72o should not be in the range")
    }

    @Test
    fun `pocket pairs should work`() {
        val hand1 = listOf(c("9", "clubs"), c("9", "hearts"))
        val hand2 = listOf(c("6", "spades"), c("6", "diamonds"))

        assertTrue(PreFlopEvaluator().isOpenForRaise(hand1), "99 should be in the range")
        assertTrue(PreFlopEvaluator().isOpenForRaise(hand2), "66 should be in the range")
    }
}
