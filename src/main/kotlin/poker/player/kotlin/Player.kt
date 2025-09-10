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

    val handType = HandEvaluator().evaluateHand(flop.plus(myCards))

    val raiseAmount = if (flop.isEmpty()) {
        gameState.minimum_raise
    } else {
        val flopAdjustment = when (HandEvaluator().evaluateHand(flop)) {
            HandType.HIGH_CARD -> 0.0
            HandType.PAIR -> 0.2
            HandType.TWO_PAIR -> 0.4
            HandType.THREE_OF_A_KIND -> 0.6
            HandType.STRAIGHT -> 0.8
            HandType.FLUSH -> 0.8
            HandType.FULL_HOUSE -> 1.0
            HandType.FOUR_OF_A_KIND -> 1.0
            HandType.STRAIGHT_FLUSH -> 1.0
            HandType.ROYAL_FLUSH -> 1.0
        }

        (gameState.minimum_raise * (flopAdjustment)).toInt()
    }

    if(flop.size < 3 ) {
        if (requiredCall <= 0) {
            return 0
        }
        return when (handType) {
            HandType.HIGH_CARD -> requiredCall
            HandType.PAIR -> requiredCall + (raiseAmount).toInt()
            HandType.TWO_PAIR -> requiredCall + raiseAmount
            HandType.THREE_OF_A_KIND-> requiredCall + (raiseAmount * 1.5).toInt()
            HandType.STRAIGHT-> requiredCall + raiseAmount *2
            HandType.FLUSH -> requiredCall + raiseAmount *2
            HandType.FULL_HOUSE,
            HandType.FOUR_OF_A_KIND,
            HandType.STRAIGHT_FLUSH,
            HandType.ROYAL_FLUSH -> myPlayer.stack
        }
    }

    if(flop.size == 3 ) {

        if (requiredCall <= 0) {
            return raiseAmount
        }

        return when (handType) {
            HandType.HIGH_CARD -> 0
            HandType.PAIR -> requiredCall
            HandType.TWO_PAIR -> requiredCall
            HandType.THREE_OF_A_KIND-> requiredCall + raiseAmount
            HandType.STRAIGHT-> requiredCall + (raiseAmount * 1.5).toInt()
            HandType.FLUSH -> requiredCall + (raiseAmount * 1.5).toInt()
            HandType.FULL_HOUSE -> requiredCall + raiseAmount *2
            HandType.FOUR_OF_A_KIND,
            HandType.STRAIGHT_FLUSH,
            HandType.ROYAL_FLUSH -> myPlayer.stack
        }
    }

    if (requiredCall <= 0) {
        return 0
    }

    return when (handType) {
        HandType.HIGH_CARD -> 0
        HandType.PAIR -> 0
        HandType.TWO_PAIR -> requiredCall
        HandType.THREE_OF_A_KIND-> requiredCall + (raiseAmount * 1).toInt()
        HandType.STRAIGHT-> requiredCall + (raiseAmount * 1.5).toInt()
        HandType.FLUSH -> requiredCall + raiseAmount * 2
        HandType.FULL_HOUSE -> requiredCall + raiseAmount * 2
        HandType.FOUR_OF_A_KIND,
        HandType.STRAIGHT_FLUSH,
        HandType.ROYAL_FLUSH -> myPlayer.stack
    }
}
