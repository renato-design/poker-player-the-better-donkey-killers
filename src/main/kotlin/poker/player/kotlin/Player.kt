package poker.player.kotlin

import io.ktor.server.engine.handleFailure
import org.json.JSONObject
import poker.player.kotlin.handtype.HandType

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

    val myPlayer = gameState.players[gameState.in_action]

    val myCards = mutableListOf<Card>()
    myPlayer.hole_cards?.let { myCards.addAll(it) }

    val flop = gameState.community_cards

    val requiredCall = gameState.current_buy_in - myPlayer.bet
    if (requiredCall < 0) {
        return 0
    }

    val handType = HandEvaluator().evaluateHand(flop.plus(myCards))


    if(flop.size < 3 ) {
        return when (handType) {
            HandType.HIGH_CARD -> 0
            HandType.PAIR -> requiredCall + gameState.minimum_raise *2
            HandType.TWO_PAIR -> requiredCall + gameState.minimum_raise
            HandType.THREE_OF_A_KIND-> requiredCall + (gameState.minimum_raise * 1.5).toInt()
            HandType.STRAIGHT-> requiredCall + gameState.minimum_raise *2
            HandType.FLUSH -> requiredCall + gameState.minimum_raise *2
            HandType.FULL_HOUSE,
            HandType.FOUR_OF_A_KIND,
            HandType.STRAIGHT_FLUSH,
            HandType.ROYAL_FLUSH -> myPlayer.stack
        }
    }

    if(flop.size == 3 ) {
        return when (handType) {
            HandType.HIGH_CARD -> 0
            HandType.PAIR -> 0
            HandType.TWO_PAIR -> 0
            HandType.THREE_OF_A_KIND-> requiredCall + gameState.minimum_raise
            HandType.STRAIGHT-> requiredCall + (gameState.minimum_raise * 1.5).toInt()
            HandType.FLUSH -> requiredCall + (gameState.minimum_raise * 1.5).toInt()
            HandType.FULL_HOUSE -> requiredCall + gameState.minimum_raise *2
            HandType.FOUR_OF_A_KIND,
            HandType.STRAIGHT_FLUSH,
            HandType.ROYAL_FLUSH -> myPlayer.stack
        }
    }

    return when (handType) {
        HandType.HIGH_CARD -> 0
        HandType.PAIR -> 0
        HandType.TWO_PAIR -> 0
        HandType.THREE_OF_A_KIND-> requiredCall + (gameState.minimum_raise * 1).toInt()
        HandType.STRAIGHT-> requiredCall + (gameState.minimum_raise * 1.5).toInt()
        HandType.FLUSH -> requiredCall + gameState.minimum_raise * 2
        HandType.FULL_HOUSE -> requiredCall + gameState.minimum_raise * 2
        HandType.FOUR_OF_A_KIND,
        HandType.STRAIGHT_FLUSH,
        HandType.ROYAL_FLUSH -> myPlayer.stack
    }
}
