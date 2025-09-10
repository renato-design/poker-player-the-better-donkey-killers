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
    val myPlayer = gameState.players.getOrNull(gameState.in_action) ?: return 0

    // Calculate the minimal call amount required to stay in the hand
    val requiredCall = gameState.current_buy_in - myPlayer.bet
    if (requiredCall < 0) {
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

fun getHandStrength(gameState: GameState): Double {
    // Construct the query parameters for RainMan API.
    // Example parameters: you may use player hole cards and community cards as input.
    // The following constructs a parameter string with hole_cards and community_cards in a simple format.
    val myPlayer = gameState.players[gameState.in_action]
    val holeCardsParam = myPlayer.hole_cards?.joinToString(separator = ",") { "${it.rank}${it.suit.first()}" } ?: ""
    val communityCardsParam = gameState.community_cards.joinToString(separator = ",") { "${it.rank}${it.suit.first()}" }

    // Construct URL. (This endpoint and query format is only an example. Please refer to the LeanPoker RainMan API documentation for details.)
    val apiUrl = "http://rainman.leanpoker.org/score" +
            "?hole_cards=$holeCardsParam" +
            "&community_cards=$communityCardsParam"

    return try {
        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 3000
        connection.readTimeout = 3000

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseText = connection.inputStream.bufferedReader().readText()
            // Expecting a JSON response with a "score" field.
            val json = JSONObject(responseText)
            json.getDouble("score")
        } else {
            // In case of error, default to a neutral rating.
            0.5
        }
    } catch (e: Exception) {
        // Log error if needed and return neutral strength.
        0.5
    }
}
