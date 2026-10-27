package com.thewayoflife.rosary.game

import com.thewayoflife.rosary.data.GameData
import com.thewayoflife.rosary.data.Saint
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.floor
import kotlin.random.Random

/** What the current bead (or a streak) asks of the player. */
sealed interface Challenge {
    data class Question(val q: Int) : Challenge
    data class SaintChallenge(val saint: Int) : Challenge
    data class Prayer(val prayer: Int, val penance: Boolean) : Challenge
    data class Dilemma(val d: Int) : Challenge
}

/** One saved game (spec 12). Cards are stored by saint name so saves survive data edits. */
class GameState(
    var position: Int = 0,
    var streak: Int = 0,
    var decade: Int = 1,
    val cards: MutableList<String> = mutableListOf(),
    var used: MutableList<String> = mutableListOf(),
    var qDeck: MutableList<Int> = mutableListOf(),
    var qPos: Int = 0,
    var pDeck: MutableList<Int> = mutableListOf(),
    var pPos: Int = 0,
    val usedDilemmas: MutableList<Int> = mutableListOf(),
    var penance: Boolean = false,
    var challenge: Challenge? = null,
    var questionsRight: Int = 0,
    var questionsAsked: Int = 0,
    var prayersPassed: Int = 0,
    var prayersTried: Int = 0,
    var missedCross: Boolean = false,
)

enum class GuessResult { IGNORED, ACCEPTED, VAGUE, NONE }

data class Judgement(val result: GuessResult, val saint: Int = -1)

/** Game rules (spec: the-way-of-life-rules.md). Mirrors browser/rules.js. */
object Rules {
    const val CROSS = 58
    const val FIRST_FINAL_BEAD = 55
    const val STREAK_FOR_SAINT = 5
    const val SAINT_SECONDS = 60
    private val FILLER = setOf("st", "saint", "of", "the", "and")

    // ---- Board ----

    /** Decade used for saint cards (spec 2.3): 1-5 for the decades and their Glory beads, 6 for the final beads. */
    fun decadeOf(index: Int): Int? = when {
        index >= CROSS -> null
        index >= FIRST_FINAL_BEAD -> 6
        else -> index / 11 + 1
    }

    fun isGlory(index: Int): Boolean = index < FIRST_FINAL_BEAD && index % 11 == 10

    /** Where a completed Prayer Challenge on a Glory bead sends the player (spec 8.4). */
    fun gloryDestination(index: Int, latin: Boolean): Int =
        if (index == 54) (if (latin) CROSS else FIRST_FINAL_BEAD) else index + 1 + if (latin) 5 else 0

    fun beadLabel(index: Int): String = when {
        index >= CROSS -> "The Cross"
        index >= FIRST_FINAL_BEAD -> "Final bead ${index - 54}"
        isGlory(index) -> "Glory bead ${decadeOf(index)}"
        else -> "Decade ${decadeOf(index)}, bead ${index % 11 + 1}"
    }

    // ---- Prayers ----

    /** Spec 8.3: 15 s for 4 segments up to 120 s for 20, rounded to the nearest 5 s (halves up), plus 10 s. */
    fun prayerSeconds(segmentCount: Int): Int {
        val raw = 15 + (segmentCount - 4) * 105.0 / 16
        return (floor(raw / 5 + 0.5) * 5).toInt() + 10
    }

    fun isInOrder(arrangement: List<String>, segments: List<String>): Boolean =
        arrangement.size == segments.size && arrangement.indices.all { arrangement[it] == segments[it] }

    /** A shuffled copy that is never already in the correct order (spec 8.1). */
    fun scramble(segments: List<String>, random: Random): List<String> {
        if (segments.toSet().size < 2) return segments
        var result: List<String>
        do {
            result = segments.shuffled(random)
        } while (isInOrder(result, segments))
        return result
    }

    // ---- Saint name matching (spec 6.4) ----

    fun normalize(text: String): List<String> =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}\\s]|_"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() && it !in FILLER }

    fun withinOneEdit(a: String, b: String): Boolean {
        if (a == b) return true
        if (abs(a.length - b.length) > 1) return false
        if (a.length == b.length) return a.indices.count { a[it] != b[it] } <= 1
        val (short, long) = if (a.length < b.length) a to b else b to a
        return long.indices.any { long.removeRange(it, it + 1) == short }
    }

    private fun wordMatches(guessWord: String, nameWord: String, saint: Saint, equivalents: List<List<String>>, useEquivalents: Boolean): Boolean =
        guessWord == nameWord ||
            guessWord in saint.spellings ||
            (useEquivalents && equivalents.any { guessWord in it && nameWord in it }) ||
            (nameWord.length >= 5 && withinOneEdit(guessWord, nameWord))

    fun judgeGuess(guess: String, saints: List<Saint>, equivalents: List<List<String>>): Judgement {
        val g = normalize(guess)
        if (g.isEmpty()) return Judgement(GuessResult.IGNORED)
        val exact = saints.indices.filter { i ->
            saints[i].variants.any { v -> v.size == g.size && v.indices.all { k -> wordMatches(g[k], v[k], saints[i], equivalents, false) } }
        }
        if (exact.size == 1) return Judgement(GuessResult.ACCEPTED, exact[0])
        val subset = saints.indices.filter { i ->
            saints[i].variants.any { v -> g.all { gw -> v.any { w -> wordMatches(gw, w, saints[i], equivalents, true) } } }
        }
        return when {
            subset.size == 1 -> Judgement(GuessResult.ACCEPTED, subset[0])
            subset.size > 1 -> Judgement(GuessResult.VAGUE)
            else -> Judgement(GuessResult.NONE)
        }
    }

    // ---- Game state ----

    fun newGame(data: GameData, random: Random): GameState =
        GameState(
            qDeck = data.questions.indices.shuffled(random).toMutableList(),
            pDeck = data.prayers.indices.shuffled(random).toMutableList(),
        )

    /**
     * Spec 10: only a game with every catechism question, Prayer Challenge and the choice at the Cross
     * right opens the whole Codex.
     */
    fun isPerfect(state: GameState): Boolean =
        state.questionsRight == state.questionsAsked && state.prayersPassed == state.prayersTried && !state.missedCross

    /**
     * Draws the next catechism question without repeats, reshuffling when the bank is used up (spec 4.1).
     * The deck holds every question; advanced ones are skipped unless the option includes them (spec 11.2).
     * A deck saved before the question bank changed size is replaced.
     */
    fun drawQuestion(state: GameState, data: GameData, advanced: Boolean, random: Random): Int {
        val includeAdvanced = advanced || data.questions.all { it.advanced }
        while (true) {
            if (state.qPos >= state.qDeck.size || state.qDeck.size != data.questions.size) {
                state.qDeck = data.questions.indices.shuffled(random).toMutableList()
                state.qPos = 0
            }
            val index = state.qDeck[state.qPos++]
            if (includeAdvanced || !data.questions[index].advanced) return index
        }
    }

    fun pickSaint(state: GameState, data: GameData, random: Random): Int {
        val pool = data.saints.indices.filter { data.saints[it].name !in state.cards }.ifEmpty { data.saints.indices.toList() }
        return pool.random(random)
    }

    /**
     * Spec 8.1: the prayers are shuffled into a deck at the start of the game and drawn in that order, one
     * per Prayer Challenge or penance, passed or not (the language does not matter). When the deck runs out
     * it is reshuffled, never starting with the prayer drawn last. With Easy prayers on (spec 11.2), prayers
     * not marked easy are skipped.
     */
    fun drawPrayer(state: GameState, data: GameData, easyOnly: Boolean, random: Random): Int {
        val easy = easyOnly && data.prayers.any { it.easy }
        fun eligible(i: Int) = !easy || data.prayers[i].easy
        while (true) {
            if (state.pPos >= state.pDeck.size) {
                val last = state.pDeck.getOrNull(state.pPos - 1)
                val several = data.prayers.indices.count(::eligible) > 1
                do {
                    state.pDeck = data.prayers.indices.shuffled(random).toMutableList()
                } while (several && state.pDeck.first(::eligible) == last)
                state.pPos = 0
            }
            val index = state.pDeck[state.pPos++]
            if (eligible(index)) return index
        }
    }

    /**
     * Spec 9.1: filtered by age setting, no repeats until all eligible are used; after a reshuffle the
     * dilemma drawn last (the one just failed) is not drawn first.
     */
    fun pickDilemma(state: GameState, data: GameData, includeOlder: Boolean, random: Random): Int {
        val eligible = data.dilemmas.filter { includeOlder || it.allAges }.map { it.id }
        var pool = eligible.filter { it !in state.usedDilemmas }
        if (pool.isEmpty()) {
            val lastFailed = state.usedDilemmas.lastOrNull()
            state.usedDilemmas.removeAll { it in eligible }
            pool = eligible.filter { it != lastFailed }.ifEmpty { eligible }
        }
        val id = pool.random(random)
        state.usedDilemmas.add(id)
        return data.dilemmas.indexOfFirst { it.id == id }
    }

    /** Moves the player and applies the new-decade card restore (spec 2.3, 7.1). Returns the cards restored. */
    fun moveTo(state: GameState, index: Int): List<String> {
        state.position = index
        val decade = decadeOf(index)
        if (decade != null && !isGlory(index) && decade != state.decade) {
            state.decade = decade
            val restored = state.used
            state.used = mutableListOf()
            return restored
        }
        return emptyList()
    }

    /** The challenge the current position asks for (spec 3). */
    fun challengeForPosition(
        state: GameState,
        data: GameData,
        random: Random,
        includeOlder: Boolean,
        easyOnly: Boolean,
        advanced: Boolean,
    ): Challenge = when {
        state.position >= CROSS -> Challenge.Dilemma(pickDilemma(state, data, includeOlder, random))
        isGlory(state.position) -> Challenge.Prayer(drawPrayer(state, data, easyOnly, random), penance = false)
        else -> Challenge.Question(drawQuestion(state, data, advanced, random))
    }

    data class Move(val restored: List<String>, val saintChallenge: Boolean = false)

    /** Correct catechism answer (spec 4.3, 5): advance and count the streak. */
    fun answerCorrect(state: GameState): Move {
        state.questionsRight += 1
        state.questionsAsked += 1
        state.streak += 1
        val restored = moveTo(state, state.position + 1)
        val saintChallenge = state.streak >= STREAK_FOR_SAINT
        if (saintChallenge) state.streak = 0
        return Move(restored, saintChallenge)
    }

    fun answerWrong(state: GameState) {
        state.questionsAsked += 1
        state.streak = 0
    }

    /** Saint card used on an unanswered question (spec 7.2): advance, mark used, streak unchanged. */
    fun useCard(state: GameState, saintName: String): Move? {
        if (saintName !in state.cards || saintName in state.used) return null
        state.used.add(saintName)
        return Move(moveTo(state, state.position + 1))
    }

    fun winSaint(state: GameState, saintName: String) {
        if (saintName !in state.cards) state.cards.add(saintName)
    }

    fun completeGloryPrayer(state: GameState, latin: Boolean): Move {
        state.prayersPassed += 1
        state.prayersTried += 1
        return Move(moveTo(state, gloryDestination(state.position, latin)))
    }

    /** A Prayer Challenge missed by running out of time or giving up (spec 8.4) counts against the score. */
    fun prayerMissed(state: GameState) {
        state.prayersTried += 1
    }

    /** Wrong dilemma choice (spec 9.3): back to the confessional by the waters of Baptism to do penance. */
    fun startPenance(state: GameState) {
        state.missedCross = true
        state.penance = true
        state.position = 0
    }

    /** Penance prayer completed: restored to a random bead 1-10 of Decade 5, which counts as a new decade. */
    fun finishPenance(state: GameState, random: Random): Move {
        state.penance = false
        return Move(moveTo(state, 44 + random.nextInt(10)))
    }
}
