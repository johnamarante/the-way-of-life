package com.thewayoflife.rosary.game

import com.thewayoflife.rosary.data.GameData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class RulesTest {
    private val data = GameData.parse(File("src/main/assets/game-data.json").readText())
    private val random = Random(42)

    private fun judge(guess: String): String {
        val j = Rules.judgeGuess(guess, data.saints, data.equivalents)
        return when (j.result) {
            GuessResult.ACCEPTED -> data.saints[j.saint].name
            GuessResult.VAGUE -> "vague"
            GuessResult.NONE -> "none"
            GuessResult.IGNORED -> "ignored"
        }
    }

    @Test
    fun `board decades, glory beads and prayer destinations`() {
        assertEquals(listOf(1, 1, 1, 2, 4, 5, 5, 6, 6, null), listOf(0, 9, 10, 11, 43, 44, 54, 55, 57, 58).map(Rules::decadeOf))
        assertTrue(listOf(10, 21, 32, 43, 54).all(Rules::isGlory))
        assertFalse(Rules.isGlory(55))
        assertEquals(11, Rules.gloryDestination(10, latin = false))
        assertEquals(16, Rules.gloryDestination(10, latin = true))
        assertEquals(55, Rules.gloryDestination(54, latin = false))
        assertEquals(Rules.CROSS, Rules.gloryDestination(54, latin = true))
    }

    @Test
    fun `prayer times match spec 8_3`() {
        val expected = mapOf(4 to 25, 5 to 30, 6 to 40, 7 to 45, 8 to 50, 10 to 65, 11 to 70, 12 to 80, 13 to 85, 14 to 90, 20 to 130)
        expected.forEach { (segments, seconds) -> assertEquals("segments $segments", seconds, Rules.prayerSeconds(segments)) }
    }

    @Test
    fun `score counts questions and prayer challenges, perfect only with every one right`() {
        val s = Rules.newGame(data, random)
        assertTrue("an empty score counts as perfect", Rules.isPerfect(s))
        Rules.answerCorrect(s)
        Rules.answerCorrect(s)
        Rules.winSaint(s, "St. Paul")
        Rules.useCard(s, "St. Paul") // a saint card is neither right nor wrong
        Rules.moveTo(s, 10)
        Rules.completeGloryPrayer(s, latin = false)
        assertEquals(listOf(2, 2, 1, 1), listOf(s.questionsRight, s.questionsAsked, s.prayersPassed, s.prayersTried))
        assertTrue(Rules.isPerfect(s))
        Rules.answerWrong(s)
        assertEquals(listOf(2, 3), listOf(s.questionsRight, s.questionsAsked))
        assertFalse(Rules.isPerfect(s))
        val t = Rules.newGame(data, random)
        Rules.prayerMissed(t)
        assertEquals(listOf(0, 1), listOf(t.prayersPassed, t.prayersTried))
        assertFalse(Rules.isPerfect(t))
        val u = Rules.newGame(data, random)
        Rules.moveTo(u, Rules.CROSS)
        Rules.startPenance(u)
        Rules.finishPenance(u, random)
        assertFalse("a wrong choice at the Cross spoils a perfect game", Rules.isPerfect(u))
    }

    @Test
    fun `scramble never returns the correct order`() {
        val segments = listOf("a", "b", "c", "d")
        repeat(200) { assertFalse(Rules.isInOrder(Rules.scramble(segments, random), segments)) }
    }

    @Test
    fun `name matching follows the spec 6_4 worked examples`() {
        val cases = mapOf(
            "ignatius loyola" to "St. Ignatius of Loyola", "Ignatius of Loyola" to "St. Ignatius of Loyola",
            "iGNATIUS lOYOLA" to "St. Ignatius of Loyola", "ignatius" to "vague", "augustine" to "St. Augustine of Hippo",
            "bernadette" to "St. Bernadette Soubirous", "teresa" to "vague", "borromeo" to "St. Charles Borromeo",
            "boromeo" to "St. Charles Borromeo", "charles" to "vague", "carlos" to "vague", "acutis" to "St. Carlo Acutis",
            "paul" to "St. Paul", "mary" to "The Blessed Virgin Mary", "mother teresa" to "St. Teresa of Calcutta (Mother Teresa)",
            "Padre Pio (Pio of Pietrelcina)" to "St. Padre Pio (Pio of Pietrelcina)", "juan" to "vague", "jean" to "vague",
            "juan diego" to "St. Juan Diego", "simon" to "vague", "Thérèse" to "St. Thérèse of Lisieux",
            "teres of lisieux" to "St. Thérèse of Lisieux", "terese of lisyeux" to "St. Thérèse of Lisieux", "terez of leezyuh" to "St. Thérèse of Lisieux",
            "john vinay" to "St. John Vianney", "john vyanay" to "St. John Vianney", "vianney" to "St. John Vianney",
            "Ávila" to "St. Teresa of Ávila", "michael" to "St. Michael the Archangel", "archangel" to "vague",
            "louis 9" to "St. Louis IX", "William" to "St. William of Gellone", "zzzz" to "none", "  St. " to "ignored",
        )
        cases.forEach { (guess, want) -> assertEquals("guess \"$guess\"", want, judge(guess)) }
    }

    @Test
    fun `every saint's own name and aliases resolve to that saint`() {
        data.saints.forEach { s ->
            assertEquals(s.name, judge(s.name))
            s.variants.forEach { v -> assertEquals("${s.name}: $v", s.name, judge(v.joinToString(" "))) }
        }
    }

    @Test
    fun `fifth correct answer in a row triggers a saint challenge`() {
        val s = Rules.newGame(data, random)
        val moves = List(5) { Rules.answerCorrect(s) }
        assertEquals(listOf(false, false, false, false, true), moves.map { it.saintChallenge })
        assertEquals(0, s.streak)
        assertEquals(5, s.position)
        Rules.answerCorrect(s)
        Rules.answerWrong(s)
        assertEquals(0, s.streak)
    }

    @Test
    fun `cards are used once per decade and restored in the next`() {
        val s = Rules.newGame(data, random)
        Rules.winSaint(s, "St. Paul")
        s.streak = 3
        assertTrue(Rules.useCard(s, "St. Paul") != null)
        assertEquals(3, s.streak)
        assertEquals(1, s.position)
        assertNull(Rules.useCard(s, "St. Paul"))
        Rules.moveTo(s, 10)
        assertEquals(listOf("St. Paul"), s.used)
        val move = Rules.completeGloryPrayer(s, latin = true)
        assertEquals(16, s.position)
        assertEquals(listOf("St. Paul"), move.restored)
        assertTrue(s.used.isEmpty())
    }

    @Test
    fun `questions are drawn without repeats, then reshuffled, advanced only when included`() {
        val normal = data.questions.count { !it.advanced }
        val advanced = data.questions.size - normal
        assertTrue("$normal normal, $advanced advanced", normal >= 90 && advanced >= 200)
        val s = Rules.newGame(data, random)
        val seen = List(normal) { Rules.drawQuestion(s, data, advanced = false, random = random) }.toSet()
        assertEquals("each normal question once per pass", normal, seen.size)
        assertTrue("no advanced questions by default", seen.none { data.questions[it].advanced })
        val t = Rules.newGame(data, random)
        val all = List(data.questions.size) { Rules.drawQuestion(t, data, advanced = true, random = random) }.toSet()
        assertEquals("with advanced on, every question once per pass", data.questions.size, all.size)
        val old = Rules.newGame(data, random)
        old.qDeck = old.qDeck.take(83).toMutableList() // a deck saved before the bank grew
        Rules.drawQuestion(old, data, advanced = false, random = random)
        assertEquals("an old deck is replaced", data.questions.size, old.qDeck.size)
    }

    @Test
    fun `dilemmas respect the age filter and do not repeat until exhausted`() {
        val s = Rules.newGame(data, random)
        val allAges = data.dilemmas.count { it.allAges }
        val ids = List(allAges) { data.dilemmas[Rules.pickDilemma(s, data, includeOlder = false, random = random)].id }
        assertEquals(allAges, ids.toSet().size)
        assertTrue(ids.all { id -> data.dilemmas.first { it.id == id }.allAges })
        val next = data.dilemmas[Rules.pickDilemma(s, data, includeOlder = false, random = random)].id
        assertNotEquals(ids.last(), next)
    }

    @Test
    fun `penance returns to bead 1, then restores to decade 5 with cards and streak kept`() {
        val s = Rules.newGame(data, random)
        Rules.winSaint(s, "St. Paul")
        Rules.moveTo(s, 55)
        s.used = mutableListOf("St. Paul")
        s.streak = 2
        Rules.moveTo(s, Rules.CROSS)
        Rules.startPenance(s)
        assertEquals(0, s.position)
        assertTrue(s.penance)
        val move = Rules.finishPenance(s, random)
        assertTrue(s.position in 44..53)
        assertEquals(5, s.decade)
        assertEquals(listOf("St. Paul"), move.restored)
        assertEquals(2, s.streak)
        assertFalse(s.penance)
    }

    @Test
    fun `prayer deck gives every prayer once per pass, no repeat across a reshuffle, easy filter`() {
        val n = data.prayers.size
        val s = Rules.newGame(data, random)
        repeat(5) {
            val drawn = List(n) { Rules.drawPrayer(s, data, easyOnly = false, random = random) }
            assertEquals("each prayer once per pass", n, drawn.toSet().size)
            assertNotEquals("no repeat across the reshuffle", drawn.last(), Rules.drawPrayer(s, data, easyOnly = false, random = random))
            s.pPos -= 1 // put that prayer back so the next pass lines up
        }
        val easy = data.prayers.indices.filter { data.prayers[it].easy }
        assertEquals(6, easy.size)
        val t = Rules.newGame(data, random)
        repeat(5) {
            val drawn = easy.map { Rules.drawPrayer(t, data, easyOnly = true, random = random) }
            assertEquals("each easy prayer once per pass", easy, drawn.sorted())
        }
        val u = Rules.newGame(data, random)
        assertEquals("the order is fixed at the start of the game", u.pDeck[0], Rules.drawPrayer(u, data, easyOnly = false, random = random))
    }

    @Test
    fun `codex unlocks need a perfect game, saints on All prayers, older dilemmas with advanced questions`() {
        val s = Rules.newGame(data, random)
        Rules.drawPrayer(s, data, easyOnly = false, random = random)
        Rules.drawQuestion(s, data, advanced = true, random = random)
        assertEquals(CodexUnlocks(perfect = true, allSaints = true, olderDilemmas = true), Rules.codexUnlocks(s))
        val easy = Rules.newGame(data, random)
        Rules.drawQuestion(easy, data, advanced = true, random = random)
        Rules.drawPrayer(easy, data, easyOnly = true, random = random) // Easy prayers once rules out the saints
        Rules.drawPrayer(easy, data, easyOnly = false, random = random)
        assertEquals(CodexUnlocks(perfect = true, allSaints = false, olderDilemmas = true), Rules.codexUnlocks(easy))
        val normal = Rules.newGame(data, random)
        Rules.drawQuestion(normal, data, advanced = true, random = random)
        Rules.drawQuestion(normal, data, advanced = false, random = random) // so does one question without the advanced set
        assertEquals(CodexUnlocks(perfect = true, allSaints = true, olderDilemmas = false), Rules.codexUnlocks(normal))
        val missed = Rules.newGame(data, random)
        Rules.answerWrong(missed)
        assertEquals(CodexUnlocks(perfect = false, allSaints = false, olderDilemmas = false), Rules.codexUnlocks(missed))
        assertEquals(1, Rules.codexMessages(Rules.codexUnlocks(missed)).size)
        assertEquals(2, Rules.codexMessages(Rules.codexUnlocks(s)).size)
    }

    @Test
    fun `a saint already held is never offered`() {
        val s = Rules.newGame(data, random)
        data.saints.drop(1).forEach { Rules.winSaint(s, it.name) }
        repeat(20) { assertEquals(0, Rules.pickSaint(s, data, true, random)) }
    }

    @Test
    fun `lesser-known saints come up only with the option, and only they make a name vague`() {
        val s = Rules.newGame(data, random)
        repeat(200) { assertFalse(data.saints[Rules.pickSaint(s, data, false, random)].lesser) }
        assertTrue((1..2000).any { data.saints[Rules.pickSaint(s, data, true, random)].lesser })
        val index = { name: String -> data.saints.indexOfFirst { it.name == name } }
        assertEquals(GuessResult.VAGUE, Rules.judgeGuess("rose", data.saints, data.equivalents).result)
        assertEquals(index("St. Rose of Lima"), Rules.judgeGuess("rose", data.saints, data.equivalents) { !data.saints[it].lesser }.saint)
        assertEquals(index("St. Joseph"), Rules.judgeGuess("joseph", data.saints, data.equivalents).saint)
        assertEquals(index("St. Benedict of Nursia"), Rules.judgeGuess("benedict", data.saints, data.equivalents).saint)
    }
}
