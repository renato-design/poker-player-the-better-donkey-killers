package poker.player.kotlin

import org.json.JSONObject
import poker.player.kotlin.handtype.HandType
import java.net.HttpURLConnection
import java.net.URL

class PlayerDecision {
    fun betRequest(game_state: JSONObject): Int {
        val gameStateData = parseGameState(game_state)
        return getState(gameStateData)
    }

    fun showdown() {

    }

    fun version(): String {
        return "donkeykilla"
    }
    
}

fun makeBet(gameState: GameState): Int {
    // Verify the players list is valid
    if (gameState.players.isEmpty() || gameState.in_action >= gameState.players.size) {
        return 0 // Fold if no valid player is found
    }
    val myPlayer = gameState.players[gameState.in_action]

    // Calculate the minimal call amount
    val requiredCall = gameState.current_buy_in - myPlayer.bet
    if (requiredCall <= 0) {
        return 0
    }

    // Get hand strength score (0.0 to 1.0)
    val handStrength = getHandStrength(gameState)

    // Simple decision strategy:
    // Adjust thresholds based on hand strength
    return when {
        handStrength <= 0.35 -> {
            // Fold if hand is weak
            0
        }
        handStrength > 0.95 -> {
            // Very strong hand; go all-in
            myPlayer.stack
        }
        handStrength > 0.75 && myPlayer.stack >= requiredCall + gameState.minimum_raise * 2 -> {
            requiredCall + gameState.minimum_raise * 2
        }
        handStrength > 0.35 && myPlayer.stack >= requiredCall + gameState.minimum_raise -> {
            requiredCall + gameState.minimum_raise
        }
        else -> {
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

    val myPlayer = gameState.players.getOrNull(gameState.in_action) ?: return 0.0
    val allCards: List<Card> = (myPlayer.hole_cards ?: emptyList()) + gameState.community_cards
    if (allCards.isEmpty()) return 0.0

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

    val flushSuit = suitFreq.entries.find { it.value >= 5 }?.key
    val isFlush = flushSuit != null

    val uniqueRanks = ranksList.toSet().toList().sorted()

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
        if (uniqueRanks.contains(14)) {
            val lowStraight = listOf(2,3,4,5)
            if (lowStraight.all { it in ranks }) return true
        }
        return false
    }

    val isStraight = hasStraight(uniqueRanks)

    var isStraightFlush = false
    if (isFlush) {
        val flushCards = allCards.filter { it.suit == flushSuit }
        val flushRanks = flushCards.map { rankValues.getOrDefault(it.rank, 0) }.toSet().toList().sorted()
        isStraightFlush = hasStraight(flushRanks)
    }

    val pairs = rankFreq.filter { it.value == 2 }.size
    val trips = rankFreq.filter { it.value == 3 }.size
    val quads = rankFreq.filter { it.value == 4 }.size

    val score = when {
        isStraightFlush && uniqueRanks.contains(14) && uniqueRanks.contains(13) -> 1.0
        isStraightFlush -> 0.9
        quads > 0 -> 0.85
        trips > 0 && pairs > 0 -> 0.8
        isFlush -> 0.75
        isStraight -> 0.7
        trips > 0 -> 0.65
        pairs >= 2 -> 0.6
        pairs == 1 -> 0.55
        else -> {
            val highCard = uniqueRanks.maxOrNull() ?: 0
            0.4 + ((highCard - 2) / 12.0 * 0.1)
        }
    }
    return score
}
