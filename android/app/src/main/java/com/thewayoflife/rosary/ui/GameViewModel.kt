package com.thewayoflife.rosary.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thewayoflife.rosary.data.GameData
import com.thewayoflife.rosary.game.Challenge
import com.thewayoflife.rosary.game.Codex
import com.thewayoflife.rosary.game.GameState
import com.thewayoflife.rosary.game.GameStore
import com.thewayoflife.rosary.game.Options
import com.thewayoflife.rosary.game.Rules
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Screen { TITLE, MENU, CONFIRM_NEW_GAME, GAME, OPTIONS, CODEX, ABOUT, VICTORY }

/** Game flow (spec 3): owns the save, the Codex and which challenge is open. Rules live in [Rules]. */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    val data: GameData = GameData.parse(app.assets.open("game-data.json").bufferedReader().use { it.readText() })
    private val store = GameStore(app)
    val sounds = Sounds(app)
    private val random = Random.Default

    /** The app opens on the title screen, the only time the whole board is in view (spec 1.3). */
    var screen by mutableStateOf(Screen.TITLE)
        private set
    var game: GameState? = null
        private set

    /** Bumped after every change to [game] so the board and card bar recompose. */
    var version by mutableIntStateOf(0)
        private set

    /** The open challenge; [challengeSerial] changes each time one opens, resetting its screen state. */
    var challenge by mutableStateOf<Challenge?>(null)
        private set
    var challengeSerial by mutableIntStateOf(0)
        private set

    /** Board lighting: beads 0..litUpTo are lit, [current] glows brighter. */
    var litUpTo by mutableIntStateOf(0)
        private set
    var current by mutableIntStateOf(0)
        private set
    var flashCards by mutableStateOf<List<String>>(emptyList())
        private set

    /** Changes every time cards should flash, so the same card can flash again in a later decade. */
    var flashSerial by mutableIntStateOf(0)
        private set

    /** True while a catechism question is open and unanswered, so saint cards can be used (spec 7.2). */
    var cardsUsable by mutableStateOf(false)
    var invokedCard by mutableStateOf<String?>(null)
        private set

    /** True while a Saint Challenge runs or a prayer is being arranged: it takes the whole screen (spec 6.2, 8.2). */
    var fullScreen by mutableStateOf(false)

    var codex: Codex = store.loadCodex()
        private set
    var codexVersion by mutableIntStateOf(0)
        private set
    var options by mutableStateOf(store.loadOptions())
        private set
    var hasSave by mutableStateOf(store.loadGame() != null)
        private set

    /** The finished game's score for the victory screen, and whether it was perfect (spec 9.4). */
    var victoryScore by mutableStateOf("")
        private set
    var victoryPerfect by mutableStateOf(false)
        private set

    /** What the win opened in the Codex, or what would (spec 10). */
    var victoryCodex by mutableStateOf<List<String>>(emptyList())
        private set

    /** False while the app is in the background; timers only count while it is true (spec 12). */
    var appInForeground by mutableStateOf(true)

    init {
        showBoardFor(store.loadGame())
    }

    // ---- Menu ----

    fun show(target: Screen) {
        if (screen == Screen.VICTORY) sounds.stopAll() // the victory music ends with the victory screen
        if (target == Screen.MENU) showBoardFor(store.loadGame())
        screen = target
    }

    fun requestNewGame() {
        if (store.loadGame() != null) screen = Screen.CONFIRM_NEW_GAME else startGame()
    }

    fun startGame() {
        game = Rules.newGame(data, random)
        screen = Screen.GAME
        nextChallenge()
    }

    fun resumeGame() {
        val saved = store.loadGame() ?: return show(Screen.MENU)
        game = saved
        screen = Screen.GAME
        val c = saved.challenge
        if (c != null && isValid(c)) openChallenge(c) else nextChallenge()
    }

    /** Back to the menu; the game is saved and the open challenge restarts on resume (spec 12). */
    fun leaveGame() {
        save()
        challenge = null
        cardsUsable = false
        invokedCard = null
        game = null
        show(Screen.MENU)
    }

    fun updateOptions(value: Options) {
        options = value
        store.saveOptions(value)
    }

    // ---- Challenges ----

    private fun nextChallenge() {
        val g = game ?: return
        openChallenge(Rules.challengeForPosition(g, data, random, options.includeOlder, options.easyPrayers, options.advancedQuestions))
    }

    private fun openChallenge(c: Challenge) {
        val g = game ?: return
        g.challenge = c
        save()
        invokedCard = null
        cardsUsable = false
        refreshBoard()
        challenge = c
        challengeSerial++
    }

    private fun isValid(c: Challenge): Boolean = when (c) {
        is Challenge.Question -> c.q in data.questions.indices
        is Challenge.SaintChallenge -> c.saint in data.saints.indices
        is Challenge.Prayer -> c.prayer in data.prayers.indices
        is Challenge.Dilemma -> c.d in data.dilemmas.indices
    }

    private fun flash(names: List<String>) {
        flashCards = names
        flashSerial++
    }

    private fun afterMove(move: Rules.Move, then: () -> Unit) {
        flash(move.restored)
        refreshBoard()
        then()
    }

    // Catechism question (spec 4)

    fun answeredCorrectly(questionId: Int) {
        val g = game ?: return
        addQuestionToCodex(questionId)
        val move = Rules.answerCorrect(g)
        afterMove(move) {
            if (move.saintChallenge) openChallenge(Challenge.SaintChallenge(Rules.pickSaint(g, data, random))) else nextChallenge()
        }
    }

    fun answeredWrongly() {
        val g = game ?: return
        Rules.answerWrong(g)
        cardsUsable = false
        save()
        version++
    }

    fun nextQuestionSameBead() {
        val g = game ?: return
        openChallenge(Challenge.Question(Rules.drawQuestion(g, data, options.advancedQuestions, random)))
    }

    /** Saint card tapped while a question is unanswered: show the invocation and the answer (spec 7.2). */
    fun invokeCard(name: String) {
        val g = game ?: return
        if (!cardsUsable || name !in g.cards || name in g.used) return
        cardsUsable = false
        invokedCard = name
    }

    fun continueAfterCard(questionId: Int) {
        val g = game ?: return
        val name = invokedCard ?: return
        addQuestionToCodex(questionId)
        val move = Rules.useCard(g, name) ?: return
        invokedCard = null
        afterMove(move) { nextChallenge() }
    }

    // Saint Challenge (spec 6)

    fun saintWon(saintIndex: Int) {
        val g = game ?: return
        sounds.play(Sound.CHALLENGE_PASSED)
        val name = data.saints[saintIndex].name
        Rules.winSaint(g, name)
        codex.saints.add(name)
        saveCodex()
        flash(listOf(name))
        save()
        version++
    }

    fun continueAfterSaint() = nextChallenge()

    // Prayer Challenge and penance (spec 8, 9.3)

    private var prayerMove: Rules.Move? = null

    /**
     * Spec 8.4: the pass is recorded as soon as the prayer is right, but the player stays on the finished
     * prayer to read it until they tap Continue.
     */
    fun gloryPrayerCompleted(latin: Boolean) {
        val g = game ?: return
        sounds.play(Sound.CHALLENGE_PASSED)
        prayerMove = Rules.completeGloryPrayer(g, latin)
        g.challenge = null
        save()
        version++
    }

    fun continueAfterPrayer() {
        val move = prayerMove ?: return nextChallenge()
        prayerMove = null
        afterMove(move) { nextChallenge() }
    }

    fun prayerTimedOut() {
        val g = game ?: return
        Rules.prayerMissed(g)
        save()
        version++
    }

    fun tryAnotherPrayer() = nextPrayer(penance = false)

    /**
     * Spec 8.4: giving up moves straight on to the next prayer in the deck. For a Prayer Challenge it counts
     * as missed, like running out of time; for penance it just changes the prayer.
     */
    fun giveUpPrayer(penance: Boolean) {
        val g = game ?: return
        if (!penance) Rules.prayerMissed(g)
        version++
        nextPrayer(penance)
    }

    private fun nextPrayer(penance: Boolean) {
        val g = game ?: return
        openChallenge(Challenge.Prayer(Rules.drawPrayer(g, data, options.easyPrayers, random), penance))
    }

    fun penanceCompleted() {
        val g = game ?: return
        afterMove(Rules.finishPenance(g, random)) { nextChallenge() }
    }

    // The Cross (spec 9)

    fun dilemmaAnswered(dilemmaId: Int, right: Boolean) {
        if (right) {
            codex.dilemmas.add(dilemmaId)
            saveCodex()
        }
    }

    fun continueAfterDilemma(right: Boolean) = if (right) victory() else startPenance()

    private fun startPenance() {
        val g = game ?: return
        val from = g.position
        Rules.startPenance(g)
        g.challenge = Challenge.Prayer(Rules.drawPrayer(g, data, options.easyPrayers, random), penance = true)
        save()
        challenge = null
        // Spec 9.3: the Cross glow goes out, then the beads turn off from the top down to bead 1.
        viewModelScope.launch {
            current = -1
            for (lit in minOf(from, Rules.CROSS - 1) downTo 0) {
                litUpTo = lit
                if (lit < from) current = lit
                delay(FALL_STEP_MS)
            }
            g.challenge?.let { openChallenge(it) }
        }
    }

    private fun victory() {
        val g = game ?: return
        sounds.play(Sound.VICTORY)
        val unlocks = Rules.codexUnlocks(g)
        victoryPerfect = unlocks.perfect
        victoryCodex = Rules.codexMessages(unlocks)
        victoryScore = scoreText(g)
        codex.beaten = true
        if (unlocks.perfect) codex.complete = true
        if (unlocks.allSaints) codex.allSaints = true
        if (unlocks.olderDilemmas) codex.olderDilemmas = true
        saveCodex()
        store.saveGame(null)
        hasSave = false
        game = null
        challenge = null
        litUpTo = Rules.CROSS
        current = Rules.CROSS
        screen = Screen.VICTORY
    }

    // ---- Helpers ----

    private fun addQuestionToCodex(questionId: Int) {
        if (codex.questions.add(questionId)) saveCodex()
    }

    private fun saveCodex() {
        store.saveCodex(codex)
        codexVersion++
    }

    private fun save() {
        game?.let {
            store.saveGame(it)
            hasSave = true
        }
    }

    private fun refreshBoard() {
        save()
        showBoardFor(game)
        version++
    }

    private fun showBoardFor(state: GameState?) {
        val p = state?.position ?: 0
        litUpTo = p
        current = p
    }

    private companion object {
        const val FALL_STEP_MS = 30L
    }

    override fun onCleared() {
        sounds.stopAll()
    }
}
