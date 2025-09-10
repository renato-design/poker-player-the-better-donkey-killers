package poker.player.kotlin

import org.json.JSONArray
import org.json.JSONObject

fun parseGameState(json: JSONObject): GameState {
    // Parse community cards with default fallback to empty list.
    fun parseCards(jsonArray: JSONArray?): List<Card> {
        if (jsonArray == null) return emptyList()
        val cardList = mutableListOf<Card>()
        for (i in 0 until jsonArray.length()) {
            val cardObj = jsonArray.optJSONObject(i) ?: continue
            val card = Card(
                rank = cardObj.optString("rank", ""),
                suit = cardObj.optString("suit", "")
            )
            cardList.add(card)
        }
        return cardList
    }

    // Parse players, ignoring missing props.
    fun parsePlayers(jsonArray: JSONArray?): List<Player> {
        if (jsonArray == null) return emptyList()
        val playerList = mutableListOf<Player>()
        for (i in 0 until jsonArray.length()) {
            val playerObj = jsonArray.optJSONObject(i) ?: continue

            // Parse optional hole_cards if exists
            val holeCardsList = if (playerObj.has("hole_cards")) {
                parseCards(playerObj.optJSONArray("hole_cards"))
            } else {
                null
            }

            val player = Player(
                id = playerObj.optInt("id", 0),
                name = playerObj.optString("name", ""),
                status = playerObj.optString("status", ""),
                version = playerObj.optString("version", ""),
                stack = playerObj.optInt("stack", 0),
                bet = playerObj.optInt("bet", 0),
                hole_cards = holeCardsList
            )
            playerList.add(player)
        }
        return playerList
    }

    return GameState(
        tournament_id = json.optString("tournament_id", ""),
        game_id = json.optString("game_id", ""),
        round = json.optInt("round", 0),
        bet_index = json.optInt("bet_index", 0),
        small_blind = json.optInt("small_blind", 0),
        current_buy_in = json.optInt("current_buy_in", 0),
        pot = json.optInt("pot", 0),
        minimum_raise = json.optInt("minimum_raise", 0),
        dealer = json.optInt("dealer", 0),
        orbits = json.optInt("orbits", 0),
        in_action = json.optInt("in_action", 0),
        players = parsePlayers(json.optJSONArray("players")),
        community_cards = parseCards(json.optJSONArray("community_cards"))
    )
}