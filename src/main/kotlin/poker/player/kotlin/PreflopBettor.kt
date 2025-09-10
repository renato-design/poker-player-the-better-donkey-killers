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

        if (isPreflop(gameState) == false) {
            return -1
        }

        if (hasOtherPlayerBetOrRaised(gameState) == true) {
            return -1
        }

        val position = getPlayerPositionForCurrentPlayer(gameState)

        val bigBlindsLeft = bigBlinedsLeftForMyPlayer(gameState)

        if (bigBlindsLeft > 15) {
            return -1
        }

        return makeBet(bigBlindsLeft, position, gameState)
    }

    fun isHoleardsOneOfTheseHands(hands: List<List<Card>>, gameState: GameState): Boolean {
        val myPlayer = gameState.players.getOrNull(gameState.in_action) ?: return false
        return hands.any { myPlayer.hasHoleCardsIn(it) }
    }

    fun areHandsInRange(range: String, gameState: GameState): Boolean {
        val myPlayer = gameState.players.getOrNull(gameState.in_action) ?: return false
        val holeCards = myPlayer.hole_cards ?: return false
        if (holeCards.size != 2) return false
        return containsHand(range, Card(holeCards[0].rank, holeCards[0].suit), Card(holeCards[1].rank, holeCards[1].suit))
    }

    private fun makeBet(bbLeft: Int, position: Position, gameState: GameState): Int {

        val myPlayer = gameState.players[gameState.in_action]

        when (position) {
            Position.BUTTON -> {
                return when {
                    bbLeft >= 15 -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                    bbLeft >= 10 -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                    bbLeft >= 5 -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                    else -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                }
            }
            Position.CUTOFF -> {
                return when {
                    bbLeft >= 20 -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                    bbLeft >= 10 -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                    bbLeft >= 5 -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                    else -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                }
            }
            Position.BIG_BLIND, Position.SMALL_BLIND -> {
                return when {
                    bbLeft >= 20 -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                    bbLeft >= 10 -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                    else -> areHandsInRange("", gameState) .let { if (it) myPlayer.stack else 0 }
                }
            }
        }
    }

    val ranks = listOf("2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A")
    val suits = listOf("c", "d", "h", "s")

    fun parseHandRange(range: String): List<Pair<Card, Card>> {
        return range.split(",").flatMap { expandPattern(it.trim()) }
    }

    fun expandPattern(pattern: String): List<Pair<Card, Card>> {
        return when {
            // pocket pairs like "22"
            pattern.length == 2 && pattern[0] == pattern[1] -> {
                val rank = pattern[0].toString()
                generatePocketPairs(rank)
            }
            // suited/offsuit with plus (e.g. A2s+, A5o+)
            pattern.endsWith("+") -> {
                val base = pattern.dropLast(1)
                expandPlus(base)
            }
            // single suited/offsuit hand (e.g. "A2s")
            else -> expandSingle(pattern)
        }
    }

    fun expandPlus(base: String): List<Pair<Card, Card>> {
        val r1 = base[0].toString()
        val r2 = base[1].toString()
        val suited = base.endsWith("s")
        val offsuit = base.endsWith("o")

        val i1 = ranks.indexOf(r1)
        val start = ranks.indexOf(r2)

        return (start until i1).flatMap { i2 ->
            val hand = r1 + ranks[i2] + if (suited) "s" else "o"
            expandSingle(hand)
        }
    }

    fun expandSingle(hand: String): List<Pair<Card, Card>> {
        val r1 = hand[0].toString()
        val r2 = hand[1].toString()
        return when {
            hand.endsWith("s") -> generateSuitedCombos(r1, r2)
            hand.endsWith("o") -> generateOffsuitCombos(r1, r2)
            else -> generatePocketPairs(r1) // fallback if "22"
        }
    }

    fun generatePocketPairs(rank: String): List<Pair<Card, Card>> {
        val cards = suits.map { Card(rank, it) }
        return cards.flatMapIndexed { i, c1 ->
            cards.drop(i + 1).map { c2 -> Pair(c1, c2) }
        }
    }

    fun generateSuitedCombos(r1: String, r2: String): List<Pair<Card, Card>> {
        return suits.map { s -> Pair(Card(r1, s), Card(r2, s)) }
    }

    fun generateOffsuitCombos(r1: String, r2: String): List<Pair<Card, Card>> {
        return suits.flatMap { s1 ->
            suits.filter { it != s1 }.map { s2 ->
                Pair(Card(r1, s1), Card(r2, s2))
            }
        }
    }

    fun containsHand(range: String, c1: Card, c2: Card): Boolean {
        val allHands = parseHandRange(range)

        // Normalize order (we don’t care which card is first)
        return allHands.any { (h1, h2) ->
            (h1 == c1 && h2 == c2) || (h1 == c2 && h2 == c1)
        }
    }
}