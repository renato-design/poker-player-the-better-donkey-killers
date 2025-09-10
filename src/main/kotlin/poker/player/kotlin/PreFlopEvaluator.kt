package poker.player.kotlin

class PreFlopEvaluator {

    fun isOpenForRaise(hand: List<Card>): Boolean {
        require(hand.size == 2) { "Hand must contain exactly 2 cards" }

        val rankOrder = listOf(
            "2","3","4","5","6","7","8","9","10","J","Q","K","A"
        )

        val r1 = hand[0].rank
        val r2 = hand[1].rank

        val suited = hand[0].suit == hand[1].suit

        val ranks = listOf(r1, r2).sortedBy { rankOrder.indexOf(it) }

        val combo = when {
            // pocket pairs
            r1 == r2 -> r1 + r2
            suited -> ranks[0] + ranks[1] + "s"
            else -> ranks[0] + ranks[1] + "o"
        }

        val ep1OpenRange = setOf(
            // Pocket pairs
            "AA","KK","QQ","JJ","TT","99","88","77","66",

            // Suited (both normal + reversed)
            "AKs","KAs",
            "AQs","QAs",
            "AJs","JAs",
            "ATs","TAs",
            "A9s","9As",
            "A8s","8As",
            "A7s","7As",
            "A6s","6As",
            "A5s","5As",
            "A4s","4As",
            "KQs","QKs",
            "KJs","JKs",
            "KTs","TKs",
            "QJs","JQs",
            "QTs","TQs",
            "JTs","TJs",
            "T9s","9Ts",
            "98s","89s",

            // Offsuit (both normal + reversed)
            "AKo","KAo",
            "AQo","QAo",
            "KQo","QKo"
        )

        return combo in ep1OpenRange
    }
}

