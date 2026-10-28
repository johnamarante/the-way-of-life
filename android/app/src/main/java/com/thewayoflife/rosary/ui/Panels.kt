package com.thewayoflife.rosary.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thewayoflife.rosary.BuildConfig
import com.thewayoflife.rosary.data.Dilemma
import com.thewayoflife.rosary.data.Saint
import com.thewayoflife.rosary.game.ModalSize
import com.thewayoflife.rosary.game.TextSize
import java.text.Collator
import java.util.Locale

/** A centered card over the dimmed board, used by every non-game screen. */
@Composable
private fun Panel(scrollable: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.parchment.copy(alpha = 0.55f))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Palette.paper,
            border = BorderStroke(1.dp, Palette.line),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = (if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) { content() }
        }
    }
}

@Composable
private fun Title(text: String) = Text(text, fontSize = 32.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

@Composable
private fun Subtitle(text: String) =
    Text(text, fontStyle = FontStyle.Italic, color = Palette.inkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

@Composable
private fun SectionTitle(text: String) = Text(text, fontSize = 24.sp)

// ---- Title screen (spec 1.3): the whole board, the only time it is all in view ----

/** Soft white words over the board that fade out and back like a dimmer, leaving the board bare for a while. */
@Composable
fun TitleScreen(onOpen: () -> Unit) {
    val dimmer = rememberInfiniteTransition(label = "title")
    val alpha = dimmer.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 6400
                1f at 0
                1f at 2200
                0f at 3400
                0f at 5200
                1f at 6400
            },
        ),
        label = "dimmer",
    )
    val style = TextStyle(
        color = Color(0xFFFFFBF2),
        textAlign = TextAlign.Center,
        shadow = Shadow(Color(0xD91E1204), Offset(0f, 3f), blurRadius = 14f),
    )
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = "Open the main menu", onClick = onOpen),
    ) {
        Text(
            "The Way of Life",
            style = style.copy(fontSize = 32.sp),
            modifier = Modifier.align(BiasAlignment(0f, -1f / 3)).padding(horizontal = 16.dp).graphicsLayer { this.alpha = alpha.value },
        )
        Text(
            "Tap for Main Menu",
            style = style.copy(fontSize = 19.sp, fontStyle = FontStyle.Italic),
            modifier = Modifier.align(BiasAlignment(0f, 1f / 3)).padding(horizontal = 16.dp).graphicsLayer { this.alpha = alpha.value },
        )
    }
}

/** The app version, shown at the bottom of the main menu. */
@Composable
fun VersionLabel(modifier: Modifier = Modifier) {
    Text(
        "Version ${BuildConfig.VERSION_NAME}",
        fontSize = 14.sp,
        color = Palette.inkSoft,
        modifier = modifier
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(50))
            .background(Palette.paper.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 2.dp),
    )
}

@Composable
fun MenuPanel(vm: GameViewModel, onQuit: () -> Unit) = Panel {
    Title("The Way of Life")
    Column(Modifier.widthIn(max = 320.dp).align(Alignment.CenterHorizontally).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GameButton("Start Game", { vm.requestNewGame() }, style = ButtonStyle.PRIMARY)
        GameButton("Resume Game", { vm.resumeGame() }, enabled = vm.hasSave)
        GameButton("Options", { vm.show(Screen.OPTIONS) })
        GameButton("Codex", { vm.show(Screen.CODEX) })
        GameButton("About", { vm.show(Screen.ABOUT) })
        GameButton("Quit", onQuit)
    }
}

@Composable
fun ConfirmNewGamePanel(vm: GameViewModel) = Panel {
    SectionTitle("Start a new game?")
    Text("Your saved game will be replaced.")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GameButton("Cancel", { vm.show(Screen.MENU) }, modifier = Modifier.weight(1f))
        GameButton("Start New Game", { vm.startGame() }, modifier = Modifier.weight(1f), style = ButtonStyle.PRIMARY)
    }
}

@Composable
fun OptionsPanel(vm: GameViewModel) = Panel {
    val options = vm.options
    SectionTitle("Options")
    Text("Text size:")
    TextSize.entries.forEach { size ->
        OptionRow(size.label, options.textSize == size) { vm.updateOptions(options.copy(textSize = size)) }
    }
    Text("Challenge window size:", modifier = Modifier.padding(top = 8.dp))
    ModalSize.entries.forEach { size ->
        OptionRow(size.label, options.modalSize == size) { vm.updateOptions(options.copy(modalSize = size)) }
    }
    Text("Catechism questions:", modifier = Modifier.padding(top = 8.dp))
    listOf(false to "Normal questions only", true to "Include advanced questions from the Catechism of the Catholic Church").forEach { (value, label) ->
        OptionRow(label, options.advancedQuestions == value) { vm.updateOptions(options.copy(advancedQuestions = value)) }
    }
    Text("Prayers:", modifier = Modifier.padding(top = 8.dp))
    val easy = vm.data.prayers.filter { it.easy }.joinToString(", ") { it.english.title.removePrefix("The ") }
    listOf(false to "All prayers", true to "Easy prayers only: $easy").forEach { (value, label) ->
        OptionRow(label, options.easyPrayers == value) { vm.updateOptions(options.copy(easyPrayers = value)) }
    }
    Text("Saints:", modifier = Modifier.padding(top = 8.dp))
    listOf(false to "Well-known saints only", true to "Include lesser-known saints").forEach { (value, label) ->
        OptionRow(label, options.lesserSaints == value) { vm.updateOptions(options.copy(lesserSaints = value)) }
    }
    Text("Moral dilemmas at the Cross:", modifier = Modifier.padding(top = 8.dp))
    listOf(false to "All Ages only", true to "Include Ages 13 and up").forEach { (value, label) ->
        OptionRow(label, options.includeOlder == value) { vm.updateOptions(options.copy(includeOlder = value)) }
    }
    GameButton("Back", { vm.show(Screen.MENU) }, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Palette.gold))
        Text(label, modifier = Modifier.padding(start = 10.dp))
    }
}

private const val PRIVACY_URL = "https://johnamarante.github.io/the-way-of-life/privacy.html"

@Composable
fun AboutPanel(vm: GameViewModel) = Panel {
    SectionTitle("About")
    Text("The Way of Life is a game of the Catholic faith. Climb the rosary from the waters of Baptism to the Cross: answer catechism questions, name the saints, pray in English or Latin, and face a moral choice at the summit.")
    val linkStyle = TextLinkStyles(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF6B4A06), textDecoration = TextDecoration.Underline))
    Text(buildAnnotatedString {
        append("The game collects no personal data. ")
        withLink(LinkAnnotation.Url(PRIVACY_URL, linkStyle)) { append("Privacy policy") }
    })
    Text("Portrait credits (Creative Commons images from Wikimedia Commons):")
    vm.data.credits.forEach { c ->
        Text(buildAnnotatedString {
            append("${c.saint}: ")
            withLink(LinkAnnotation.Url(c.url, linkStyle)) { append(c.image) }
            append(" (${c.license})")
        }, fontSize = 15.sp)
    }
    GameButton("Back", { vm.show(Screen.MENU) }, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun VictoryPanel(vm: GameViewModel) = Panel {
    Title("Victory")
    Subtitle("You have reached the Cross.")
    Text(
        "Through prayer, the help of the saints, and the teaching of the Church, you have climbed the way of life.",
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
    Text(vm.victoryScore, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    if (vm.victoryPerfect) Text("You made no mistakes along the way.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    vm.victoryCodex.forEach { Text(it, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
    Text("Ave Maria", fontStyle = FontStyle.Italic, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    GameButton("Main Menu", { vm.show(Screen.MENU) }, style = ButtonStyle.PRIMARY)
}

// ---- Codex (spec 10) ----

private fun codexName(name: String) = name.removePrefix("St. ")

private enum class CodexTab(val label: String) { PRAYERS("Prayers"), SAINTS("Saints"), QUESTIONS("Questions"), DILEMMAS("Dilemmas") }

@Composable
fun CodexPanel(vm: GameViewModel) = Panel(scrollable = false) {
    @Suppress("UNUSED_VARIABLE") val version = vm.codexVersion
    var tab by rememberSaveable { mutableStateOf(CodexTab.PRAYERS) }
    // Prayers, questions and the all-ages dilemmas are always open, for learning; saints open as they are won,
    // and the rest with the victories in spec 10.
    val codex = vm.codex
    val data = vm.data
    fun saintOpen(s: Saint) = codex.allSaints || s.name in codex.saints
    fun dilemmaOpen(d: Dilemma) = d.allAges || codex.olderDilemmas || d.id in codex.dilemmas
    // Saints in alphabetical order, without the "St." every entry would otherwise start with (spec 10).
    val saintsByName = remember(data) {
        val collator = Collator.getInstance(Locale.ENGLISH).apply { strength = Collator.PRIMARY }
        data.saints.sortedWith(compareBy(collator) { codexName(it.name).removePrefix("The ") })
    }
    SectionTitle("Codex")
    // One section at a time, picked from a drop-down list.
    var picking by remember { mutableStateOf(false) }
    Box {
        GameButton("${tab.label}  ▾", { picking = true })
        DropdownMenu(expanded = picking, onDismissRequest = { picking = false }, containerColor = Palette.paper) {
            CodexTab.entries.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.label, fontSize = 18.sp, fontWeight = if (t == tab) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        tab = t
                        picking = false
                    },
                )
            }
        }
    }
    val count = when (tab) {
        CodexTab.PRAYERS -> null
        CodexTab.SAINTS -> "${data.saints.count(::saintOpen)} of ${data.saints.size} saints"
        CodexTab.QUESTIONS -> "${data.questions.count { !it.advanced }} questions, ${data.questions.count { it.advanced }} advanced questions"
        CodexTab.DILEMMAS -> "${data.dilemmas.count(::dilemmaOpen)} of ${data.dilemmas.size} dilemmas"
    }
    if (count != null) Text(count, fontStyle = FontStyle.Italic, color = Palette.inkSoft)
    LazyColumn(Modifier.weight(1f, fill = false).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (tab) {
            CodexTab.PRAYERS -> itemsIndexed(data.prayers) { _, p ->
                Entry(open = true, title = p.english.title) {
                    Text("English", fontWeight = FontWeight.Bold)
                    p.english.segments.forEach { Text(it) }
                    Text("Latin: ${p.latin.title}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    p.latin.segments.forEach { Text(it) }
                }
            }
            CodexTab.SAINTS -> itemsIndexed(saintsByName) { _, s ->
                val open = saintOpen(s)
                Entry(open = open, title = if (open) codexName(s.name) else "Locked saint", portrait = s.portrait.takeIf { open }) {
                    if (s.year.isNotEmpty()) Text("Born: ${s.year}", fontStyle = FontStyle.Italic)
                    s.clues.forEachIndexed { i, clue -> Text("${i + 1}. $clue") }
                }
            }
            CodexTab.QUESTIONS -> {
                val (advanced, normal) = data.questions.partition { it.advanced }
                itemsIndexed(normal) { _, q -> Entry(open = true, title = q.question) { Text(q.answer) } }
                if (advanced.isNotEmpty()) item { Text("Advanced questions", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                itemsIndexed(advanced) { _, q -> Entry(open = true, title = q.question) { Text(q.answer) } }
            }
            CodexTab.DILEMMAS -> itemsIndexed(data.dilemmas) { _, d ->
                val open = dilemmaOpen(d)
                Entry(open = open, title = if (open) d.title else "Locked dilemma") {
                    Text(d.dilemma)
                    Text(d.question, fontWeight = FontWeight.Bold)
                    Text("Right choice: ${d.right}", fontStyle = FontStyle.Italic)
                    Teaching(d)
                }
            }
        }
    }
    GameButton("Back", { vm.show(Screen.MENU) })
}

@Composable
private fun Entry(open: Boolean, title: String, portrait: String? = null, body: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.button)
            .border(1.dp, Palette.line.copy(alpha = 0.6f), shape)
            .clickable(enabled = open) { expanded = !expanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (portrait != null) {
                val image = rememberPortrait(portrait)
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Palette.line)) {
                    if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.size(40.dp))
                }
            }
            Text(title, fontStyle = if (open) FontStyle.Normal else FontStyle.Italic, color = if (open) Palette.ink else Palette.inkSoft)
        }
        if (expanded && open) body()
    }
}
