package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject

fun parseGameState(json: JSONObject): GameState {
    // Parse community cards
    fun parseCards(jsonArray: JSONArray): List<Card> {
        val cardList = mutableListOf<Card>()
        for (i in 0 until jsonArray.length()) {
            val cardObj = jsonArray.getJSONObject(i)
            val card = Card(
                rank = cardObj.getString("rank"),
                suit = cardObj.getString("suit")
            )
            cardList.add(card)
        }
        return cardList
    }

    // Parse players including optional hole_cards
    fun parsePlayers(jsonArray: JSONArray): List<Player> {
        val playerList = mutableListOf<Player>()
        for (i in 0 until jsonArray.length()) {
            val playerObj = jsonArray.getJSONObject(i)

            // Parse optional hole_cards if exists
            val holeCardsList = if (playerObj.has("hole_cards")) {
                parseCards(playerObj.getJSONArray("hole_cards"))
            } else {
                null
            }

            val player = Player(
                id = playerObj.getInt("id"),
                name = playerObj.getString("name"),
                status = playerObj.getString("status"),
                version = playerObj.getString("version"),
                stack = playerObj.getInt("stack"),
                bet = playerObj.getInt("bet"),
                hole_cards = holeCardsList
            )
            playerList.add(player)
        }
        return playerList
    }

    return GameState(
        tournament_id = json.getString("tournament_id"),
        game_id = json.getString("game_id"),
        round = json.getInt("round"),
        bet_index = json.getInt("bet_index"),
        small_blind = json.getInt("small_blind"),
        current_buy_in = json.getInt("current_buy_in"),
        pot = json.getInt("pot"),
        minimum_raise = json.getInt("minimum_raise"),
        dealer = json.getInt("dealer"),
        orbits = json.getInt("orbits"),
        in_action = json.getInt("in_action"),
        players = parsePlayers(json.getJSONArray("players")),
        community_cards = parseCards(json.getJSONArray("community_cards"))
    )
}