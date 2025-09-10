package poker.player.kotlin

import poker.player.kotlin.handtype.HandType

class FlopEvaluator {

    fun evaluateHand(cards: List<Card>): HandType {
        require(cards.size == 5) { "Hand must contain exactly 5 cards" }

        // --- Step 1: Map ranks to numeric values ---
        val rankMap = mapOf(
            "2" to 2, "3" to 3, "4" to 4, "5" to 5,
            "6" to 6, "7" to 7, "8" to 8, "9" to 9,
            "10" to 10, "J" to 11, "Q" to 12, "K" to 13, "A" to 14
        )
        val ranks = cards.map { rankMap[it.rank] ?: error("Unknown rank ${it.rank}") }
        val suits = cards.map { it.suit }

        // --- Step 2: Count occurrences ---
        val rankCounts = ranks.groupingBy { it }.eachCount().values.sortedDescending()

        val isFlush = suits.toSet().size == 1

        // Handle ace-low straight (A,2,3,4,5)
        val sortedRanks = ranks.sorted()
        val isStraight = (sortedRanks.zipWithNext().all { (a, b) -> b == a + 1 }) ||
                (sortedRanks == listOf(2, 3, 4, 5, 14)) // special case A-2-3-4-5

        // --- Step 3: Decide hand type ---
        return when {
            isStraight && isFlush && sortedRanks.maxOrNull() == 14 -> HandType.ROYAL_FLUSH
            isStraight && isFlush -> HandType.STRAIGHT_FLUSH
            rankCounts[0] == 4 -> HandType.FOUR_OF_A_KIND
            rankCounts[0] == 3 && rankCounts[1] == 2 -> HandType.FULL_HOUSE
            isFlush -> HandType.FLUSH
            isStraight -> HandType.STRAIGHT
            rankCounts[0] == 3 -> HandType.THREE_OF_A_KIND
            rankCounts[0] == 2 && rankCounts[1] == 2 -> HandType.TWO_PAIR
            rankCounts[0] == 2 -> HandType.PAIR
            else -> HandType.HIGH_CARD
        }
    }
}