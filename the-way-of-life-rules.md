# The Way of Life: Game Rules and Behavior Specification

Single-player Catholic catechism board game for Android and browser. This document defines the rules and game logic. Board rendering and bead indexing over the board image are already implemented and are not covered here, except where game logic depends on bead positions.

The app is called **The Way of Life**. It is a standalone single-player game, distinct from the tabletop game The Purgative Way.

---

## 1. Data Files

All content comes from five CSV files in the repo root. Treat each as read-only content loaded at startup. The Android app copies them into its assets; the browser version's build script (`browser/build.py`) embeds them into `index.html` so the page stays a single file.

| File | Rows | Columns used |
|---|---|---|
| `St_Joseph_First_Communion_Catechism_Quiz.csv` | 93 (normal questions) | `Number`, `Question`, `Answer`, `Answer A`, `Answer B`, `Answer C`, `Answer D`, `Correct Answer` |
| `Catechism_Advanced_Questions.csv` | 289 (advanced questions) | The same columns, plus `CCC References`. `Number` starts at 1001 so it never collides with a normal question. |
| `Catholic_Saints_Trivia.csv` | 113 | `Saint`, `Year Born`, `Clue 1` through `Clue 10`, `Accepted Names`, `Accepted Spellings`, `Invocation`, `Portrait` |
| `Catholic_Prayers_Segments.csv` | 30 (15 prayers x English/Latin) | `Title`, `Language`, `EasyPrayer` (`TRUE` or `FALSE`, the same for both languages of a prayer; Section 11.2), `Segment 1` through `Segment 20` (unused trailing segments are empty) |
| `Catholic_Moral_Dilemmas.csv` | 34 | `Number`, `Title`, `Age Level`, `Dilemma`, `Question`, `Right Choice`, `Wrong Choice`, `Catholic Answer`, `Key Principle`, `CCC References`, `Sources` |

Notes on the data:

- Catechism questions are either 4-option multiple choice or True/False. A question is True/False when `Answer C` and `Answer D` are empty; `Answer A` is `TRUE` and `Answer B` is `FALSE`. Display these as "True" and "False".
- `Correct Answer` is a letter referring to the original column. Because options are shuffled (Section 4), track the correct option by its content, not its letter.
- `Year Born` is a display string (for example `c. 50 BC`, `early 1st c.`, `1225`). Show it as-is; do not parse it. It is empty for the three archangels (Michael, Gabriel, Raphael), who were never born; when it is empty, show no birth year at all (no label, no placeholder).
- English and Latin rows of the same prayer are adjacent in the file. Pair them by row order (row 2n is English, row 2n+1 is its Latin counterpart). Segment counts can differ between the two languages (the Act of Contrition has 11 in English and 12 in Latin).
- `Age Level` is either `All Ages` or `Ages 13 and up`.
- `Sources` may contain one or more URLs mixed with text, or no URL at all. Render any URLs as tappable links.

### 1.1 Required additions to the saints data

Two columns in `Catholic_Saints_Trivia.csv` support name matching (Section 6.4). Both are optional per row and use `;` to separate multiple values. They have been filled in: 71 saints have accepted names and 21 have accepted spellings. Every name and spelling was checked with the matching rules so that it resolves to its own saint and no other.

- `Accepted Names`: alternate full names that count as a correct guess. Example for The Blessed Virgin Mary: `Mary;Virgin Mary;Blessed Virgin;Blessed Mother;Our Lady;Mother of God;Mother Mary;Madonna;Mary Mother of Jesus`. Others include John Paul II (`John Paul 2;Pope John Paul;Karol Wojtyla`...), John XXIII (`John 23;Pope John`...), Peter (`Simon Peter;Cephas`...), and Nicholas of Myra (`Saint Nick;Santa Claus`...). Because Peter's accepted names include `Simon Peter`, the guess "Simon" alone is too vague (Peter or Simon the Zealot).
- `Accepted Spellings`: common misspellings of individual name words. Example for Charles Borromeo: `Boromeo;Borromio`.

Two more columns support saint cards (Section 7), also filled in:

- `Invocation`: the text shown when the card is used. If empty, use `Saint {Display Name}, pray for me!`, where the display name is the `Saint` column without the leading `St. `. Five saints have an override: The Blessed Virgin Mary (`Holy Mary, Mother of God, pray for me!`) and the four saints whose names contain a parenthetical (Acacius, Erasmus, Pio of Pietrelcina, Teresa of Calcutta), so the parentheses never appear in the prayer.
- `Portrait`: filename of the saint's portrait image, filled in for all 113 (for example `teresa_of_avila.jpg`). The 256 x 256 JPEG images are in the `portraits/` folder; `saint_portrait_sources.md` lists where each one came from and its license.

### 1.2 Required new data file: name equivalents

`Saint_Name_Equivalents.csv` holds one group of interchangeable first names per row. This stops a guess from matching one saint when the name is really a translation of another saint's name. Equivalents are used only by the unique-subset rule, never by the exact rule (Section 6.4).

Starting content:

```
Group
charles;carlo;carlos;carl
john;juan;giovanni;jean
```

Examples of why this is needed:

- "Charles" alone must not be accepted for Charles Borromeo, because Charles and Carlo are the same name and St. Carlo Acutis is also in the bank.
- "Juan" alone must not be accepted as St. John the Apostle. It is too vague: it could mean Juan Diego or any of the saints named John.

### 1.3 Presentation

- There are no arrow buttons; the player's position changes only through the game.
- **Title screen:** the app opens on the whole board, the only time all of it is in view. "The Way of Life" is written over the board in soft white about a third of the way down the screen, and "Tap for Main Menu" (Android) or "Click for Main Menu" (browser) in a smaller size about two thirds of the way down. On Android the words fade out and back in like a dimmer switch, leaving the board unobstructed for a couple of seconds each time. In the browser they disappear while the mouse is outside the window and come back when it returns. Tapping or clicking anywhere opens the main menu.
- **Board view:** everywhere else (menu, game, other screens) the board is zoomed in (1.8 times the whole-board size) and centered on the player's current bead, moving smoothly as the player moves, without showing past the board's edges. The bead is placed in the part of the screen the challenge modal leaves open.
- Every challenge (catechism question, Saint Challenge, Prayer Challenge, Moral Dilemma, penance) is plain text with simple buttons on a modal over the board, with its timer, if it has one, in the modal's top corner. The modal opens at the top of the screen when the bead is in the lower half of the board, and at the bottom otherwise. The one exception is arranging a prayer, which takes the whole screen (Section 8.2).
- Text is small by default and the modal is compact, to leave more of the board visible. Both can be changed in Options (Section 11.2).
- All text uses Times New Roman, or the closest available serif font (`"Times New Roman", Times, serif` in the browser; the system serif font on Android).

---

## 2. The Board

The board image is a rosary path climbing a mountain, from the waters of Baptism at the bottom to the Cross at the top, with the Blessed Virgin Mary guiding the way. Unity with the Cross is the goal of the game.

### 2.1 Bead layout (58 beads)

| Section | Beads | Challenge on that bead |
|---|---|---|
| Decade 1 | 10 gray beads | Catechism question |
| Glory bead 1 | 1 gold bead | Prayer challenge |
| Decade 2 | 10 gray beads | Catechism question |
| Glory bead 2 | 1 gold bead | Prayer challenge |
| Decade 3 | 10 gray beads | Catechism question |
| Glory bead 3 | 1 gold bead | Prayer challenge |
| Decade 4 | 10 gray beads | Catechism question |
| Glory bead 4 | 1 gold bead | Prayer challenge |
| Decade 5 | 10 gray beads | Catechism question |
| Glory bead 5 | 1 gold bead | Prayer challenge |
| Final beads | 3 gray beads | Catechism question |
| The Cross | (goal, not a bead) | Moral dilemma |

Logical indexing (0-based). This matches the bead order in `rosary_beads.json` exactly (verified: the gold beads are at indexes 10, 21, 32, 43, and 54, and the final beads are 55 to 57):

- Decade `d` (1 to 5), bead `b` (1 to 10): index `(d - 1) * 11 + (b - 1)`
- Glory bead after decade `d`: index `(d - 1) * 11 + 10`
- Final beads `f` (1 to 3): index `54 + f` (55, 56, 57)
- The Cross: index 58 (terminal state)

### 2.2 The Cross position

The Cross is not a bead, so it is not in the `"beads"` list. The game needs a spot to show the player arriving at the Cross. Measured on `rosary_board_4x_complete_latest.png` (2392 x 3840), using the same normalized units as the bead map:

| | x | y | r |
|---|---|---|---|
| Center of the Cross (where the crossbar meets the upright) | 0.7157 | 0.0326 | 0.065 |

This is stored in `rosary_beads.json` (and its copy in the Android assets) as a separate top-level `"cross"` object with `index` 58, outside `"beads"`, so the bead count stays 58. The current Android and browser loaders read only the keys they need, so they ignore it until the game code uses it. When the player reaches the Cross, light it with the same glow as a bead, scaled to `r`. If the board art changes, re-measure this point along with the beads.

### 2.3 Decades for saint card purposes

For saint card usage (Section 7), there are six "decades":

- Decades 1 to 5: the ten gray beads of each decade, plus the Glory bead that follows it.
- Decade 6: the three final beads.

A new decade begins when the player lands on any gray bead of the next decade (normally bead 1, or bead 6 after a Latin bonus).

---

## 3. Game Flow Overview

1. The player starts on Decade 1, bead 1 (the bead nearest the waters of Baptism).
2. On each gray bead, a catechism question modal appears. A correct answer moves the player to the next bead.
3. Every 5 correct answers in a row triggers a Saint Challenge. Winning it adds that saint's card to the player's hand.
4. On each gold Glory bead, a Prayer Challenge modal appears. Completing it moves the player into the next decade; completing it in Latin skips 5 beads.
5. After the third final bead is answered correctly, the player reaches the Cross and faces the Moral Dilemma.
6. Choosing correctly wins the game. Choosing wrongly sends the player back to Decade 1, bead 1, to do penance (an untimed prayer challenge), after which they are restored to a random bead in Decade 5.

### 3.1 Score

The in-game bar shows the player's score for this game under the saint cards:

`Questions: n/m · Prayer challenge: x/y`

- **Questions:** `n` catechism questions answered correctly out of `m` answered. A wrong answer adds to `m` only. A question passed with a saint card counts toward neither number. A question left open by going back to the menu is not counted.
- **Prayer challenge:** `x` Prayer Challenges completed out of `y` attempted on the Glory beads. Running out of time adds to `y` only. The penance prayer is not counted.
- The score is saved with the game, along with whether the player has chosen wrongly at the Cross in this game.
- A perfect game has every question and every Prayer Challenge right (`n = m` and `x = y`) and no wrong choice at the Cross. Winning a perfect game can open more of the Codex (Section 10).

---

## 4. Catechism Questions (gray beads)

### 4.1 Selection

- There are two sets of questions:
  - **Normal questions:** the 93 in `St_Joseph_First_Communion_Catechism_Quiz.csv`. These are the 83 from the St. Joseph First Communion Catechism (revised in version 0.1.0 after a doctrinal review), plus 10 on the Mass and the Eucharist.
  - **Advanced questions:** the 289 in `Catechism_Advanced_Questions.csv`, written from the Catechism of the Catholic Church. Every answer cites its CCC paragraph.
- By default only normal questions are asked. With **Advanced questions** on (Section 11.2), both sets are mixed together at random.
- The game keeps one shuffled deck of every question and draws from it without repeats, skipping advanced questions unless they are on. When the deck runs out, it is reshuffled and play continues. A deck saved before the question bank changed size is replaced with a new one.
- Every question shown counts as used, whether it was answered correctly, answered wrongly, or passed with a saint card.

### 4.2 Presentation

- Show the question text and its answer options as buttons.
- Multiple choice: shuffle the four options every time the question is shown.
- True/False: always show True first, then False. Do not shuffle.
- Any saint cards available for this decade are tappable while the question is unanswered (Section 7).

### 4.3 Correct answer

- Show brief "Correct!" feedback, then close the modal and move the player one bead forward.
- Increase the streak by 1 (Section 5).
- Record the question as answered correctly in the Codex (Section 10).

### 4.4 Wrong answer

- There is no penalty beyond not advancing and losing the streak.
- Highlight the player's wrong choice and show the correct answer, including the full `Answer` sentence from the CSV.
- Show a **Next** button. Tapping it draws a new random question for the same bead.
- Reset the streak to 0.

---

## 5. The Streak

- The streak counts consecutive correct catechism answers.
- A wrong answer resets it to 0.
- Using a saint card leaves it unchanged (neither increases nor resets).
- It carries across beads, decades, and Glory beads; prayer challenges do not affect it.
- Failing the Moral Dilemma and doing penance do not reset it. The player keeps their streak when restored to Decade 5.
- When the streak reaches 5, a Saint Challenge triggers and the streak resets to 0 (whether the challenge is won, failed, or given up).
- Optional UI suggestion: a small 5-pip streak indicator so the player can see progress toward the next challenge.

### 5.1 Order of events when a challenge triggers

When the 5th correct answer occurs: show the "Correct!" feedback, move the player to the next bead, then open the Saint Challenge. When the challenge closes, continue with whatever the new bead requires (a catechism question, a Prayer Challenge on a Glory bead, or the Moral Dilemma at the Cross).

---

## 6. Saint Challenge

### 6.1 Saint selection

Pick a random saint from the 113 that the player does not currently hold a card for in this game. If the player somehow holds all 113, pick any saint at random.

### 6.2 Timer, layout and clue reveal

- Total time limit: 60 seconds.
- The challenge takes the whole screen, covering the board and the in-game bar, like an arranged prayer (Section 8.2). The timer is in the top corner, and a menu button sits beside the "Saint Challenge" heading. The result (Section 6.5) shows in the normal challenge window.
- From top to bottom the screen shows:
  1. The saint's `Year Born`, as "Born: ...", if it has one (the archangels do not).
  2. The clues revealed so far, numbered. At the start only `Clue 1` is shown; no number appears for a clue not yet revealed.
  3. Any feedback on the last guess.
  4. **Next Clue**, the full width of the screen. There is no Submit button (Section 6.3).
  5. The text field for the saint's name.
  6. **Give Up**, directly below the text field.
- Clues are never revealed by the clock. Each tap of Next Clue adds the next clue, with its number, in CSV order. Revealed clues stay visible for the rest of the challenge.
- Every 5 seconds on the timer, while clues remain, the Next Clue button gives the same short shudder as a grabbed prayer capsule (Section 8.2), to remind the player that more clues are there.
- At the same moments (every 5 seconds, even after all clues are shown), if the name in the field fits more than one saint, the red message "More than one saint has that name. Be more specific." appears. For example, "Ignatius" fits both St. Ignatius of Antioch and St. Ignatius of Loyola. A player waiting on such a name learns why nothing happened without having to press Enter or Done.
- On a touch screen the keyboard does not open by itself when the challenge starts, since it would cover half the screen; it opens when the player taps the name field. In the browser, the full-screen challenge then shrinks to the part of the screen the keyboard leaves visible (on iPhone Safari as well as Android Chrome), so the field and buttons are never hidden behind it.
- When the clues do not fit (for example, with the phone keyboard open), they scroll between the birth year and the buttons, which stay at the bottom above the keyboard. The newest clue is scrolled into view.

### 6.3 Controls

- **Text field** for the guess (device keyboard or on-screen keyboard). There is no Submit button: after every keystroke the game checks what is in the field (Section 6.4), and as soon as it would be accepted as this saint's name, the challenge is won at once. The keyboard closes and the saint card is awarded. For example, typing "Dominic" for St. Dominic wins the moment the "c" is typed.
- Pressing Enter (browser) or Done (phone keyboard) on a name that is not accepted shows "Not quite, try again," or "More than one saint has that name. Be more specific." The message clears when the player types again. The player may guess as many times as they like within the time limit. Wrong guesses carry no penalty, and a name that matches a different saint does not end the challenge.
- **Next Clue** button: reveals the next clue. It does not change the timer. Disabled once Clue 10 is shown.
- **Give Up** button: ends the challenge at once as a failure, without waiting for the timer.

### 6.4 Name matching

The goal is to be forgiving about casing, spelling, and partial names, but to require a guess specific enough that it cannot mean a different saint in the bank.

**Normalization** (applied to guesses and to every name variant):

1. Lowercase.
2. Remove accents (for example Ávila becomes avila, Thérèse becomes therese).
3. Replace punctuation, including periods, apostrophes, commas, and parentheses, with spaces.
4. Split into words and drop the filler words `st`, `saint`, `of`, `the`, `and`.
5. Do not drop `de` (it is meaningful in names like Vincent de Paul).

**Name variants for each saint:**

- The `Saint` column with any parenthetical removed (for example `St. Teresa of Calcutta (Mother Teresa)` gives `teresa calcutta`).
- The parenthetical text, if any, as a separate variant (`mother teresa`, `elmo`, `acacius`, `pio pietrelcina`).
- For names with a parenthetical, the whole `Saint` column with only the parentheses characters removed (`St. Padre Pio (Pio of Pietrelcina)` gives `padre pio pio pietrelcina`), so a player who types the name exactly as displayed is accepted.
- Each entry in the `Accepted Names` column.

**Word matching:** a guess word matches a name word if any of these hold:

- They are identical.
- The guess word is listed in `Accepted Spellings` for that saint and corresponds to the name word.
- Both words are in the same group in `Saint_Name_Equivalents.csv`. This condition applies only in the unique-subset rule (step 3 below), not in the exact rule.
- The name word has 5 or more letters and the two words differ by at most one letter (one insertion, deletion, or substitution).

**Decision procedure:**

1. Normalize the guess. If it is empty after normalization, ignore it.
2. **Exact rule:** if the guess's words match, in order and with nothing left over, all the words of a variant of exactly one saint, accept that saint. Name equivalents are not used here. This is what lets "Paul" mean St. Paul (not John Paul II), "John" mean St. John, "Peter" mean St. Peter, and "Mary" mean the Blessed Virgin Mary (via `Accepted Names`) rather than Mary Magdalene.
3. **Unique-subset rule:** otherwise, find every saint that has at least one variant containing a match for every word of the guess. If exactly one saint qualifies, accept that saint.
4. Otherwise the guess is not accepted.

**Feedback:**

- Accepted and it is the target saint: challenge won (Section 6.5).
- Accepted but it is a different saint: show "Not quite, try again."
- Two or more saints qualify: show "More than one saint has that name. Be more specific."
- No saint qualifies: show "Not quite, try again."

**Worked examples** (target in parentheses):

| Guess | Result | Why |
|---|---|---|
| `ignatius loyola`, `Ignatius of Loyola`, `iGNATIUS lOYOLA` | Accepted (Ignatius of Loyola) | Unique subset |
| `ignatius` | Too vague | Matches Ignatius of Antioch and Ignatius of Loyola |
| `augustine` | Accepted (Augustine of Hippo) | Only one Augustine |
| `bernadette` | Accepted (Bernadette Soubirous) | Only one Bernadette |
| `teresa` | Too vague | Teresa of Ávila and Teresa of Calcutta |
| `borromeo` or `boromeo` | Accepted (Charles Borromeo) | Unique; one-letter tolerance |
| `charles` or `carlos` | Too vague | Name equivalents match Charles Borromeo and Carlo Acutis |
| `acutis` | Accepted (Carlo Acutis) | Unique |
| `paul` | Accepted (Paul) | Exact rule beats John Paul II |
| `mary` | Accepted (Blessed Virgin Mary) | Exact rule via Accepted Names |
| `mother teresa` | Accepted (Teresa of Calcutta) | Parenthetical alias |
| `Padre Pio (Pio of Pietrelcina)` | Accepted (Padre Pio) | Whole displayed name |
| `juan` or `jean` | Too vague | Equivalents are not used by the exact rule; the subset rule matches Juan Diego and every John |
| `juan diego` | Accepted (Juan Diego) | Unique subset |

These examples were checked against the real saints list, including the filled-in `Accepted Names` and `Accepted Spellings` columns, with a direct implementation of this section.

### 6.5 Outcome

- **Won:** show a celebration with the saint's name and portrait, add the saint card to the player's hand (Section 7), and record the saint in the Codex.
- **Time runs out or Give Up:** reveal the saint's name. No penalty, but no card is awarded and the saint is not added to the Codex.
- In both cases, a **Continue** button closes the modal and the game resumes.

---

## 7. Saint Cards

### 7.1 Display

- Each held card appears as a small square showing the saint's face in a bar along the top of the game screen.
- Cards used in the current decade appear grayed out.
- When the player enters a new decade, all used cards are restored to full color and flash once.
- Suggested: long-press (mobile) or hover (browser) shows the saint's name.
- Until portraits exist, show a placeholder (for example the saint's initials on a colored tile).

### 7.2 Using a card

A card can be used only when all of these are true:

- A catechism question is open (never on a Prayer Challenge, Saint Challenge, Moral Dilemma, or penance).
- The player has not yet answered that question. This is deliberate: it teaches the player to ask for the prayers of the saints before acting, not after failing.
- That card has not been used in the current decade.

When the player taps an eligible card:

1. Show the prompt "Saint {Display Name}, pray for me!" (or the `Invocation` override).
2. Reveal the correct answer to the question.
3. Move the player to the next bead.
4. Mark the card as used for this decade.
5. Leave the streak unchanged. Record the question in the Codex the same as a correct answer (Section 10).

### 7.3 Lifetime

- Each card can be used once per decade, as many decades as the player holds it.
- Cards last for the current game only. A new game starts with no cards.
- Cards are kept through penance (Section 9.3).

---

## 8. Prayer Challenge (gold Glory beads)

### 8.1 Setup

1. When the player lands on a Glory bead, draw the next prayer from the prayer deck (below). A modal tells the player how many segments the prayer has and how many seconds they will have to put them in order (Section 8.3), then asks them to choose **English** or **Latin**. When the two languages have different numbers of segments (the Act of Contrition: 11 in English, 12 in Latin), give the count and time for each. The prayer's name is not shown until the language is chosen.
2. Load the segments for the chosen language.
3. Present the segments in a random order (never already in the correct order).

**Prayer deck.** At the start of each game the 15 prayers are shuffled into a deck, kept in the saved game. Every Prayer Challenge and penance prayer takes the next prayer from the deck, whether the player passes it, runs out of time or gives up; English and Latin are the same prayer, so the language does not matter. Only when the whole deck has been used is it reshuffled, and the new order never starts with the prayer drawn last. With **Easy prayers** on (Section 11.2), prayers not marked easy are skipped as the deck is drawn, so only the six easy prayers appear, each once per pass. A game saved by an older version gets a new deck the first time it draws a prayer.

### 8.2 Arranging the segments

- Once the language is chosen, the prayer takes the whole screen, covering the board and the in-game bar, so that even the longest prayers show as many segments as possible. The timer stays in the top corner, and a menu button sits beside the heading so the player can still go back to the main menu. The Text size option (Section 11.2) still applies, but the Challenge window size does not.
- The segments are shown as a vertical list of capsules (rounded rectangles), one segment per capsule, in a shuffled order, packed closely together. The list is inset a little (24 px / 24 dp) from both sides of the screen, so a left- or right-handed player can scroll with a thumb in that margin without grabbing a capsule.
- The player rearranges them by drag and drop: press and hold a capsule (finger or mouse) and drag it up or down. When grabbed, the capsule gives a short shudder, is highlighted and lifted with a shadow, and follows the finger.
- While dragging, a dark line, a little wider than the capsules, appears in the gap between capsules where the held capsule will go: above the first capsule, between two, or below the last. No line shows while the capsule is over its own place. Letting go moves the capsule into that gap; the others close up around it. Letting go with no line showing leaves the order unchanged.
- When a capsule is held near the top or bottom edge of the screen, the list scrolls so it can reach segments out of view.
- **Timed Prayer Challenge (Glory beads):** the order is checked after every drop. As soon as the order is correct and time remains, the player passes immediately: the timer stops and the pass is recorded. There is no "Not quite" message and no hint about which segments are out of place.
- **Penance prayer (Section 9.3):** there is a **Submit** button instead of automatic checking. Submitting shows "Correct!" or "Not quite, keep trying." The player can only continue the game after submitting the correct order. This makes the player work from what they actually know.

### 8.3 Time limit

The time limit scales with the number of segments in the chosen language, from 25 seconds for 4 segments to 130 seconds for 20 segments:

`time = 15 + (segments - 4) x 105 / 16`, rounded to the nearest 5 seconds (halves round up), plus 10 seconds.

| Prayer (English / Latin) | Segments | Time |
|---|---|---|
| The Sign of the Cross / Signum Crucis | 4 | 25 s |
| Come, Holy Spirit / Veni, Sancte Spiritus | 5 | 30 s |
| Eternal Rest / Requiem Aeternam | 4 | 25 s |
| The Fatima Prayer / Oratio Fatimae | 6 | 40 s |
| Grace Before Meals / Benedictio Mensae | 6 | 40 s |
| The Guardian Angel Prayer / Angele Dei | 7 | 45 s |
| The Glory Be / Gloria Patri | 8 | 50 s |
| The Hail Mary / Ave Maria | 10 | 65 s |
| The Prayer to Saint Michael / Oratio ad Sanctum Michaelem | 11 | 70 s |
| The Act of Contrition | 11 | 70 s |
| Actus Contritionis | 12 | 80 s |
| The Our Father / Pater Noster | 12 | 80 s |
| The Hail, Holy Queen / Salve Regina | 13 | 85 s |
| The Soul of Christ / Anima Christi | 14 | 90 s |
| The Memorare / Memorare | 14 | 90 s |
| The Apostles' Creed / Credo | 20 | 130 s |

Compute the time from the data rather than hard-coding this table, so edits to the CSV stay consistent.

### 8.4 Outcome

- **Completed (either language):** the capsules turn green and "Well done!" shows with a **Continue** button. The feedback and buttons under the prayer (Give Up, Submit, then Continue or Try another prayer) stay at the bottom of the screen even when the list is longer than the screen, so Continue is always in view. The finished prayer stays on screen so the player can read it; the game moves on only when they tap Continue. The pass is recorded straight away, so going back to the menu before tapping Continue does not lose it: on resume the game continues from the new bead.
- **Completed in English:** move the player to bead 1 of the next decade.
- **Completed in Latin:** move the player to bead 6 of the next decade (beads 1 to 5 are skipped and do not affect the streak).
- **Completed on Glory bead 5:** in English, move to final bead 1. In Latin, skip the three final beads entirely and go straight to the Cross and the Moral Dilemma.
- **Time runs out:** show "Time's up," then start a new Prayer Challenge on the same bead with the next prayer from the deck, and ask for the language again. The missed challenge counts against the score (Section 3.1).
- **Give Up:** a Give Up button sits below the capsules while the player is arranging. It ends the challenge at once and starts a new Prayer Challenge on the same bead with the next prayer from the deck, asking for the language again. There is no other penalty, but like running out of time it counts as a missed challenge in the score (Section 3.1), so it rules out a perfect game.

---

## 9. The Cross: Moral Dilemma

### 9.1 Selection

- Pick a random dilemma, filtered by the age setting in Options (Section 11).
- Do not repeat a dilemma within the same game until all eligible dilemmas have been used. Then reshuffle the eligible dilemmas and continue, making sure the first one drawn is not the one just failed.
- With the default setting 20 dilemmas are eligible (`All Ages`); with "Include Ages 13 and up" all 34 are.

### 9.2 Presentation and answer

- Show `Title`, `Dilemma`, and `Question`, then two buttons: `Right Choice` and `Wrong Choice`, in random order. There is no timer.
- After the player chooses, always show the teaching, whichever choice they made: `Catholic Answer`, `Key Principle`, `CCC References`, and `Sources` with tappable links.
- **Correct choice:** the player wins. Record the dilemma in the Codex and go to the victory screen (Section 9.4).
- **Wrong choice:** the message reads "That is not the right choice. Go to the confessional by the waters of Baptism to receive the Sacrament of Reconciliation and do penance." After the teaching, go to penance (Section 9.3).

### 9.3 Penance

1. The player falls back to Decade 1, bead 1 (the waters of Baptism). Show this on the board: the Cross glow goes out, then the lit beads turn off one at a time from the top down to bead 1, about 30 ms per bead (under 2 seconds in total). Bead 1 stays lit.
2. A penance Prayer Challenge begins: the next prayer is drawn from the prayer deck (Section 8.1), the modal gives its number of segments and says there is no time limit, and the player chooses English or Latin. It works like Section 8 except there is **no timer**, **no Latin bonus**, and the arrangement is checked only when the player taps **Submit** (Section 8.2). Give Up sits beside Submit and changes to the next prayer from the deck, with no penalty.
3. When the prayer is completed, the player is restored to a random bead (1 to 10) of Decade 5.
4. Play continues normally from there: catechism questions to the end of Decade 5, Glory bead 5, the final beads, and the Cross with the next dilemma from the draw (Section 9.1).
5. Saint cards are kept. Entering Decade 5 counts as entering a new decade, so all cards are restored.
6. The streak is kept (Section 5).

### 9.4 Victory

- Show a victory screen with the game's score (Section 3.1).
- Mark the game as beaten at least once.
- Open what the win earns in the Codex (Section 10) and say so. After a perfect game, say which parts opened and, for any that did not, what would open them (All prayers for the whole game for the saints; advanced questions for the whole game for the dilemmas for ages 13 and up). After a game that was not perfect, explain that a perfect game with those settings opens them.
- Clear the save file.
- Return to the main menu.

---

## 10. Codex

Opened from the main menu. A library of the game's content, one section at a time. The section is picked from a drop-down list at the top (Prayers, Saints, Questions, Dilemmas; one at a time), which replaced the four section buttons whose labels wrapped.

The Codex exists to help players learn, not to hold content back, so everything a player needs for learning is open from the very first game. Only the saints and the dilemmas for older players are earned.

- **Prayers:** always open, showing each prayer in English and Latin.
- **Saints:** shows each saint the player has won a card for in a Saint Challenge in any game (name, portrait, birth year, all 10 clues). Locked saints show as a placeholder. **Every** saint opens after the player wins a perfect game (Section 3.1) with Prayers set to **All prayers** for the whole game; a single prayer drawn with Easy prayers on rules that game out. Saints are listed alphabetically by name, without the "St." in front (so "Augustine of Hippo," not "St. Augustine of Hippo"). "The Blessed Virgin Mary" keeps her title and is sorted under B.
- **Questions:** every catechism question, normal and advanced, is always open with its answer. The normal questions come first, then the advanced questions under their own heading, with a count for each set.
- **Moral Dilemmas:** the All Ages dilemmas are always open, with their full teaching and links, so parents can read what younger players will see. A dilemma for ages 13 and up shows once the player has answered it correctly in any game. **Every** dilemma for ages 13 and up opens after the player wins a perfect game with **advanced questions included** for the whole game; a single question drawn with "Normal questions only" rules that game out.
- The game records, in the saved game, whether Easy prayers or "Normal questions only" were used at any point. A game saved before version 0.2.2 is taken to have used the options as they are when it is loaded.
- Stored separately: "beaten" (won at least once), "complete" (won a perfect game), "all saints" and "older dilemmas". A Codex that a perfect game had fully opened before version 0.2.2 keeps every saint and every dilemma open.
- Codex progress persists across games and app restarts. It is separate from the in-game card hand, which resets every game.

---

## 11. Main Menu and Options

### 11.1 Main menu buttons

The menu shows the game's title over the zoomed board, the buttons below, and the version number (for example "Version 0.2.2") at the bottom center of the screen. The version comes from the `VERSION` file in the repo root, which both builds read.

- **Start Game:** begins a new game. If a save exists, ask to confirm before overwriting it.
- **Resume Game:** continues the saved game. Disabled when there is no save.
- **Options**
- **Codex**
- **About**
- **Quit:** on Android, exits the app. In the browser version, a web page cannot close itself, so show a modal with this text, one line each:

  > Thanks for playing!
  >
  > Please close the browser to exit the game.
  >
  > AVE MARIA

### 11.2 Options

- **Text size:** Small, Medium or Large, for the text in the menu, the other screens and the challenge modals. Default: Small (85% of Medium).
- **Phones (browser):** on screens up to 520 pixels wide or 700 tall, challenge windows use smaller text, tighter spacing and a smaller saint portrait, so questions and results fit without scrolling. A heading only leaves room for a timer when the challenge has one. Any button that appears at the end of a challenge (Next, Continue) is scrolled into view.
- **Challenge window size:** Compact, Standard or Large. Compact is at most 480 wide and half the screen tall; Standard 580 wide and 70% tall; Large 680 wide and as tall as the screen allows. The content scrolls inside when it does not fit. It does not apply while a prayer is being arranged, which always takes the whole screen (Section 8.2). Default: Compact.
- **Catechism questions:** "Normal questions only" or "Include advanced questions from the Catechism of the Catholic Church" (Section 4.1). It is separate from Easy prayers and from the dilemma age level, and can be changed at any time; it takes effect from the next question drawn. Default: Normal questions only.
- **Prayers:** "All prayers" or "Easy prayers only." With easy prayers, only the prayers marked `EasyPrayer` = `TRUE` in the data appear in Prayer Challenges and penance: The Our Father, The Hail Mary, The Glory Be, The Sign of the Cross, Grace Before Meals and The Guardian Angel Prayer. The player still chooses English or Latin, and Latin still skips five beads. It can be changed at any time, including during a game (from the in-game menu button, then Options); it takes effect from the next prayer drawn. Default: All prayers.
- **Moral dilemma age level:** "All Ages only" or "Include Ages 13 and up." Default: All Ages only.

---

## 12. Saving

- One save slot.
- Auto-save after every state change (bead move, card won, card used, streak change, challenge started).
- Saved state includes: current bead index, current decade, streak, cards held and which are used this decade, catechism question draw order and position, used dilemmas, any active challenge and its key data (saint, prayer, language, revealed clue count), whether the player is in penance, the prayer deck and how far it has been drawn (Section 8.1), the score, and whether the player has chosen wrongly at the Cross (Section 3.1).
- When the app is closed during a timed challenge, resuming restarts that same challenge from the beginning: the same saint at t = 0, or the same Glory bead with the same prayer, with the language choice shown again.
- Timers pause while the app is in the background or the browser tab is hidden.
- Where it is stored: on Android, in app-private storage. In the browser version, in `localStorage`, with the save slot and the Codex progress under separate keys so that clearing the save on victory never touches the Codex. Browser storage has the limits described in `browser/README.md` (per browser, per device, cleared with site data).

---

## 13. Assets Still Needed

- Victory screen art.
- About screen content, including a credit line for each portrait marked "Credit needed: Yes" in `saint_portrait_sources.md`.

---

## 13. Sound Effects

Five short sounds, kept in the `sounds/` folder; each file's name says when it plays. Each plays once per event, never on a loop:

| File | Plays when |
|---|---|
| `question_answered_correctly.mp3` | a catechism question is answered correctly |
| `question_wrong.mp3` | a catechism question is answered wrongly |
| `passed_challenge.mp3` | a Prayer Challenge is completed or a Saint Challenge is won |
| `pennance_complete.mp3` | the penance prayer is submitted correctly |
| `final_victory.mp3` | the victory screen opens; it stops when the player leaves the victory screen |

- The browser build embeds the sounds in the page; the Android build copies them into the app's assets.
- Browsers only play sound after the player has tapped or clicked, and every sound here follows a tap or click. On an iPhone the silent switch can mute them.

