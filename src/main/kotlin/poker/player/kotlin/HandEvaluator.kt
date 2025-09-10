package poker.player.kotlin

import poker.player.kotlin.handtype.HandType

class HandEvaluator {

    fun evaluateHand(cards: List<Card>): HandType {
        return if (cards.size == 2) evaluatePreFlopHand(cards)
        else evaluateBestHand(cards)
    }

    private fun evaluatePreFlopHand(cards: List<Card>): HandType {
        return if (cards[0].rank == cards[1].rank) {
            HandType.PAIR
        } else {
            HandType.HIGH_CARD
        }
    }

    private fun evaluateBestHand(cards: List<Card>): HandType {
        val combinations = cards.combinations(5)

        return combinations
            .map { evaluateFiveCardHand(it) }
            .maxBy { it.strength }
    }

    // ---- Helper: evaluate exactly 5 cards ----
    private fun evaluateFiveCardHand(cards: List<Card>): HandType {

        val rankMap = mapOf(
            "2" to 2, "3" to 3, "4" to 4, "5" to 5,
            "6" to 6, "7" to 7, "8" to 8, "9" to 9,
            "10" to 10, "J" to 11, "Q" to 12, "K" to 13, "A" to 14
        )
        val ranks = cards.map { rankMap[it.rank] ?: error("Unknown rank ${it.rank}") }
        val suits = cards.map { it.suit }

        val rankCounts = ranks.groupingBy { it }.eachCount().values.sortedDescending()

        val isFlush = suits.toSet().size == 1

        val sortedRanks = ranks.sorted()
        val isStraight = (sortedRanks.zipWithNext().all { (a, b) -> b == a + 1 }) ||
                (sortedRanks == listOf(2, 3, 4, 5, 14)) // special case A-2-3-4-5

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

    // ---- Helper: generate combinations of k elements ----
    private fun <T> List<T>.combinations(k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (this.isEmpty()) return emptyList()
        val head = first()
        val rest = drop(1)
        val withHead = rest.combinations(k - 1).map { listOf(head) + it }
        val withoutHead = rest.combinations(k)
        return withHead + withoutHead
    }
}