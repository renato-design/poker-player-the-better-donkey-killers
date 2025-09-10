package poker.player.kotlin

import org.json.JSONObject

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
    
    // New function to make bet based on GameState data class
    fun makeBet(gameState: GameState): Int {
        // Find our own player using the in_action index
        val myPlayer = gameState.players.getOrNull(gameState.in_action)


        // When player data is invalid, fold by returning 0
        if (myPlayer == null || myPlayer.stack <= 0) {
            return 0
        }
        return (gameState.current_buy_in - myPlayer.bet) + gameState.minimum_raise + 30
        
        // Simple strategy:
        // Determine call amount needed to match current_buy_in
        val callAmount = gameState.current_buy_in - myPlayer.bet
        
        // If calling uses more chips than available, then go all-in
        if (callAmount >= myPlayer.stack) {
            return myPlayer.stack
        }
        
        // If the call amount is within a reasonable range, then check for raise possibility
        // Example: if callAmount is low relative to player's stack, then raise by minimum_raise amount.
        return if (callAmount < myPlayer.stack / 2) {
            // Ensure the raise amount is within the remaining stack, otherwise go for call
            val raise = callAmount + gameState.minimum_raise
            if (raise > myPlayer.stack) myPlayer.stack else raise
        } else {
            // Otherwise, simply call
            callAmount
        }
    }
}
