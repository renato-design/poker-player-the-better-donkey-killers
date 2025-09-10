package poker.player.kotlin.preflop

import poker.player.kotlin.Card
import poker.player.kotlin.handtype.HandType

class PreFlopEvaluator {
    fun evaluatePreFlopHand(cards: List<Card>): HandType {
        return if (cards[0].rank == cards[1].rank) {
            HandType.PAIR
        } else {
            HandType.HIGH_CARD
        }
    }
}