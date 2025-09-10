package poker.player.kotlin

import org.json.JSONObject

class PlayerDecision {
    fun betRequest(game_state: JSONObject): Int {
        return 100
    }

    fun showdown() {

    }

    fun version(): String {
        return "donkeykilla"
    }
}
