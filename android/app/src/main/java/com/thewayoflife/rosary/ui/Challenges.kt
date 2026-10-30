package com.thewayoflife.rosary.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thewayoflife.rosary.data.Dilemma
import com.thewayoflife.rosary.data.Prayer
import com.thewayoflife.rosary.game.Challenge
import com.thewayoflife.rosary.game.GameState
import com.thewayoflife.rosary.game.GuessResult
import com.thewayoflife.rosary.game.Rules
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val CORRECT_PAUSE_MS = 900L

fun scoreText(game: GameState) =
    "Questions: ${game.questionsRight}/${game.questionsAsked} \u00B7 Prayer challenge: ${game.prayersPassed}/${game.prayersTried}"

// ---- In-game bar: menu button, saint cards, streak (spec 5, 7.1) ----

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameBar(vm: GameViewModel, onMenu: () -> Unit, onToast: (String) -> Unit, modifier: Modifier = Modifier) {
    @Suppress("UNUSED_VARIABLE") val version = vm.version
    val game = vm.game ?: return
    // Copies taken on every recomposition: the game mutates these lists in place, and the LazyRow
    // keeps its item lambda while the captured values look unchanged, so it would miss new cards.
    val cards = game.cards.toList()
    val used = game.used.toSet()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Palette.parchment.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MenuButton(onMenu)
            if (cards.isEmpty()) {
                Text("No saint cards yet", modifier = Modifier.weight(1f), fontStyle = FontStyle.Italic, color = Palette.inkSoft, fontSize = 15.sp)
            } else {
                LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cards, key = { it }) { name ->
                        SaintCard(
                            name = name,
                            portrait = vm.data.saints[vm.data.saintIndex(name)].portrait,
                            used = name in used,
                            usable = vm.cardsUsable && name !in used,
                            flash = name in vm.flashCards,
                            flashSerial = vm.flashSerial,
                            onTap = { if (vm.cardsUsable && name !in game.used) vm.invokeCard(name) else onToast(name) },
                            onLongPress = { onToast(name) },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(Rules.STREAK_FOR_SAINT) { i ->
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (i < game.streak) Palette.gold else Palette.parchment)
                            .border(1.dp, Palette.gold, CircleShape),
                    )
                }
            }
        }
        Text(scoreText(game), fontSize = 14.sp, color = Palette.inkSoft)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Palette.paper)
            .border(1.dp, Palette.line, CircleShape)
            .combinedClickable(onClickLabel = "Main menu", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("☰", fontSize = 20.sp) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SaintCard(
    name: String,
    portrait: String,
    used: Boolean,
    usable: Boolean,
    flash: Boolean,
    flashSerial: Int,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val image = rememberPortrait(portrait)
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(flash, flashSerial) {
        if (flash) {
            pulse.animateTo(1f)
            pulse.animateTo(0f)
        }
    }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { scaleX = 1f + pulse.value * 0.15f; scaleY = scaleX }
            .clip(shape)
            .border(if (usable || pulse.value > 0f) 3.dp else 2.dp, if (usable || pulse.value > 0f) Palette.gold else Palette.line, shape)
            .combinedClickable(
                onClickLabel = if (usable) "Ask for $name's prayers" else name,
                onClick = onTap,
                onLongClick = onLongPress,
            ),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).alpha(if (used) 0.45f else 1f),
                colorFilter = if (used) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
            )
        }
    }
}

// ---- Modal host ----

/**
 * The open challenge as a card over the board, above or below the player's bead so it stays visible, no
 * taller than the share of the screen chosen in Options (spec 11.2). While a Saint Challenge runs or a
 * prayer is being arranged it takes the whole screen instead (spec 6.2, 8.2). Only the modifiers change,
 * so the challenge keeps its state.
 */
@Composable
fun ChallengeModal(vm: GameViewModel, modifier: Modifier = Modifier) {
    val challenge = vm.challenge ?: return
    val game = vm.game ?: return
    val spot = if (game.position >= Rules.CROSS) vm.data.board.cross else vm.data.board.beads[game.position]
    val size = vm.options.modalSize
    val full = vm.fullScreen
    val screenHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    Box(
        modifier = modifier.padding(if (full) PaddingValues(0.dp) else PaddingValues(start = 12.dp, end = 12.dp, top = 86.dp, bottom = 12.dp)),
        contentAlignment = if (full || spot.y > 0.5f) Alignment.TopCenter else Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = if (full) FULL_SCREEN_MAX_WIDTH else size.maxWidthDp.dp)
                .fillMaxWidth()
                .then(if (full) Modifier.fillMaxHeight() else Modifier.heightIn(max = screenHeight * size.heightFraction)),
            shape = if (full) RectangleShape else RoundedCornerShape(14.dp),
            color = Palette.paper,
            border = if (full) null else BorderStroke(1.dp, Palette.line),
            shadowElevation = if (full) 0.dp else 8.dp,
        ) {
            androidx.compose.runtime.key(vm.challengeSerial) {
                when (challenge) {
                    is Challenge.Question -> QuestionContent(vm, challenge.q)
                    is Challenge.SaintChallenge -> SaintChallengeContent(vm, challenge.saint)
                    is Challenge.Prayer -> PrayerContent(vm, challenge.prayer, challenge.penance)
                    is Challenge.Dilemma -> DilemmaContent(vm, challenge.d)
                }
            }
        }
    }
}

/** Scrolling content with an optional timer badge pinned in the top corner (spec 1.3). */
@Composable
private fun ModalBody(
    timer: String? = null,
    timerLow: Boolean = false,
    scroll: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(scroll)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { content() }
        if (timer != null) TimerBadge(timer, timerLow, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun TimerBadge(label: String, low: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(50))
            .background(Palette.button)
            .border(1.dp, if (low) Palette.wrong else Palette.line, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) { Text(label, color = if (low) Palette.wrong else Palette.ink, fontSize = 18.sp) }
}

/** The top of a full-screen challenge: it covers the bar, so the menu button comes with it. */
@Composable
private fun SheetHead(vm: GameViewModel, kicker: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        MenuButton { vm.leaveGame() }
        Kicker(kicker)
    }
}

@Composable
private fun Kicker(text: String) = Text(text.uppercase(), fontSize = 13.sp, letterSpacing = 1.sp, color = Palette.inkSoft, modifier = Modifier.padding(end = 72.dp))

@Composable
private fun Heading(text: String) = Text(text, fontSize = 23.sp, lineHeight = 29.sp, modifier = Modifier.padding(end = 72.dp))

@Composable
private fun Feedback(text: String, good: Boolean) {
    if (text.isNotEmpty()) Text(text, fontStyle = FontStyle.Italic, color = if (good) Palette.right else Palette.wrong)
}

@Composable
private fun AnswerNote(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Palette.note)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(Palette.gold))
        Column(Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 12.dp)) { content() }
    }
}

@Composable
private fun Portrait(file: String, name: String, folder: String = "portraits") {
    val image = rememberPortrait(file, folder)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(160.dp).clip(RoundedCornerShape(10.dp)).border(2.dp, Palette.line, RoundedCornerShape(10.dp))) {
            if (image != null) Image(bitmap = image, contentDescription = name, modifier = Modifier.size(160.dp))
        }
    }
}

// ---- Countdown that only runs while the app is in the foreground (spec 12) ----

private class Countdown(val seconds: Int) {
    var elapsed by mutableDoubleStateOf(0.0)
    var running by mutableStateOf(true)
    val remaining: Double get() = (seconds - elapsed).coerceAtLeast(0.0)
    val label: String get() = "${kotlin.math.ceil(remaining).toInt()}s"
    val low: Boolean get() = remaining <= 10
}

@Composable
private fun RunCountdown(countdown: Countdown, vm: GameViewModel, onEnd: () -> Unit) {
    val end by rememberUpdatedState(onEnd)
    LaunchedEffect(countdown) {
        var last = System.nanoTime()
        while (countdown.running) {
            delay(100)
            val now = System.nanoTime()
            if (vm.appInForeground) countdown.elapsed += (now - last) / 1e9
            last = now
            if (countdown.running && countdown.remaining <= 0.0) {
                countdown.running = false
                end()
            }
        }
    }
}

// ---- Catechism question (spec 4, 7.2) ----

@Composable
private fun QuestionContent(vm: GameViewModel, qIndex: Int) {
    val q = vm.data.questions[qIndex]
    val choices = remember { if (q.trueFalse) q.options else q.options.shuffled() }
    // "What is this?" questions show one of several photos of the object (spec 4.5).
    val picture = remember { q.pictures.randomOrNull() }
    var picked by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.cardsUsable = true }
    val invoked = vm.invokedCard
    val position = vm.game?.position ?: 0

    if (invoked != null) {
        val saint = vm.data.saints[vm.data.saintIndex(invoked)]
        ModalBody {
            Kicker(Rules.beadLabel(position))
            Portrait(saint.portrait, saint.name)
            Text(saint.invocation, fontSize = 22.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(q.question)
            if (picture != null) Portrait(picture, "Picture for the question", folder = "objects")
            AnswerNote {
                Text(q.correct, fontWeight = FontWeight.Bold)
                Text(q.answer)
            }
            GameButton("Continue", { vm.continueAfterCard(q.id) }, style = ButtonStyle.PRIMARY)
        }
        return
    }

    val answered = picked != null
    val correct = picked == q.correct
    if (correct) {
        LaunchedEffect(Unit) {
            delay(CORRECT_PAUSE_MS)
            vm.answeredCorrectly(q.id)
        }
    }
    ModalBody {
        Kicker(Rules.beadLabel(position))
        Heading(q.question)
        if (picture != null) Portrait(picture, "Picture for the question", folder = "objects")
        choices.forEach { text ->
            val style = when {
                !answered -> ButtonStyle.NORMAL
                text == q.correct -> ButtonStyle.RIGHT
                text == picked -> ButtonStyle.WRONG
                else -> ButtonStyle.NORMAL
            }
            GameButton(text, onClick = {
                picked = text
                vm.cardsUsable = false
                vm.sounds.play(if (text == q.correct) Sound.QUESTION_RIGHT else Sound.QUESTION_WRONG)
                if (text != q.correct) vm.answeredWrongly()
            }, style = style, enabled = !answered)
        }
        if (answered) {
            Feedback(if (correct) "Correct!" else "Not quite.", correct)
            if (!correct) {
                AnswerNote { Text(q.answer) }
                GameButton("Next", { vm.nextQuestionSameBead() }, style = ButtonStyle.PRIMARY)
            }
        }
    }
}

// ---- Saint Challenge (spec 6) ----

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SaintChallengeContent(vm: GameViewModel, saintIndex: Int) {
    val saint = vm.data.saints[saintIndex]
    // Names are matched only against saints that can come up in this game, and always this one (spec 6.4).
    val inPlay = { i: Int -> i == saintIndex || vm.options.lesserSaints || !vm.data.saints[i].lesser }
    val countdown = remember { Countdown(Rules.SAINT_SECONDS) }
    var guess by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf<Boolean?>(null) }
    var shown by remember { mutableIntStateOf(1) }

    fun finish(won: Boolean) {
        if (outcome != null) return
        countdown.running = false
        outcome = won
        if (won) vm.saintWon(saintIndex)
    }
    RunCountdown(countdown, vm) { finish(false) }

    val result = outcome
    if (result != null) {
        ModalBody {
            Kicker("Saint Challenge")
            Portrait(saint.portrait, saint.name)
            Text(saint.name, fontSize = 23.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(
                if (result) "Well done! This saint's card has joined your hand." else "Not this time. No card was awarded.",
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            GameButton("Continue", { vm.continueAfterSaint() }, style = ButtonStyle.PRIMARY)
        }
        return
    }

    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    // The name is checked after every keystroke, and the challenge is won as soon as it would be accepted
    // (spec 6.3); there is no Submit button. Done on the keyboard explains why a guess is not accepted yet.
    fun accepted(text: String): Boolean {
        val j = Rules.judgeGuess(text, vm.data.saints, vm.data.equivalents, inPlay)
        if (j.result != GuessResult.ACCEPTED || j.saint != saintIndex) return false
        keyboard?.hide()
        focus.clearFocus()
        finish(true)
        return true
    }
    fun explain() {
        if (accepted(guess)) return
        feedback = when (Rules.judgeGuess(guess, vm.data.saints, vm.data.equivalents, inPlay).result) {
            GuessResult.IGNORED -> feedback
            GuessResult.VAGUE -> "More than one saint has that name. Be more specific."
            else -> "Not quite, try again."
        }
    }
    // Full screen while the challenge runs (spec 6.2): the clues the player has asked for, one more with
    // each Next Clue, then the buttons and the guess box at the bottom, above the keyboard.
    DisposableEffect(Unit) {
        vm.fullScreen = true
        onDispose { vm.fullScreen = false }
    }
    val newest = remember { BringIntoViewRequester() }
    LaunchedEffect(shown) { newest.bringIntoView() }
    // Every 5 seconds Next Clue shudders, as a reminder that more clues are there.
    val nudge = remember { Animatable(0f) }
    val beat = (countdown.elapsed / REMINDER_SECONDS).toInt()
    LaunchedEffect(beat) {
        if (beat == 0) return@LaunchedEffect
        // A name that fits more than one saint (such as "Ignatius") never wins by itself, so say so
        // without waiting for Done (spec 6.3).
        if (Rules.judgeGuess(guess, vm.data.saints, vm.data.equivalents, inPlay).result == GuessResult.VAGUE) {
            feedback = "More than one saint has that name. Be more specific."
        }
        if (shown < saint.clues.size) nudge.animateTo(0f, SHUDDER)
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetHead(vm, "Saint Challenge")
            if (saint.year.isNotEmpty()) {
                Text(buildAnnotatedString {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append("Born: "); pop(); append(saint.year)
                })
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                saint.clues.take(shown).forEachIndexed { i, clue ->
                    Row(if (i == shown - 1) Modifier.bringIntoViewRequester(newest) else Modifier) {
                        Text("${i + 1}.", modifier = Modifier.width(30.dp))
                        Text(clue, modifier = Modifier.weight(1f))
                    }
                }
            }
            Feedback(feedback, good = false)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameButton(
                    "Next Clue",
                    { shown++ },
                    modifier = Modifier.weight(1f).graphicsLayer { rotationZ = nudge.value },
                    enabled = shown < saint.clues.size,
                )
            }
            OutlinedTextField(
                value = guess,
                onValueChange = {
                    guess = it
                    if (!accepted(it)) feedback = ""
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Name the saint") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { explain() }),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.gold, unfocusedBorderColor = Palette.line, cursorColor = Palette.ink),
            )
            GameButton("Give Up", { finish(false) })
        }
        TimerBadge(countdown.label, countdown.low, Modifier.align(Alignment.TopEnd))
    }
}

// ---- Prayer Challenge and penance prayer (spec 8, 9.3) ----

@Composable
private fun PrayerContent(vm: GameViewModel, prayerIndex: Int, penance: Boolean) {
    var language by remember { mutableStateOf<String?>(null) }
    val position = vm.game?.position ?: 0
    val chosen = language
    if (chosen == null) {
        ModalBody {
            Kicker(if (penance) "The waters of Baptism" else Rules.beadLabel(position))
            Heading(if (penance) "Penance" else "Prayer Challenge")
            Text(prayerSize(vm.data.prayers[prayerIndex], penance), fontWeight = FontWeight.Bold)
            Text(
                if (penance) "Drag each line to where it belongs, or tap two lines to swap them, then tap Submit. When it is right, you will be restored to the fifth decade."
                else "Put the prayer in the right order before time runs out. Drag each line to where it belongs, or tap two lines to swap them.",
            )
            if (!penance) Text("Pray it in Latin to skip ahead five beads.", fontStyle = FontStyle.Italic)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameButton("English", { language = "english" }, modifier = Modifier.weight(1f))
                GameButton("Latin", { language = "latin" }, modifier = Modifier.weight(1f))
            }
        }
        return
    }

    val prayer = vm.data.prayers[prayerIndex].let { if (chosen == "latin") it.latin else it.english }
    val order = remember { mutableStateListOf<String>().apply { addAll(Rules.scramble(prayer.segments, kotlin.random.Random.Default)) } }
    var state by remember { mutableStateOf(PrayerState.ARRANGING) }
    var feedback by remember { mutableStateOf("") }
    val countdown = remember { Countdown(Rules.prayerSeconds(prayer.segments.size)) }

    // Spec 8.4: when time runs out or the player gives up, the prayer is shown in its right order.
    fun showAnswer(message: String) {
        countdown.running = false
        state = PrayerState.MISSED
        feedback = message
        order.clear()
        order.addAll(prayer.segments)
        vm.prayerMissed(penance)
    }
    DisposableEffect(Unit) {
        vm.fullScreen = true
        onDispose { vm.fullScreen = false }
    }
    if (!penance) {
        RunCountdown(countdown, vm) {
            if (state == PrayerState.ARRANGING) showAnswer("Time's up. Here is the prayer in the right order.")
        }
    }
    if (state == PrayerState.PASSED && !penance) {
        LaunchedEffect(Unit) { vm.gloryPrayerCompleted(latin = chosen == "latin") }
    }

    // Drag and drop, or tap two capsules to swap them (spec 8.2). Positions are in the capsule list's own
    // coordinates, which scrolling does not change: the held capsule is drawn dragOffset below its place, and
    // the finger is at grabY + dragOffset.
    var held by remember { mutableIntStateOf(-1) }
    var selected by remember { mutableIntStateOf(-1) }
    var grabY by remember { mutableFloatStateOf(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dropGap by remember { mutableIntStateOf(-1) }
    val bounds = remember { mutableMapOf<Int, Pair<Float, Float>>() }
    var listTop by remember { mutableFloatStateOf(0f) }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    val scroll = rememberScrollState()
    val shudder = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val edge = with(LocalDensity.current) { 48.dp.toPx() }
    val step = with(LocalDensity.current) { 8.dp.toPx() }

    // The gap the held capsule would drop into: before the first other capsule whose middle is below the
    // finger. The gaps on either side of the held capsule change nothing, so they show no line.
    fun findGap() {
        if (held < 0) {
            dropGap = -1
            return
        }
        val y = grabY + dragOffset
        var gap = order.size
        for (k in order.indices) {
            if (k == held) continue
            val (top, bottom) = bounds[k] ?: continue
            if (y < (top + bottom) / 2) {
                gap = k
                break
            }
        }
        dropGap = if (gap == held || gap == held + 1) -1 else gap
    }
    // A press becomes a drag once the finger has moved a little; any capsule picked with a tap is put back.
    fun grab(i: Int, y: Float, moved: Float) {
        selected = -1
        held = i
        grabY = (bounds[i]?.first ?: 0f) + y
        dragOffset = moved
        dropGap = -1
        findGap()
        scope.launch { shudder.animateTo(0f, SHUDDER) }
    }
    // After every move: a timed prayer passes as soon as it is in order.
    fun arranged() {
        feedback = ""
        if (!penance && Rules.isInOrder(order, prayer.segments)) {
            countdown.running = false
            state = PrayerState.PASSED
        }
    }
    fun drop() {
        val from = held
        val gap = dropGap
        held = -1
        dropGap = -1
        dragOffset = 0f
        if (gap < 0 || state != PrayerState.ARRANGING) return
        order.add(if (gap > from) gap - 1 else gap, order.removeAt(from))
        arranged()
    }
    // The first tap picks a capsule, tapping it again puts it back, and tapping another swaps the two.
    fun tap(i: Int) {
        if (state != PrayerState.ARRANGING) return
        val first = selected
        when (first) {
            -1 -> {
                selected = i
                scope.launch { shudder.animateTo(0f, SHUDDER) }
            }
            i -> selected = -1
            else -> {
                order[first] = order[i].also { order[i] = order[first] }
                selected = -1
                arranged()
            }
        }
    }
    // When the prayer is over, bring Continue (or Try another prayer) into view below a long list.
    LaunchedEffect(state) {
        if (state != PrayerState.ARRANGING) scroll.animateScrollTo(scroll.maxValue)
    }
    // Long prayers scroll while a capsule is held near the top or bottom edge.
    LaunchedEffect(held >= 0) {
        while (held >= 0) {
            val finger = listTop + grabY + dragOffset
            val delta = when {
                finger < viewport.top + edge -> -step
                finger > viewport.bottom - edge -> step
                else -> 0f
            }
            if (delta != 0f) {
                dragOffset += scroll.scrollBy(delta)
                findGap()
            }
            delay(16)
        }
    }

    ModalBody(
        timer = if (penance) null else countdown.label,
        timerLow = countdown.low,
        scroll = scroll,
        modifier = Modifier.onGloballyPositioned { viewport = it.boundsInRoot() },
    ) {
        SheetHead(vm, if (penance) "Penance" else Rules.beadLabel(position))
        Heading(prayer.title)
        // Side margins leave room to scroll with either thumb without grabbing a capsule.
        Box(Modifier.padding(horizontal = CAPSULE_SIDE_MARGIN)) {
            Column(
                Modifier.onGloballyPositioned { listTop = it.positionInRoot().y },
                verticalArrangement = Arrangement.spacedBy(CAPSULE_GAP),
            ) {
                order.forEachIndexed { i, text ->
                    val isHeld = i == held
                    val isSelected = i == selected && state == PrayerState.ARRANGING
                    Capsule(
                        text = text,
                        passed = state == PrayerState.PASSED,
                        held = isHeld || isSelected,
                        // The pointer handler and the bounds come before the layer, so moving the held
                        // capsule does not move the coordinates the finger is measured in.
                        modifier = Modifier
                            .pointerInput(i) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    if (state != PrayerState.ARRANGING) return@awaitEachGesture
                                    down.consume()
                                    val start = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                                    if (start == null) {
                                        // Let go before moving: a tap.
                                        if (currentEvent.changes.none { it.pressed }) tap(i)
                                        return@awaitEachGesture
                                    }
                                    grab(i, down.position.y, start.position.y - down.position.y)
                                    val completed = drag(start.id) { change ->
                                        dragOffset += change.positionChange().y
                                        change.consume()
                                        findGap()
                                    }
                                    if (completed) drop() else {
                                        held = -1
                                        dropGap = -1
                                        dragOffset = 0f
                                    }
                                }
                            }
                            .onGloballyPositioned { c -> bounds[i] = c.positionInParent().y.let { it to it + c.size.height } }
                            .zIndex(if (isHeld) 1f else 0f)
                            .graphicsLayer {
                                if (isSelected) rotationZ = shudder.value
                                if (isHeld) {
                                    translationY = dragOffset
                                    rotationZ = shudder.value
                                    scaleX = 1.02f
                                    scaleY = 1.02f
                                    shadowElevation = 8.dp.toPx()
                                    shape = RoundedCornerShape(50)
                                }
                            },
                    )
                }
            }
            if (dropGap >= 0) {
                val above = bounds[dropGap - 1]?.second
                val below = bounds[dropGap]?.first
                val gapPx = with(LocalDensity.current) { CAPSULE_GAP.toPx() }
                val y = when {
                    above != null && below != null -> (above + below) / 2
                    above != null -> above + gapPx / 2
                    below != null -> below - gapPx / 2
                    else -> null
                }
                if (y != null) {
                    // Wider than the capsules, so it is easy to see where the held one will land.
                    Box(
                        Modifier
                            .offset { IntOffset(0, (y - 2.dp.toPx()).roundToInt()) }
                            .layout { measurable, constraints ->
                                val overhang = DROP_LINE_OVERHANG.roundToPx()
                                val width = constraints.maxWidth + 2 * overhang
                                val line = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                                layout(constraints.maxWidth, line.height) { line.place(-overhang, 0) }
                            }
                            .height(4.dp)
                            .background(Palette.ink, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
        when (state) {
            PrayerState.ARRANGING -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameButton("Give Up", {
                        if (state == PrayerState.ARRANGING) showAnswer("Here is the prayer in the right order.")
                    }, modifier = Modifier.weight(1f))
                    if (penance) {
                        GameButton("Submit", {
                            if (Rules.isInOrder(order, prayer.segments)) {
                                vm.sounds.play(Sound.PENANCE_COMPLETE)
                                state = PrayerState.PASSED
                            } else {
                                feedback = "Not quite, keep trying."
                            }
                        }, modifier = Modifier.weight(1f), style = ButtonStyle.PRIMARY)
                    }
                }
                Feedback(feedback, good = false)
            }
            PrayerState.PASSED -> {
                Feedback(if (penance) "Correct! Your penance is complete." else "Well done!", good = true)
                GameButton("Continue", { if (penance) vm.penanceCompleted() else vm.continueAfterPrayer() }, style = ButtonStyle.PRIMARY)
            }
            PrayerState.MISSED -> {
                Feedback(feedback, good = false)
                GameButton("Try another prayer", { vm.tryAnotherPrayer() }, style = ButtonStyle.PRIMARY)
            }
        }
    }
}

private enum class PrayerState { ARRANGING, PASSED, MISSED }

private val FULL_SCREEN_MAX_WIDTH = 760.dp
private val CAPSULE_GAP = 5.dp
private val CAPSULE_SIDE_MARGIN = 24.dp
private val DROP_LINE_OVERHANG = 16.dp
private const val REMINDER_SECONDS = 5

/** The short shudder of a grabbed prayer capsule, also used to nudge Next Clue (spec 6.2, 8.2). */
private val SHUDDER: AnimationSpec<Float> = keyframes {
    durationMillis = 320
    -1.6f at 50
    1.6f at 110
    -1.1f at 175
    0.7f at 240
}

/** One prayer segment, dragged into place or swapped with another by tapping both (spec 8.2). */
@Composable
private fun Capsule(text: String, passed: Boolean, held: Boolean, modifier: Modifier) {
    val (background, border, textColor) = when {
        passed -> Triple(Palette.rightBg, Palette.right, Palette.right)
        held -> Triple(Palette.goldSoft, Palette.gold, Palette.ink)
        else -> Triple(Palette.button, Palette.line, Palette.ink)
    }
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (held) 2.dp else 1.dp, border), shape)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
    }
}

/** Spec 8.1: before choosing a language, say how many segments there are and how long the player will have. */
private fun prayerSize(prayer: Prayer, penance: Boolean): String {
    val en = prayer.english.segments.size
    val la = prayer.latin.segments.size
    if (penance) {
        return (if (en == la) "This prayer has $en segments." else "This prayer has $en segments in English and $la in Latin.") +
            " There is no time limit."
    }
    return if (en == la) {
        "This prayer has $en segments. You will have ${Rules.prayerSeconds(en)} seconds to put them in order."
    } else {
        "In English this prayer has $en segments and you will have ${Rules.prayerSeconds(en)} seconds. " +
            "In Latin it has $la segments and you will have ${Rules.prayerSeconds(la)} seconds."
    }
}

// ---- The Cross: Moral Dilemma (spec 9) ----

@Composable
private fun DilemmaContent(vm: GameViewModel, dIndex: Int) {
    val d = vm.data.dilemmas[dIndex]
    val choices = remember { listOf(d.right, d.wrong).shuffled() }
    var picked by remember { mutableStateOf<String?>(null) }
    ModalBody {
        Kicker("The Cross")
        Heading(d.title)
        Text(d.dilemma)
        Text(d.question, fontWeight = FontWeight.Bold)
        choices.forEach { text ->
            val style = when {
                picked == null -> ButtonStyle.NORMAL
                text == d.right -> ButtonStyle.RIGHT
                text == picked -> ButtonStyle.WRONG
                else -> ButtonStyle.NORMAL
            }
            GameButton(text, onClick = {
                picked = text
                vm.dilemmaAnswered(d.id, text == d.right)
            }, style = style, enabled = picked == null)
        }
        val choice = picked
        if (choice != null) {
            val right = choice == d.right
            Feedback(
                if (right) "That is the right choice." else "That is not the right choice. Go to the confessional by the waters of Baptism to receive the Sacrament of Reconciliation and do penance.",
                good = right,
            )
            Teaching(d)
            Spacer(Modifier.height(4.dp))
            GameButton("Continue", { vm.continueAfterDilemma(right) }, style = ButtonStyle.PRIMARY)
        }
    }
}

/** The teaching shown after every dilemma choice and in the Codex (spec 9.2). */
@Composable
fun Teaching(d: Dilemma) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TeachingPart("The Catholic answer", d.catholicAnswer)
        TeachingPart("Key principle", d.keyPrinciple)
        TeachingPart("Catechism references", d.ccc)
        Text("Sources", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        Text(linkified(d.sources))
    }
}

@Composable
private fun TeachingPart(title: String, body: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
    Text(body)
}

/** Plain text with any URLs as tappable links (spec 1). */
private fun linkified(text: String) = buildAnnotatedString {
    var last = 0
    val style = TextLinkStyles(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF6B4A06), textDecoration = TextDecoration.Underline))
    for (m in Regex("https?://[^\\s)\\]]+[^\\s)\\].,;:]").findAll(text)) {
        append(text.substring(last, m.range.first))
        withLink(LinkAnnotation.Url(m.value, style)) { append(m.value) }
        last = m.range.last + 1
    }
    append(text.substring(last))
}
