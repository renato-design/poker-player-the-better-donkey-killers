package poker.player.kotlin

import org.json.JSONObject
import poker.player.kotlin.handtype.HandType
import java.net.HttpURLConnection
import java.net.URL

class PlayerDecision {
    fun betRequest(game_state: JSONObject): Int {
        val gameStateData = parseGameState(game_state)
        return makeBet(gameStateData)
    }

    fun showdown() {

    }

    fun version(): String {
        return "donkeykilla"
    }
    
}

fun makeBet(gameState: GameState): Int {
    // Get the current player's data using the in_action index
    val myPlayer = gameState.players.getOrNull(gameState.in_action)// ?: return 0

    // Calculate the minimal call amount required to stay in the hand
    val requiredCall = gameState.current_buy_in - (myPlayer?.bet?: 0)
    if (requiredCall < 0 || myPlayer == null) {
        // Nothing more to call
        return 0
    }

//    return gameState.current_buy_in - myPlayer.bet + gameState.minimum_raise

    // Obtain the hand strength rating from the RainMan API, value from 0.0 (worst) to 1.0 (best)
    val handStrength = getHandStrength(gameState)

    // Simple decision strategy:
    // If hand strength is low, fold by returning 0.
    // If hand strength is high enough and the player can afford a raise, bet call + minimum_raise.
    // Otherwise, just call the required amount.
    return when {
        handStrength <= 0.35 -> {
            // Hand is not good enough to continue; folding.
            0
        }
        handStrength > 0.95 -> {
            // For a very strong hand, try to raise the bet.
            myPlayer.stack
        }
        handStrength > 0.75 && myPlayer.stack >= requiredCall + gameState.minimum_raise * 2 -> {
            // For a very strong hand, try to raise the bet.
            requiredCall + gameState.minimum_raise * 2
        }
        handStrength > 0.35 && myPlayer.stack >= requiredCall + gameState.minimum_raise -> {
            // For a very strong hand, try to raise the bet.
            requiredCall + gameState.minimum_raise
        }
        else -> {
            // Otherwise, call the current bet.
            if (myPlayer.stack >= requiredCall) requiredCall else myPlayer.stack
        }
    }
}

fun getState(gameState: GameState): Int {

    val myPlayer = gameState.players[gameState.in_action]

    val myCards = mutableListOf<Card>()
    myPlayer.hole_cards?.let { myCards.addAll(it) }

    val flop = gameState.community_cards.subList(0, 3)

    return if (flop.plus(myCards).size == 5){
        val handType = FlopEvaluator().evaluateHand(flop.plus(myCards))
        return when (handType) {
            HandType.HIGH_CARD -> 0
            HandType.PAIR,
            HandType.TWO_PAIR,
            HandType.THREE_OF_A_KIND,
            HandType.STRAIGHT,
            HandType.FLUSH -> makeBet(gameState)
            HandType.FULL_HOUSE,
            HandType.FOUR_OF_A_KIND,
            HandType.STRAIGHT_FLUSH,
            HandType.ROYAL_FLUSH -> myPlayer.stack
        }
    } else {
        makeBet(gameState)
    }

}

fun getHandStrength(gameState: GameState): Double {
    // Map card ranks to values
    val rankValues = mapOf(
        "2" to 2, "3" to 3, "4" to 4, "5" to 5,
        "6" to 6, "7" to 7, "8" to 8, "9" to 9,
        "10" to 10, "J" to 11, "Q" to 12, "K" to 13, "A" to 14
    )

    // Combine hole cards and community cards into one list (7 cards)
    val myPlayer = gameState.players[gameState.in_action]
    val allCards: List<Card> = (myPlayer.hole_cards ?: emptyList()) + gameState.community_cards

    // Count rank frequencies and suit frequencies
    val rankFreq = mutableMapOf<Int, Int>()
    val suitFreq = mutableMapOf<String, Int>()
    val ranksList = mutableListOf<Int>()
    for (card in allCards) {
        val value = rankValues.getOrDefault(card.rank, 0)
        ranksList.add(value)
        rankFreq[value] = rankFreq.getOrDefault(value, 0) + 1
        suitFreq[card.suit] = suitFreq.getOrDefault(card.suit, 0) + 1
    }

    // Determine if flush exists (5 or more cards with same suit)
    val flushSuit = suitFreq.entries.find { it.value >= 5 }?.key
    val isFlush = flushSuit != null

    // Sort the card values and remove duplicates for straight detection
    val uniqueRanks = ranksList.toSet().toList().sorted()

    // Function that checks for straight in a sorted list
    fun hasStraight(ranks: List<Int>): Boolean {
        if (ranks.size < 5) return false
        var consecutive = 1
        for (i in 1 until ranks.size) {
            if (ranks[i] == ranks[i - 1] + 1) {
                consecutive++
                if (consecutive >= 5) return true
            } else if (ranks[i] != ranks[i - 1]) {
                consecutive = 1
            }
        }
        // Special case: Ace can be low in A-2-3-4-5
        if (uniqueRanks.contains(14)) {
            val lowStraight = listOf(2, 3, 4, 5)
            if (lowStraight.all { it in ranks }) return true
        }
        return false
    }

    // Check if straight exists
    val isStraight = hasStraight(uniqueRanks)

    // Check for straight flush (if flush, then check straight within flush suit cards)
    var isStraightFlush = false
    if (isFlush) {
        val flushCards = allCards.filter { it.suit == flushSuit }
        val flushRanks = flushCards.map { rankValues.getOrDefault(it.rank, 0) }.toSet().toList().sorted()
        isStraightFlush = hasStraight(flushRanks)
    }

    // Count multiples: pairs, three of a kind, four of a kind.
    val pairs = rankFreq.filter { it.value == 2 }.size
    val trips = rankFreq.filter { it.value == 3 }.size
    val quads = rankFreq.filter { it.value == 4 }.size

    // Determine hand type and assign a base score.
    val score = when {
        isStraightFlush && uniqueRanks.contains(14) && uniqueRanks.contains(13) -> 1.0 // Royal flush
        isStraightFlush -> 0.9 // Straight flush
        quads > 0 -> 0.85       // Four of a kind
        trips > 0 && pairs > 0 -> 0.8  // Full house
        isFlush -> 0.75         // Flush
        isStraight -> 0.7       // Straight
        trips > 0 -> 0.65       // Three of a kind
        pairs >= 2 -> 0.6       // Two pair
        pairs == 1 -> 0.55      // One Pair
        else -> {
            // For high card, use the highest card normalized between 0.4 and 0.5
            val highCard = uniqueRanks.maxOrNull() ?: 0
            0.4 + ((highCard - 2) / 12.0 * 0.1)
        }
    }

    return score
}
