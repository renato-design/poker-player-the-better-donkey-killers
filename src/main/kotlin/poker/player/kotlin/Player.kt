package poker.player.kotlin

import org.json.JSONObject
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
        handStrength <= 0.55 -> {
            // Hand is not good enough to continue; folding.
            0
        }
//        handStrength > 0.95 -> {
//            // For a very strong hand, try to raise the bet.
//            myPlayer.stack
//        }
//        handStrength > 0.75 && myPlayer.stack >= requiredCall + gameState.minimum_raise * 2 -> {
//            // For a very strong hand, try to raise the bet.
//            requiredCall + gameState.minimum_raise * 2
//        }
//        handStrength > 0.55 && myPlayer.stack >= requiredCall + gameState.minimum_raise -> {
//            // For a very strong hand, try to raise the bet.
//            requiredCall + gameState.minimum_raise
//        }
        else -> {
            // Otherwise, call the current bet.
            if (myPlayer.stack >= requiredCall) requiredCall else myPlayer.stack
        }
    }
}

fun getState(gameState: GameState) {

    val myPlayer = gameState.players[gameState.in_action]

    val myCards = mutableListOf<Card>()
    myPlayer.hole_cards?.let { myCards.addAll(it) }

}

fun getHandStrength(gameState: GameState): Double {
    // Define a simple mapping from card rank to a numeric value.
    val rankValues = mapOf(
        "2" to 2, "3" to 3, "4" to 4, "5" to 5,
        "6" to 6, "7" to 7, "8" to 8, "9" to 9,
        "10" to 10, "J" to 11, "Q" to 12, "K" to 13, "A" to 14
    )

    val myPlayer = gameState.players[gameState.in_action]
    // Gather all cards: hole cards (if available) and community cards.
    val allCards = mutableListOf<Card>()
    myPlayer.hole_cards?.let { allCards.addAll(it) }
    allCards.addAll(gameState.community_cards)

    // Compute total score based on available cards.
    val totalScore = allCards.sumOf { card ->
        rankValues.getOrDefault(card.rank, 0)
    }

    // In our simple approach, we assume the maximum possible total for 7 cards is if all cards are Aces.
    // That's 7 * 14 = 98. The minimal total (if all are "2") is 7 * 2 = 14.
    // Normalize the score between 0.0 and 1.0.
    val normalized = ((totalScore - 14).toDouble() / (98 - 14).toDouble()).coerceIn(0.0, 1.0)
    return normalized
}

