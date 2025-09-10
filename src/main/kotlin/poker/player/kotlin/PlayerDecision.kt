package poker.player.kotlin

interface PlayerDecisionDecider {

    fun makeBet(gameState: GameState)

}