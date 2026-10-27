package com.thewayoflife.rosary.data

import org.json.JSONArray
import org.json.JSONObject

data class Spot(val x: Float, val y: Float, val r: Float)

data class Board(val imageWidth: Int, val imageHeight: Int, val beads: List<Spot>, val glory: List<Boolean>, val cross: Spot)

data class Question(
    val id: Int,
    val question: String,
    val answer: String,
    val options: List<String>,
    val correct: String,
    val trueFalse: Boolean,
    /** From the Catechism of the Catholic Church; asked only with Advanced questions on (spec 11.2). */
    val advanced: Boolean,
)

data class Saint(
    val name: String,
    val year: String,
    val clues: List<String>,
    /** Normalized name variants (spec 6.4), each a list of words. */
    val variants: List<List<String>>,
    /** Normalized accepted misspellings of single name words. */
    val spellings: List<String>,
    val invocation: String,
    val portrait: String,
)

data class PrayerText(val title: String, val segments: List<String>)

/** [easy]: one of the prayers used when Easy prayers is on (spec 11.2). */
data class Prayer(val easy: Boolean, val english: PrayerText, val latin: PrayerText)

data class Dilemma(
    val id: Int,
    val title: String,
    val allAges: Boolean,
    val dilemma: String,
    val question: String,
    val right: String,
    val wrong: String,
    val catholicAnswer: String,
    val keyPrinciple: String,
    val ccc: String,
    val sources: String,
)

data class Credit(val saint: String, val image: String, val url: String, val license: String)

/** Everything the game reads, built from the CSVs by tools/build_game_data.py into game-data.json. */
data class GameData(
    val board: Board,
    val questions: List<Question>,
    val saints: List<Saint>,
    val prayers: List<Prayer>,
    val dilemmas: List<Dilemma>,
    val equivalents: List<List<String>>,
    val credits: List<Credit>,
) {
    fun saintIndex(name: String): Int = saints.indexOfFirst { it.name == name }

    companion object {
        fun parse(json: String): GameData {
            val root = JSONObject(json)
            val board = root.getJSONObject("board")
            val beads = board.getJSONArray("beads").objects()
            return GameData(
                board = Board(
                    imageWidth = board.getInt("imageWidth"),
                    imageHeight = board.getInt("imageHeight"),
                    beads = beads.map { it.spot() },
                    glory = beads.map { it.getBoolean("glory") },
                    cross = board.getJSONObject("cross").spot(),
                ),
                questions = root.getJSONArray("questions").objects().map {
                    Question(
                        id = it.getInt("id"),
                        question = it.getString("question"),
                        answer = it.getString("answer"),
                        options = it.getJSONArray("options").strings(),
                        correct = it.getString("correct"),
                        trueFalse = it.getBoolean("trueFalse"),
                        advanced = it.getBoolean("advanced"),
                    )
                },
                saints = root.getJSONArray("saints").objects().map {
                    Saint(
                        name = it.getString("name"),
                        year = it.getString("year"),
                        clues = it.getJSONArray("clues").strings(),
                        variants = it.getJSONArray("variants").let { a -> List(a.length()) { i -> a.getJSONArray(i).strings() } },
                        spellings = it.getJSONArray("spellings").strings(),
                        invocation = it.getString("invocation"),
                        portrait = it.getString("portrait"),
                    )
                },
                prayers = root.getJSONArray("prayers").objects().map {
                    Prayer(it.getBoolean("easy"), it.getJSONObject("english").prayerText(), it.getJSONObject("latin").prayerText())
                },
                dilemmas = root.getJSONArray("dilemmas").objects().map {
                    Dilemma(
                        id = it.getInt("id"),
                        title = it.getString("title"),
                        allAges = it.getBoolean("allAges"),
                        dilemma = it.getString("dilemma"),
                        question = it.getString("question"),
                        right = it.getString("right"),
                        wrong = it.getString("wrong"),
                        catholicAnswer = it.getString("catholicAnswer"),
                        keyPrinciple = it.getString("keyPrinciple"),
                        ccc = it.getString("ccc"),
                        sources = it.getString("sources"),
                    )
                },
                equivalents = root.getJSONArray("equivalents").let { a -> List(a.length()) { i -> a.getJSONArray(i).strings() } },
                credits = root.getJSONArray("credits").objects().map {
                    Credit(it.getString("saint"), it.getString("image"), it.getString("url"), it.getString("license"))
                },
            )
        }

        private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

        private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

        private fun JSONObject.spot() = Spot(getDouble("x").toFloat(), getDouble("y").toFloat(), getDouble("r").toFloat())

        private fun JSONObject.prayerText() = PrayerText(getString("title"), getJSONArray("segments").strings())
    }
}
