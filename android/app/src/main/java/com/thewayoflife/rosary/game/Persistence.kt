package com.thewayoflife.rosary.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Codex progress (spec 10): persists across games, separate from the save slot. [beaten] means won at
 * least once; [complete] means won a perfect game, which opens the whole Codex.
 */
class Codex(
    val saints: MutableSet<String> = mutableSetOf(),
    val questions: MutableSet<Int> = mutableSetOf(),
    val dilemmas: MutableSet<Int> = mutableSetOf(),
    var beaten: Boolean = false,
    var complete: Boolean = false,
)

/** Text size for panels and challenges (spec 11.2), as a multiple of the normal size. */
enum class TextSize(val label: String, val scale: Float) { SMALL("Small", 0.85f), MEDIUM("Medium", 1f), LARGE("Large", 1.15f) }

/** Challenge window size (spec 11.2): its widest, and its tallest as a share of the screen height. */
enum class ModalSize(val label: String, val maxWidthDp: Int, val heightFraction: Float) {
    COMPACT("Compact", 480, 0.5f), STANDARD("Standard", 580, 0.7f), LARGE("Large", 680, 1f),
}

data class Options(
    val includeOlder: Boolean = false,
    /** Spec 11.2: only the prayers marked easy in the data. */
    val easyPrayers: Boolean = false,
    /** Spec 11.2: mix the advanced questions from the Catechism of the Catholic Church in with the normal ones. */
    val advancedQuestions: Boolean = false,
    val textSize: TextSize = TextSize.SMALL,
    val modalSize: ModalSize = ModalSize.COMPACT,
)

/** One save slot, the Codex and the options, each under its own key in app-private storage (spec 12). */
class GameStore(context: Context) {
    private val preferences = context.getSharedPreferences("wayoflife", Context.MODE_PRIVATE)

    init {
        // Left over from the bead-lighting prototype.
        context.getSharedPreferences("rosary", Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun loadGame(): GameState? = preferences.getString(KEY_SAVE, null)?.let { raw ->
        runCatching { gameFromJson(JSONObject(raw)) }.getOrNull()
    }

    fun saveGame(state: GameState?) {
        preferences.edit().apply {
            if (state == null) remove(KEY_SAVE) else putString(KEY_SAVE, gameToJson(state).toString())
        }.apply()
    }

    fun loadCodex(): Codex = preferences.getString(KEY_CODEX, null)?.let { raw ->
        runCatching {
            val o = JSONObject(raw)
            Codex(
                saints = o.getJSONArray("saints").strings().toMutableSet(),
                questions = o.getJSONArray("questions").ints().toMutableSet(),
                dilemmas = o.getJSONArray("dilemmas").ints().toMutableSet(),
                beaten = o.optBoolean("beaten"),
                complete = o.optBoolean("complete"),
            )
        }.getOrNull()
    } ?: Codex()

    fun saveCodex(codex: Codex) {
        val o = JSONObject()
            .put("saints", JSONArray(codex.saints.toList()))
            .put("questions", JSONArray(codex.questions.toList()))
            .put("dilemmas", JSONArray(codex.dilemmas.toList()))
            .put("beaten", codex.beaten)
            .put("complete", codex.complete)
        preferences.edit().putString(KEY_CODEX, o.toString()).apply()
    }

    fun loadOptions(): Options = Options(
        includeOlder = preferences.getBoolean(KEY_INCLUDE_OLDER, false),
        easyPrayers = preferences.getBoolean(KEY_EASY_PRAYERS, false),
        advancedQuestions = preferences.getBoolean(KEY_ADVANCED_QUESTIONS, false),
        textSize = TextSize.entries.firstOrNull { it.name == preferences.getString(KEY_TEXT_SIZE, null) } ?: TextSize.SMALL,
        modalSize = ModalSize.entries.firstOrNull { it.name == preferences.getString(KEY_MODAL_SIZE, null) } ?: ModalSize.COMPACT,
    )

    fun saveOptions(options: Options) {
        preferences.edit()
            .putBoolean(KEY_INCLUDE_OLDER, options.includeOlder)
            .putBoolean(KEY_EASY_PRAYERS, options.easyPrayers)
            .putBoolean(KEY_ADVANCED_QUESTIONS, options.advancedQuestions)
            .putString(KEY_TEXT_SIZE, options.textSize.name)
            .putString(KEY_MODAL_SIZE, options.modalSize.name)
            .apply()
    }

    private companion object {
        const val KEY_SAVE = "save"
        const val KEY_CODEX = "codex"
        const val KEY_INCLUDE_OLDER = "includeOlder"
        const val KEY_EASY_PRAYERS = "easyPrayers"
        const val KEY_ADVANCED_QUESTIONS = "advancedQuestions"
        const val KEY_TEXT_SIZE = "textSize"
        const val KEY_MODAL_SIZE = "modalSize"
        const val SAVE_VERSION = 1

        fun gameToJson(s: GameState): JSONObject = JSONObject()
            .put("v", SAVE_VERSION)
            .put("position", s.position)
            .put("streak", s.streak)
            .put("decade", s.decade)
            .put("cards", JSONArray(s.cards))
            .put("used", JSONArray(s.used))
            .put("qDeck", JSONArray(s.qDeck))
            .put("qPos", s.qPos)
            .put("pDeck", JSONArray(s.pDeck))
            .put("pPos", s.pPos)
            .put("usedDilemmas", JSONArray(s.usedDilemmas))
            .put("penance", s.penance)
            .put("challenge", s.challenge?.let { challengeToJson(it) } ?: JSONObject.NULL)
            .put("questionsRight", s.questionsRight)
            .put("questionsAsked", s.questionsAsked)
            .put("prayersPassed", s.prayersPassed)
            .put("prayersTried", s.prayersTried)
            .put("missedCross", s.missedCross)

        fun gameFromJson(o: JSONObject): GameState? {
            if (o.optInt("v") != SAVE_VERSION) return null
            return GameState(
                position = o.getInt("position"),
                streak = o.getInt("streak"),
                decade = o.getInt("decade"),
                cards = o.getJSONArray("cards").strings().toMutableList(),
                used = o.getJSONArray("used").strings().toMutableList(),
                qDeck = o.getJSONArray("qDeck").ints().toMutableList(),
                qPos = o.getInt("qPos"),
                pDeck = o.optJSONArray("pDeck")?.ints()?.toMutableList() ?: mutableListOf(),
                pPos = o.optInt("pPos"),
                usedDilemmas = o.getJSONArray("usedDilemmas").ints().toMutableList(),
                penance = o.getBoolean("penance"),
                challenge = o.optJSONObject("challenge")?.let { challengeFromJson(it) },
                // Saves from before the score was kept start it at 0/0.
                questionsRight = o.optInt("questionsRight"),
                questionsAsked = o.optInt("questionsAsked"),
                prayersPassed = o.optInt("prayersPassed"),
                prayersTried = o.optInt("prayersTried"),
                missedCross = o.optBoolean("missedCross"),
            )
        }

        fun challengeToJson(c: Challenge): JSONObject = when (c) {
            is Challenge.Question -> JSONObject().put("kind", "question").put("q", c.q)
            is Challenge.SaintChallenge -> JSONObject().put("kind", "saint").put("saint", c.saint)
            is Challenge.Prayer -> JSONObject().put("kind", "prayer").put("prayer", c.prayer).put("penance", c.penance)
            is Challenge.Dilemma -> JSONObject().put("kind", "dilemma").put("d", c.d)
        }

        fun challengeFromJson(o: JSONObject): Challenge? = when (o.getString("kind")) {
            "question" -> Challenge.Question(o.getInt("q"))
            "saint" -> Challenge.SaintChallenge(o.getInt("saint"))
            "prayer" -> Challenge.Prayer(o.getInt("prayer"), o.getBoolean("penance"))
            "dilemma" -> Challenge.Dilemma(o.getInt("d"))
            else -> null
        }

        fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

        fun JSONArray.ints(): List<Int> = List(length()) { getInt(it) }
    }
}
