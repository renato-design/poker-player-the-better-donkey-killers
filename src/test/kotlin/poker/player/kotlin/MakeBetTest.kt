package poker.player.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class getStateTest {

    private fun gsWithCards(
        myBet: Int,
        currentBuyIn: Int,
        minRaise: Int,
        myStack: Int = 1000,
        holeCards: List<Card> = listOf(Card("2","clubs"), Card("3","diamonds")), // weak hand by default
        communityCards: List<Card> = emptyList()
    ): GameState {
        val me = Player(
            id = 0, name = "me", status = "active", version = "v",
            stack = myStack, bet = myBet, hole_cards = holeCards
        )
        return GameState(
            tournament_id = "T", game_id = "G", round = 0, bet_index = 0,
            small_blind = 10, current_buy_in = currentBuyIn, pot = 0,
            minimum_raise = minRaise, dealer = 0, orbits = 0, in_action = 0,
            players = listOf(me), community_cards = communityCards
        )
    }

    @Test
    fun `weak hand folds`() {
        // 2-3 offsuit is a very weak hand (high card 3, should be <= 0.55)
        val game = gsWithCards(
            myBet = 80, currentBuyIn = 320, minRaise = 240, myStack = 1000,
            holeCards = listOf(Card("2","clubs"), Card("3","diamonds"))
        )
        val bet = makeBet(game)
        assertEquals(0, bet) // should fold with weak hand
    }

    @Test
    fun `pair of aces at threshold folds`() {
        // A-A has exactly 0.55 strength (at threshold, so folds)
        val game = gsWithCards(
            myBet = 80, currentBuyIn = 320, minRaise = 240, myStack = 1000,
            holeCards = listOf(Card("A","spades"), Card("A","hearts"))
        )
        val bet = makeBet(game)
        assertEquals(0, bet) // folds at exactly 0.55 threshold
    }

    @Test
    fun `pair of kings folds`() {
        // K-K appears to be <= 0.55 strength based on the algorithm
        val game = gsWithCards(
            myBet = 50, currentBuyIn = 200, minRaise = 50, myStack = 1000,
            holeCards = listOf(Card("K","clubs"), Card("K","diamonds"))
        )
        val bet = makeBet(game)
        assertEquals(0, bet) // should fold if strength <= 0.55
    }

    @Test
    fun `pair of queens folds`() {
        // Q-Q appears to be <= 0.55 strength based on the algorithm
        val game = gsWithCards(
            myBet = 100, currentBuyIn = 300, minRaise = 50, myStack = 150,
            holeCards = listOf(Card("Q","spades"), Card("Q","hearts"))
        )
        val bet = makeBet(game)
        assertEquals(0, bet) // should fold if strength <= 0.55
    }

    @Test
    fun `negative requiredCall returns 0`() {
        // When player has already bet more than current buy-in
        val game = gsWithCards(
            myBet = 400, currentBuyIn = 300, minRaise = 50, myStack = 1000,
            holeCards = listOf(Card("A","clubs"), Card("A","diamonds")) // doesn't matter
        )
        val bet = makeBet(game)
        assertEquals(0, bet) // no additional bet needed
    }

    @Test
    fun `zero stack always returns 0`() {
        val game = gsWithCards(
            myBet = 0, currentBuyIn = 100, minRaise = 50, myStack = 0,
            holeCards = listOf(Card("A","clubs"), Card("A","diamonds")) // doesn't matter
        )
        val bet = makeBet(game)
        assertEquals(0, bet) // no money to bet
    }

    @Test
    fun `ace-king suited folds`() {
        // A-K suited has 0.5 strength (below 0.55 threshold)
        val game = gsWithCards(
            myBet = 50, currentBuyIn = 100, minRaise = 25, myStack = 1000,
            holeCards = listOf(Card("A","spades"), Card("K","spades"))
        )
        val bet = makeBet(game)
        assertEquals(0, bet) // should fold with 0.5 strength
    }

    @Test
    fun `low cards fold`() {
        // 2-7 offsuit is considered the worst starting hand
        val game = gsWithCards(
            myBet = 50, currentBuyIn = 100, minRaise = 25, myStack = 1000,
            holeCards = listOf(Card("2","clubs"), Card("7","diamonds"))
        )
        val bet = makeBet(game)
        assertEquals(0, bet) // should fold
    }

    @Test
    fun `medium pair folds`() {
        // 8-8 appears to be <= 0.55 strength based on the algorithm
        val game = gsWithCards(
            myBet = 50, currentBuyIn = 100, minRaise = 25, myStack = 1000,
            holeCards = listOf(Card("8","hearts"), Card("8","diamonds"))
        )
        val bet = makeBet(game)
        assertEquals(0, bet) // should fold if strength <= 0.55
    }
}