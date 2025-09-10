package poker.player.kotlin

enum class Position {
    BUTTON,
    CUTOFF,
    BIG_BLIND,
    SMALL_BLIND,
}

class PreflopBettor {

    fun hasOtherPlayerBetOrRaised(gameState: GameState): Boolean {
        val myPlayer = gameState.players.getOrNull(gameState.in_action) ?: return false
        return gameState.current_buy_in > myPlayer.bet
    }

    fun isPreflop(gameState: GameState): Boolean {
        return gameState.community_cards.isEmpty()
    }

    fun bigBlinedsLeftForMyPlayer(gameState: GameState): Int {
        val myPlayer = gameState.players.getOrNull(gameState.in_action) ?: return 0
        return myPlayer.stack / (gameState.small_blind * 2)
    }

    private fun isButton(gameState: GameState): Boolean {
        return gameState.dealer == gameState.in_action
    }

    private fun isCutoff(gameState: GameState): Boolean {
        val numPlayers = gameState.players.size
        val cutoffPosition = if (numPlayers > 2) (numPlayers - 2) else 0
        return gameState.in_action == cutoffPosition
    }

    private fun Player.hasHoleCardsIn(cards: List<Card>): Boolean {
        val holeCards = this.hole_cards ?: listOf<poker.player.kotlin.Card>()
        return holeCards.all { it in cards }
    }

    fun getPlayerPositionForCurrentPlayer(gameState: GameState): Position {
        val numPlayers = gameState.players.size
        return when {
            isButton(gameState) -> Position.BUTTON
            isCutoff(gameState) -> Position.CUTOFF
            gameState.in_action == (gameState.dealer + 1) % numPlayers -> Position.SMALL_BLIND
            gameState.in_action == (gameState.dealer + 2) % numPlayers -> Position.BIG_BLIND
            else -> Position.CUTOFF // Defaulting to CUTOFF for other positions
        }
    }

    fun makeBetPreflop(gameState: GameState): Int {

        if (!isPreflop(gameState)) {
            return -1
        }

        if (hasOtherPlayerBetOrRaised(gameState)) {
            return -1
        }

        val position = getPlayerPositionForCurrentPlayer(gameState)

        val bigBlindsLeft = bigBlinedsLeftForMyPlayer(gameState)

        if (bigBlindsLeft > 15) {
            return -1
        }

        return makeBet(bigBlindsLeft, position, gameState)
    }

    fun areHandsInRange(range: String, gameState: GameState): Boolean {
        val myPlayer = gameState.players.getOrNull(gameState.in_action) ?: return false
        val holeCards = myPlayer.hole_cards ?: return false
        if (holeCards.size != 2) return false
        val preflopCards = mapCardsToPreflopCards(holeCards)
        val rangeCards = parsePreflopRange(range)

        if (preflopCards != null && rangeCards.contains(preflopCards)) {
            return true
        }

        return false
    }

    fun mapCardsToPreflopCards(holeCards: List<Card>): PreflopCard? {
        if (holeCards.size != 2) return null
        val ranks = listOf("A", "K", "Q", "J", "T", "9", "8", "7", "6", "5", "4", "3", "2")
        val rank1Index = ranks.indexOf(holeCards[0].rank)
        val rank2Index = ranks.indexOf(holeCards[1].rank)
        if (rank1Index == -1 || rank2Index == -1) return null
        
        val isSuited = holeCards[0].suit == holeCards[1].suit
        
        // For pairs, use the same rank twice (e.g., "AA")
        if (rank1Index == rank2Index) {
            return PreflopCard(holeCards[0].rank + holeCards[0].rank, false) // Pairs are never "suited"
        }
        
        // For non-pairs, put the higher rank first (e.g., "AK" not "KA")
        val higherRank = if (rank1Index < rank2Index) holeCards[0].rank else holeCards[1].rank
        val lowerRank = if (rank1Index < rank2Index) holeCards[1].rank else holeCards[0].rank
        return PreflopCard(higherRank + lowerRank, isSuited)
    }

    fun makeBet(bbLeft: Int, position: Position, gameState: GameState): Int {

        val myPlayer = gameState.players[gameState.in_action]

        when (position) {
            Position.BUTTON -> {
                return when {
                    bbLeft >= 15 -> areHandsInRange("A2s+,K5s+,Q7s+,J8s+,T8s+,97s+,86s+,76s,22+,A4o+,K9o+,Q9o+,JTo,T9o", gameState) .let { if (it) myPlayer.stack else 0 }
                    bbLeft >= 10 -> areHandsInRange("A2s+,K4s+,Q6s+,J7s+,T8s+,97s+,87s,76s,22+,A4o+,K9o+,JTo,T9o", gameState) .let { if (it) myPlayer.stack else 0 }
                    bbLeft >= 5 -> areHandsInRange("A2s+,K2s+,Q5s+,J7s+,T7s+,97s+,87s,76s,22+,A2o+,K5o+,Q9o+,J9o,T9o", gameState) .let { if (it) myPlayer.stack else 0 }
                    else -> areHandsInRange("A2s+,K2s+,Q5s+,J7s+,T7s+,97s+,87s,76s,22+,A2o+,K5o+,Q9o+,J9o,T9o", gameState) .let { if (it) myPlayer.stack else 0 }
                }
            }
            Position.CUTOFF -> {
                return when {
                    bbLeft >= 20 -> areHandsInRange("A2s+,K3s+,Q7s+,J8s+,T8s+,97s+,86s+,76s,22+,A5o+,K9o+,Q9o+,JTo,T9o", gameState) .let { if (it) gameState.minimum_raise else 0 }
                    bbLeft >= 15 -> areHandsInRange("A2s+,K5s+,Q8s+,J8s+,T8s+,98s+,87s,22+,A2o+,KTo+,QTo+,JTo", gameState) .let { if (it) myPlayer.stack else 0 }
                    bbLeft >= 10 -> areHandsInRange("A2s+,K5s+,Q8s+,J8s+,T8s+,98s+,87s,22+,A2o+,KTo+,QTo+,JTo", gameState) .let { if (it) myPlayer.stack else 0 }
                    bbLeft >= 5 -> areHandsInRange("A2s+,K2s+,Q6s+,J7s+,T7s+,98s,87s,22+,A2o+,K6o+,Q9o+,J9o+", gameState) .let { if (it) myPlayer.stack else 0 }
                    else -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                }
            }
            Position.BIG_BLIND, Position.SMALL_BLIND -> {
                return when {
                    bbLeft >= 20 -> areHandsInRange("A2s+,K3s+,Q7s+,J8s+,T8s+,97s+,86s+,76s,22+,A5o+,K9o+,Q9o+,JTo,T9o", gameState) .let { if (it) gameState.minimum_raise else 0 }
                    bbLeft >= 15 -> areHandsInRange("A2s+,K9s+,Q9s+J9s+,T9s+,98s+,22+,A8o+,KJo+", gameState) .let { if (it) myPlayer.stack else 0 }
                    bbLeft >= 10 -> areHandsInRange("A2s+,K9s+,Q9s+J9s+,T9s+,98s+,22+,A8o+,KJo+", gameState) .let { if (it) myPlayer.stack else 0 }
                    else -> areHandsInRange("A2s+,K2s+,Q2s+J2s+,T2s+,92s+,83s+,74s+,63s+,53s+,43s+,22+,A2o+,K2o+,Q2o+,J2o+,T3o+,96o+,86o+,76o+", gameState) .let { if (it) myPlayer.stack else 0 }
                }
            }
        }
    }

    data class PreflopCard(
        val rank: String,
        val isSuited: Boolean
    )

    val ranks = listOf("A", "K", "Q", "J", "T", "9", "8", "7", "6", "5", "4", "3", "2")

    fun parsePreflopRange(range: String): List<PreflopCard> {
        val result = mutableListOf<PreflopCard>()

        fun rankIndex(rank: String) = ranks.indexOf(rank)

        for (part in range.split(",")) {
            val cleanPart = part.trim()
            if (cleanPart.isEmpty()) continue

            val plus = cleanPart.endsWith("+")
            val type = when {
                cleanPart.endsWith("s+") || cleanPart.endsWith("s") -> "s"
                cleanPart.endsWith("o+") || cleanPart.endsWith("o") -> "o"
                else -> ""
            }

            val main = cleanPart.removeSuffix("+").removeSuffix("s").removeSuffix("o")

            if (main.length == 2) {
                val r1 = main[0].toString()
                val r2 = main[1].toString()

                if (r1 == r2) { // Pair
                    val start = rankIndex(r1)
                    val end = if (plus) 0 else start
                    for (i in start downTo end) {
                        result.add(PreflopCard(ranks[i] + ranks[i], false))
                    }
                } else { // Non-pair
                    val start = rankIndex(r2)
                    val end = if (plus) {
                        val r1Index = rankIndex(r1)
                        // For XY+, we want to go up to the rank just before X
                        // Since ranks are in descending order, "just before X" is at index r1Index + 1
                        r1Index + 1
                    } else start
                    for (i in start downTo end) {
                        val hand = r1 + ranks[i]
                        when (type) {
                            "s" -> result.add(PreflopCard(hand, true))
                            "o" -> result.add(PreflopCard(hand, false))
                            "" -> {
                                result.add(PreflopCard(hand, true))
                                result.add(PreflopCard(hand, false))
                            }
                        }
                    }
                }
            }
        }
        return result
    }

}