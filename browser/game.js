// The Way of Life: browser game (spec: the-way-of-life-rules.md). Game rules live in rules.js;
// this file handles screens, modals, timers and saving.
(function () {
  "use strict";

  const R = window.Rules;
  const DATA = window.GAME_DATA;
  const PORTRAITS = window.PORTRAITS;
  const OBJECT_PICTURES = window.OBJECT_PICTURES || {};
  const SOUNDS = window.SOUNDS || {};
  const rng = Math.random;

  const KEYS = { save: "wayoflife.save", codex: "wayoflife.codex", options: "wayoflife.options" };
  const SAVE_VERSION = 1;
  const CORRECT_PAUSE_MS = 900;
  const REMINDER_SECONDS = 5;
  const FALL_STEP_MS = 30;
  // How far a pressed prayer capsule must move before it is dragged rather than tapped (spec 8.2).
  const TAP_SLOP_PX = 8;

  // ---- Storage (localStorage can be blocked; the game still works without it) ----------------

  function load(key, fallback) {
    try {
      const raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : fallback;
    } catch (e) {
      return fallback;
    }
  }
  function store(key, value) {
    try {
      if (value === null) localStorage.removeItem(key);
      else localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {
      // Progress just won't persist in this browser.
    }
  }

  let state = null;
  // `beaten`: won at least once. `complete`: won a perfect game. `allSaints` and `olderDilemmas`: opened
  // by a perfect game on All prayers / with advanced questions (spec 10). A Codex from before 0.2.2 that a
  // perfect game had fully opened keeps both.
  let codex = (() => {
    const saved = load(KEYS.codex, {});
    const opened = !!saved.complete;
    return Object.assign({ saints: [], questions: [], dilemmas: [], beaten: false, complete: false, allSaints: opened, olderDilemmas: opened }, saved);
  })();
  let options = Object.assign({ includeOlder: false, easyPrayers: false, advancedQuestions: false, lesserSaints: false, textSize: "small", modalSize: "compact" }, load(KEYS.options, {}));
  store("wayoflife.rosary.position", null); // left over from the bead-lighting prototype

  function saveGame() { if (state) store(KEYS.save, state); }
  function savedGame() {
    const s = load(KEYS.save, null);
    // Saves from before the score was kept start it at 0/0.
    return s && s.v === SAVE_VERSION ? Object.assign({ questionsRight: 0, questionsAsked: 0, prayersPassed: 0, prayersTried: 0, missedCross: false, pDeck: [], pPos: 0,
      usedEasyPrayers: options.easyPrayers, usedNormalOnly: !options.advancedQuestions }, s) : null;
  }
  function addToCodex(list, value) {
    if (!codex[list].includes(value)) {
      codex[list].push(value);
      store(KEYS.codex, codex);
    }
  }

  // ---- DOM helpers ------------------------------------------------------------------------

  const $ = id => document.getElementById(id);
  function el(tag, props, ...children) {
    const node = document.createElement(tag);
    for (const [k, v] of Object.entries(props || {})) {
      if (v === undefined || v === null || v === false) continue;
      if (k === "class") node.className = v;
      else if (k === "text") node.textContent = v;
      else if (k.startsWith("on")) node.addEventListener(k.slice(2), v);
      else if (v === true) node.setAttribute(k, "");
      else node.setAttribute(k, v);
    }
    for (const c of children.flat()) if (c !== null && c !== undefined && c !== false) node.append(c);
    return node;
  }
  function button(label, onclick, cls) {
    return el("button", { type: "button", class: "btn" + (cls ? " " + cls : ""), onclick }, label);
  }
  // Plain text with any URLs turned into links (dilemma sources, spec 1).
  function linkify(text) {
    const frag = document.createDocumentFragment();
    let last = 0;
    for (const m of text.matchAll(/https?:\/\/[^\s)\]]+[^\s)\].,;:]/g)) {
      frag.append(text.slice(last, m.index));
      frag.append(el("a", { href: m[0], target: "_blank", rel: "noopener" }, m[0]));
      last = m.index + m[0].length;
    }
    frag.append(text.slice(last));
    return frag;
  }
  let toastTimer = null;
  function toast(message) {
    const t = $("toast");
    t.textContent = message;
    t.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { t.hidden = true; }, 1600);
  }

  // ---- Board ------------------------------------------------------------------------------

  const board = $("board");
  const BEADS = DATA.board.beads;
  function makeGlow(spot) {
    const g = el("div", { class: "glow" });
    g.style.left = spot.x * 100 + "%";
    g.style.top = spot.y * 100 + "%";
    board.append(g);
    return g;
  }
  const glows = BEADS.map(makeGlow);
  const crossGlow = makeGlow(DATA.board.cross);
  crossGlow.style.width = DATA.board.cross.r * 2 * 1.8 * 100 + "%";

  // ---- Board view (spec 1.3): the whole board on the title screen, zoomed on the player otherwise ----

  const BOARD_ASPECT = DATA.board.imageWidth / DATA.board.imageHeight;
  const ZOOM = 1.8;
  let zoomed = false;
  let focus = 0;
  function spotOf(index) { return index >= R.CROSS ? DATA.board.cross : BEADS[index]; }
  // Challenges open at the top of the screen when the bead is in the lower half of the board.
  function modalAtTop(index) { return spotOf(index).y > 0.5; }
  function layoutBoard() {
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const scale = zoomed ? ZOOM : 1;
    const w = board.offsetWidth * scale; // the CSS width fits the whole board on screen
    const h = w / BOARD_ASPECT;
    const spot = spotOf(focus);
    // Put the bead in the part of the screen the challenge leaves open, without showing past the board's edges.
    const fy = modalAtTop(focus) ? 0.72 : 0.3;
    const place = (size, view, want) => size <= view ? (view - size) / 2 : Math.min(0, Math.max(view - size, want));
    const x = place(w, vw, vw / 2 - spot.x * w);
    const y = place(h, vh, vh * fy - spot.y * h);
    board.style.transform = `translate(${x}px, ${y}px) scale(${scale})`;
  }
  function setZoomed(value) {
    zoomed = value;
    layoutBoard();
  }
  window.addEventListener("resize", layoutBoard);
  // The area left visible by the on-screen keyboard, for the full-screen challenges (Safari on iPhone
  // covers the page with the keyboard instead of shrinking it).
  function trackVisibleArea() {
    const vv = window.visualViewport;
    if (!vv) return;
    const update = () => {
      document.documentElement.style.setProperty("--vv-height", vv.height + "px");
      document.documentElement.style.setProperty("--vv-top", vv.offsetTop + "px");
    };
    vv.addEventListener("resize", update);
    vv.addEventListener("scroll", update);
    update();
  }
  trackVisibleArea();
  // Plays a sound effect once (spec 13). Every sound follows a tap or click, which browsers require.
  function playSound(name) {
    if (!SOUNDS[name]) return null;
    const audio = new Audio(SOUNDS[name]);
    audio.play().catch(() => {});
    return audio;
  }
  // Scrolls a button that just appeared at the end of a challenge into view (small screens).
  function reveal(node) {
    requestAnimationFrame(() => node.scrollIntoView({ block: "nearest", behavior: "smooth" }));
  }

  // A white sunburst from a bead that has just lit up: a flash and sparks flying outward, fading to the lit bead.
  function burst(spot) {
    const r = spot.r * board.offsetWidth;
    const b = el("div", { class: "burst" });
    b.style.left = spot.x * 100 + "%";
    b.style.top = spot.y * 100 + "%";
    const flash = el("div", { class: "burst-flash" });
    flash.style.width = flash.style.height = r * 9 + "px";
    b.append(flash);
    flash.animate([
      { transform: "translate(-50%, -50%) scale(0.2)", opacity: 1 },
      { transform: "translate(-50%, -50%) scale(1)", opacity: 0 },
    ], { duration: 650, easing: "ease-out" });
    const SPARKS = 22;
    for (let i = 0; i < SPARKS; i++) {
      const spark = el("div", { class: "burst-spark" });
      const angle = (i + Math.random() * 0.6) / SPARKS * 2 * Math.PI;
      const distance = r * (5 + Math.random() * 3);
      const dx = Math.cos(angle) * distance;
      const dy = Math.sin(angle) * distance;
      spark.style.width = spark.style.height = Math.max(3, r * (0.4 + Math.random() * 0.3)) + "px";
      b.append(spark);
      spark.animate([
        { transform: "translate(-50%, -50%)", opacity: 1 },
        { transform: `translate(calc(-50% + ${dx * 0.85}px), calc(-50% + ${dy * 0.85}px))`, opacity: 1, offset: 0.55 },
        { transform: `translate(calc(-50% + ${dx}px), calc(-50% + ${dy + r * 1.5}px)) scale(0.4)`, opacity: 0 },
      ], { duration: 1000 + Math.random() * 300, easing: "cubic-bezier(0.2, 0.7, 0.3, 1)" });
    }
    board.append(b);
    setTimeout(() => b.remove(), 1400);
  }
  // Bursts on beads that just lit up: each one on a short step (a Latin skip lights six at once), only the
  // new bead on a long jump (restored after penance), none on the first render or when beads go out.
  let shownLit = null;
  function burstNewlyLit(litUpTo) {
    const from = shownLit;
    shownLit = litUpTo;
    if (from === null || litUpTo <= from) return;
    for (let i = litUpTo - from <= 6 ? from + 1 : litUpTo; i <= Math.min(litUpTo, R.CROSS); i++) burst(spotOf(i));
  }

  // Lights beads 0..litUpTo; the bead at `current` (or the Cross) gets the stronger glow and the view follows it.
  function renderBoard(litUpTo, current) {
    burstNewlyLit(litUpTo);
    glows.forEach((g, i) => {
      const isCurrent = i === current;
      g.style.width = BEADS[i].r * 2 * (isCurrent ? 2.25 : 1.8) * 100 + "%";
      g.classList.toggle("lit", i <= litUpTo);
      g.classList.toggle("current", isCurrent);
    });
    crossGlow.classList.toggle("lit", litUpTo >= R.CROSS);
    crossGlow.classList.toggle("current", current >= R.CROSS);
    if (current >= 0) {
      focus = Math.min(current, R.CROSS);
      layoutBoard();
    }
  }
  function showPosition() {
    const p = state ? state.position : 0;
    renderBoard(p, p);
  }
  // Spec 9.3: the Cross glow goes out, then the beads turn off from the top down to bead 1.
  function animateFall(from, done) {
    let lit = from;
    renderBoard(Math.min(lit, R.CROSS - 1), -1);
    const timer = setInterval(() => {
      lit -= 1;
      renderBoard(lit, lit);
      if (lit <= 0) { clearInterval(timer); done(); }
    }, FALL_STEP_MS);
  }

  // ---- Pausable countdown (timers pause while the tab is hidden, spec 12) ---------------------

  let activeTimer = null;
  function startTimer(seconds, onTick, onEnd) {
    const timer = { seconds, elapsed: 0, last: performance.now(), stopped: false, onTick, onEnd };
    timer.interval = setInterval(() => tickTimer(timer), 100);
    activeTimer = timer;
    onTick(0, seconds);
    return timer;
  }
  function tickTimer(timer) {
    const now = performance.now();
    if (!document.hidden) timer.elapsed += (now - timer.last) / 1000;
    timer.last = now;
    if (timer.stopped) return;
    const remaining = Math.max(0, timer.seconds - timer.elapsed);
    timer.onTick(timer.elapsed, remaining);
    if (remaining <= 0) {
      stopTimer(timer);
      timer.onEnd();
    }
  }
  function stopTimer(timer) {
    if (!timer) return;
    timer.stopped = true;
    clearInterval(timer.interval);
    if (activeTimer === timer) activeTimer = null;
  }
  document.addEventListener("visibilitychange", () => {
    if (activeTimer) activeTimer.last = performance.now();
  });
  function timerBadge() { return el("div", { class: "timer", "aria-live": "off" }); }
  function setBadge(badge, remaining) {
    badge.textContent = Math.ceil(remaining) + "s";
    badge.classList.toggle("low", remaining <= 10);
  }

  // ---- HUD: saint cards and streak ----------------------------------------------------------

  const saintByName = new Map(DATA.saints.map((s, i) => [s.name, i]));
  let cardHandler = null; // set while a question is open and unanswered

  function renderHud(flash) {
    const cards = $("cards");
    cards.textContent = "";
    if (state.cards.length === 0) cards.append(el("span", { class: "empty" }, "No saint cards yet"));
    for (const name of state.cards) {
      const saint = DATA.saints[saintByName.get(name)];
      const used = state.used.includes(name);
      const usable = !!cardHandler && !used;
      const card = el("button", {
        type: "button",
        class: "card" + (used ? " used" : "") + (usable ? " usable" : "") + (flash && flash.includes(name) ? " flash" : ""),
        title: name + (used ? " (used this decade)" : ""),
        "aria-label": name + (used ? ", used this decade" : usable ? ", tap to ask for prayers" : ""),
        onclick: () => usable ? cardHandler(name) : toast(name),
      }, el("img", { src: PORTRAITS[saint.portrait], alt: "" }));
      addLongPress(card, () => toast(name));
      cards.append(card);
    }
    const pips = $("pips");
    pips.textContent = "";
    for (let i = 0; i < R.STREAK_FOR_SAINT; i++) pips.append(el("span", { class: "pip" + (i < state.streak ? " on" : "") }));
    pips.title = `Streak: ${state.streak} of ${R.STREAK_FOR_SAINT}`;
    $("stats").textContent = scoreText(state);
  }
  function scoreText(s) {
    return `Questions: ${s.questionsRight}/${s.questionsAsked} \u00B7 Prayer challenge: ${s.prayersPassed}/${s.prayersTried}`;
  }
  function addLongPress(node, action) {
    let timer = null;
    node.addEventListener("touchstart", () => { timer = setTimeout(action, 500); }, { passive: true });
    for (const ev of ["touchend", "touchmove", "touchcancel"]) node.addEventListener(ev, () => clearTimeout(timer));
  }

  // ---- Modal ------------------------------------------------------------------------------

  // Timers stay pinned in the modal's corner; everything else scrolls in the body, which is returned.
  function openModal(...children) {
    const modal = $("modal");
    const items = children.filter(c => c !== null && c !== undefined);
    const body = el("div", { class: "modal-body" }, items.filter(c => !(c.classList && c.classList.contains("timer"))));
    const timers = items.filter(c => c.classList && c.classList.contains("timer"));
    modal.textContent = "";
    modal.classList.toggle("timed", timers.length > 0);
    $("modal-wrap").classList.remove("full");
    modal.append(...timers, body);
    placeModal();
    $("modal-wrap").hidden = false;
    return body;
  }
  // Keep the player's bead visible: put the modal at the top when the bead is in the lower half.
  function placeModal() {
    $("modal-wrap").classList.toggle("top", modalAtTop(state ? state.position : 0));
  }
  function closeModal() {
    stopTimer(activeTimer);
    cardHandler = null;
    $("modal-wrap").hidden = true;
    $("modal").textContent = "";
  }

  function beadLabel(index) {
    if (index >= R.CROSS) return "The Cross";
    if (index >= 55) return `Final bead ${index - 54}`;
    const decade = R.decadeOf(index);
    return R.isGlory(index) ? `Glory bead ${decade}` : `Decade ${decade}, bead ${index % 11 + 1}`;
  }

  // ---- Game flow ----------------------------------------------------------------------------

  function startGame() {
    state = R.newGame(DATA, rng);
    enterGame();
    nextChallenge();
  }
  function resumeGame() {
    state = savedGame();
    if (!state) return showMenu();
    enterGame();
    if (state.challenge) openChallenge(); else nextChallenge();
  }
  function enterGame() {
    hidePanel();
    $("hud").hidden = false;
    showPosition();
    renderHud();
  }
  function leaveGame() {
    closeModal();
    saveGame();
    $("hud").hidden = true;
    showMenu();
  }

  // Asks the current bead for its challenge and opens it.
  function nextChallenge() {
    state.challenge = R.challengeForPosition(state, DATA, rng, options.includeOlder, options.easyPrayers, options.advancedQuestions);
    saveGame();
    openChallenge();
  }
  function openChallenge() {
    const c = state.challenge;
    showPosition();
    renderHud();
    if (c.kind === "question") openQuestion(c.q);
    else if (c.kind === "saint") openSaintChallenge(c.saint);
    else if (c.kind === "prayer") openPrayer(c.prayer, c.penance);
    else if (c.kind === "dilemma") openDilemma(c.d);
  }
  // After a move: refresh board and cards (flashing any restored), then continue.
  function afterMove(restored, then) {
    saveGame();
    showPosition();
    renderHud(restored);
    then();
  }

  // ---- Catechism question (spec 4, 7.2) ---------------------------------------------------------

  function openQuestion(qIndex) {
    const q = DATA.questions[qIndex];
    const choices = q.trueFalse ? q.options.slice() : R.shuffle(q.options, rng);
    // "What is this?" questions show one of several photos of the object (spec 4.5).
    // "Who is this?" pictures of people are worded by picture: a statue or an image (spec 4.6).
    const pictureIndex = q.pictures.length ? Math.floor(rng() * q.pictures.length) : -1;
    const picture = pictureIndex >= 0 ? q.pictures[pictureIndex] : null;
    const questionText = picture ? q.pictureQuestions[pictureIndex] : q.question;
    const pictureImg = () => picture ? el("img", { class: "portrait", src: OBJECT_PICTURES[picture], alt: "Picture for the question" }) : null;
    const feedback = el("p", { class: "feedback" });
    const buttons = choices.map(text => button(text, () => choose(text)));
    const extra = el("div");
    let answered = false;

    function lock() {
      answered = true;
      cardHandler = null;
      renderHud();
      buttons.forEach(b => { b.disabled = true; });
    }
    function choose(text) {
      if (answered) return;
      lock();
      const right = buttons[choices.indexOf(q.correct)];
      if (text === q.correct) {
        right.classList.add("right");
        playSound("question_answered_correctly");
        feedback.textContent = "Correct!";
        feedback.className = "feedback good";
        addToCodex("questions", q.id);
        setTimeout(() => {
          closeModal();
          const result = R.answerCorrect(state);
          if (result.saintChallenge) state.challenge = { kind: "saint", saint: R.pickSaint(state, DATA, options.lesserSaints, rng) };
          afterMove(result.restored, () => result.saintChallenge ? openChallenge() : nextChallenge());
        }, CORRECT_PAUSE_MS);
      } else {
        R.answerWrong(state);
        saveGame();
        renderHud();
        buttons[choices.indexOf(text)].classList.add("wrong");
        right.classList.add("right");
        feedback.textContent = "Not quite.";
        feedback.className = "feedback bad";
        playSound("question_wrong");
        const next = button("Next", () => {
          state.challenge = { kind: "question", q: R.drawQuestion(state, DATA, options.advancedQuestions, rng) };
          saveGame();
          openChallenge();
        }, "primary");
        extra.append(el("div", { class: "answer-note" }, q.answer), next);
        reveal(next);
      }
    }
    // Saint card: ask for prayers before answering (spec 7.2).
    cardHandler = name => {
      if (answered) return;
      lock();
      const saint = DATA.saints[saintByName.get(name)];
      buttons[choices.indexOf(q.correct)].classList.add("right");
      addToCodex("questions", q.id);
      openModal(
        el("p", { class: "kicker" }, beadLabel(state.position)),
        el("img", { class: "portrait", src: PORTRAITS[saint.portrait], alt: name }),
        el("p", { class: "invocation" }, saint.invocation),
        el("p", {}, questionText),
        pictureImg(),
        el("div", { class: "answer-note" }, el("strong", {}, q.correct), el("br"), q.answer),
        button("Continue", () => {
          closeModal();
          const result = R.useCard(state, name);
          afterMove(result ? result.restored : [], nextChallenge);
        }, "primary"));
    };
    renderHud();
    openModal(
      el("p", { class: "kicker" }, beadLabel(state.position)),
      el("h2", {}, questionText),
      pictureImg(),
      el("div", { class: "stack" }, buttons),
      feedback,
      extra);
  }

  // ---- Saint Challenge (spec 6) -------------------------------------------------------------

  function openSaintChallenge(saintIndex) {
    const saint = DATA.saints[saintIndex];
    const badge = timerBadge();
    const clueList = el("ol", { class: "clues" });
    const feedback = el("p", { class: "feedback", "aria-live": "polite" });
    const input = el("input", { type: "text", placeholder: "Name the saint", autocomplete: "off", autocapitalize: "words", spellcheck: "false", "aria-label": "Your guess" });
    const nextClue = button("Next Clue", () => revealNext());
    let shown = 0;
    let over = false;

    // Clues appear one at a time, only when the player asks for the next one (spec 6.2).
    function revealNext() {
      if (shown >= saint.clues.length) return;
      const clue = el("li", { class: "new" }, saint.clues[shown]);
      clueList.append(clue);
      shown++;
      nextClue.disabled = shown >= saint.clues.length;
      clue.scrollIntoView({ block: "nearest" });
    }
    // Names are matched only against saints that can come up in this game, and always this one (spec 6.4).
    const inPlay = (s, i) => i === saintIndex || options.lesserSaints || !s.lesser;
    // Every 5 seconds Next Clue shudders, as a reminder that more clues are there.
    let beat = 0;
    const timer = startTimer(R.SAINT_SECONDS, (elapsed, remaining) => {
      setBadge(badge, remaining);
      if (Math.floor(elapsed / REMINDER_SECONDS) > beat) {
        beat = Math.floor(elapsed / REMINDER_SECONDS);
        if (!nextClue.disabled) shudder(nextClue);
        // A name that fits more than one saint (such as "Ignatius") never wins by itself, so say so
        // without waiting for Enter (spec 6.3).
        if (!over && R.judgeGuess(input.value, DATA.saints, DATA.equivalents, inPlay).result === "vague") {
          feedback.className = "feedback bad";
          feedback.textContent = "More than one saint has that name. Be more specific.";
        }
      }
    }, () => finish(false));
    // The name is checked after every keystroke, and the challenge is won as soon as it would be
    // accepted (spec 6.3); there is no Submit button. Enter explains why a guess is not accepted yet.
    function accepted() {
      const r = R.judgeGuess(input.value, DATA.saints, DATA.equivalents, inPlay);
      if (r.result !== "accepted" || r.saint !== saintIndex) return false;
      input.blur();
      finish(true);
      return true;
    }
    function guess() {
      if (over || accepted()) return;
      const r = R.judgeGuess(input.value, DATA.saints, DATA.equivalents, inPlay);
      if (r.result === "ignored") return;
      feedback.className = "feedback bad";
      feedback.textContent = r.result === "vague" ? "More than one saint has that name. Be more specific." : "Not quite, try again.";
      input.select();
    }
    function finish(won) {
      if (over) return;
      over = true;
      stopTimer(timer);
      if (won) {
        playSound("passed_challenge");
        R.winSaint(state, saint.name);
        addToCodex("saints", saint.name);
        saveGame();
        renderHud([saint.name]);
      }
      const next = button("Continue", () => {
        closeModal();
        state.challenge = null;
        nextChallenge();
      }, "primary");
      openModal(
        el("p", { class: "kicker" }, "Saint Challenge"),
        el("img", { class: "portrait", src: PORTRAITS[saint.portrait], alt: saint.name }),
        el("p", { class: "result-name" }, saint.name),
        el("p", { class: "center" }, won ? "Well done! This saint's card has joined your hand." : "Not this time. No card was awarded."),
        next);
      reveal(next);
    }
    input.addEventListener("input", () => {
      if (!over && !accepted()) feedback.textContent = "";
    });
    input.addEventListener("keydown", e => { if (e.key === "Enter") guess(); });
    revealNext();
    // The challenge takes the whole screen; the clues scroll between the birth year and the guess box.
    openModal(
      badge,
      sheetHead("Saint Challenge"),
      saint.year ? el("p", {}, el("em", {}, "Born: "), saint.year) : null,
      el("div", { class: "clue-scroll" }, clueList),
      feedback,
      el("div", { class: "row" }, nextClue),
      el("div", { class: "guess" }, input),
      button("Give Up", () => finish(false))).classList.add("split");
    $("modal-wrap").classList.add("full");
    // On a phone the keyboard would cover half the screen at once, so it opens only when the field is tapped.
    if (!matchMedia("(hover: none)").matches) input.focus({ preventScroll: true });
  }

  // Restarts the shudder animation (the one a grabbed prayer capsule gives).
  function shudder(node) {
    node.classList.remove("shudder");
    void node.offsetWidth;
    node.classList.add("shudder");
  }

  // The top of a full-screen challenge: it covers the bar, so the menu button comes with it.
  function sheetHead(kicker) {
    return el("div", { class: "sheet-head" },
      el("button", { class: "icon-btn", type: "button", "aria-label": "Main menu", onclick: leaveGame }, "\u2630"),
      el("p", { class: "kicker" }, kicker));
  }

  // ---- Prayer Challenge and penance prayer (spec 8, 9.3) ---------------------------------------

  function openPrayer(prayerIndex, penance) {
    const title = penance ? "Penance" : "Prayer Challenge";
    openModal(
      el("p", { class: "kicker" }, penance ? "The waters of Baptism" : beadLabel(state.position)),
      el("h2", {}, title),
      el("p", {}, el("strong", {}, prayerSize(DATA.prayers[prayerIndex], penance))),
      el("p", {}, penance
        ? "Drag each line to where it belongs, or tap two lines to swap them, then tap Submit. When it is right, you will be restored to the fifth decade."
        : "Put the prayer in the right order before time runs out. Drag each line to where it belongs, or tap two lines to swap them."),
      penance ? null : el("p", {}, el("em", {}, "Pray it in Latin to skip ahead five beads.")),
      el("div", { class: "row" },
        button("English", () => arrangePrayer(prayerIndex, "english", penance)),
        button("Latin", () => arrangePrayer(prayerIndex, "latin", penance))));
  }

  // Spec 8.1: before choosing a language, say how many segments there are and how long the player will have.
  function prayerSize(prayer, penance) {
    const en = prayer.english.segments.length;
    const la = prayer.latin.segments.length;
    if (penance) {
      return (en === la ? `This prayer has ${en} segments.` : `This prayer has ${en} segments in English and ${la} in Latin.`) + " There is no time limit.";
    }
    return en === la
      ? `This prayer has ${en} segments. You will have ${R.prayerSeconds(en)} seconds to put them in order.`
      : `In English this prayer has ${en} segments and you will have ${R.prayerSeconds(en)} seconds. In Latin it has ${la} segments and you will have ${R.prayerSeconds(la)} seconds.`;
  }

  function arrangePrayer(prayerIndex, language, penance) {
    const prayer = DATA.prayers[prayerIndex][language];
    const order = R.scramble(prayer.segments, rng);
    const list = el("div", { class: "capsules", role: "list" });
    const line = el("div", { class: "drop-line", hidden: true });
    const feedback = el("p", { class: "feedback", "aria-live": "polite" });
    const badge = penance ? null : timerBadge();
    let over = false;
    let timer = null;
    let drag = null;
    let selected = -1;

    function draw() {
      if (drag) endDrag();
      list.textContent = "";
      order.forEach((text, i) => {
        list.append(el("div", { class: "capsule" + (i === selected ? " selected" : ""), role: "listitem", onpointerdown: e => grab(e, i) }, text));
      });
      list.append(line);
    }

    // A capsule is either dragged into the gap where it belongs, or tapped and then swapped with a second
    // tapped capsule (spec 8.2). Pointer events cover mouse and touch. A press only becomes a drag once the
    // pointer has moved a little; letting go before that is a tap.
    function grab(e, i) {
      if (over || drag || e.button > 0) return;
      e.preventDefault();
      const node = e.currentTarget;
      node.setPointerCapture(e.pointerId);
      const scroller = list.closest(".modal-body");
      drag = { i, node, scroller, startY: e.clientY, y: e.clientY, startScroll: scroller.scrollTop, gap: -1, moved: false };
      node.addEventListener("pointermove", ev => {
        if (!drag) return;
        drag.y = ev.clientY;
        if (!drag.moved) {
          if (Math.abs(drag.y - drag.startY) < TAP_SLOP_PX) return;
          lift();
        }
        follow();
      });
      node.addEventListener("pointerup", drop);
      node.addEventListener("pointercancel", () => draw());
    }
    // The press has turned into a drag: any capsule picked with a tap is put back.
    function lift() {
      drag.moved = true;
      selected = -1;
      capsules().forEach(c => c.classList.remove("selected"));
      drag.node.classList.add("grabbed");
      drag.frame = requestAnimationFrame(autoScroll);
    }
    // The first tap picks a capsule, tapping it again puts it back, and tapping another swaps the two.
    function tap(i) {
      if (selected < 0) selected = i;
      else if (selected === i) selected = -1;
      else {
        [order[selected], order[i]] = [order[i], order[selected]];
        selected = -1;
        if (arranged()) return;
      }
      draw();
    }
    // After every move: a timed prayer passes as soon as it is in order.
    function arranged() {
      feedback.textContent = "";
      if (!penance && R.isInOrder(order, prayer.segments)) {
        passed();
        return true;
      }
      return false;
    }
    function capsules() {
      return [...list.children].filter(c => c.classList.contains("capsule"));
    }
    // Moves the held capsule with the pointer and finds the gap it would drop into: before the first
    // other capsule whose middle is below the pointer. Gaps next to the capsule itself change nothing.
    function follow() {
      drag.node.style.transform = `translateY(${drag.y - drag.startY + drag.scroller.scrollTop - drag.startScroll}px)`;
      const caps = capsules();
      let gap = caps.length;
      for (let k = 0; k < caps.length; k++) {
        if (k === drag.i) continue;
        const r = caps[k].getBoundingClientRect();
        if (drag.y < r.top + r.height / 2) { gap = k; break; }
      }
      drag.gap = gap === drag.i || gap === drag.i + 1 ? -1 : gap;
      if (drag.gap < 0) { line.hidden = true; return; }
      const above = drag.gap > 0 ? caps[drag.gap - 1].getBoundingClientRect().bottom : null;
      const below = drag.gap < caps.length ? caps[drag.gap].getBoundingClientRect().top : null;
      const y = above === null ? below - 3 : below === null ? above + 3 : (above + below) / 2;
      line.style.top = (y - list.getBoundingClientRect().top - 1.5) + "px";
      line.hidden = false;
    }
    // Long prayers scroll while a capsule is held near the top or bottom edge.
    function autoScroll() {
      if (!drag) return;
      const r = drag.scroller.getBoundingClientRect();
      const bottom = foot.getBoundingClientRect().top;
      const step = drag.y < r.top + 48 ? -8 : drag.y > bottom - 48 ? 8 : 0;
      if (step) {
        drag.scroller.scrollTop += step;
        follow();
      }
      drag.frame = requestAnimationFrame(autoScroll);
    }
    function endDrag() {
      cancelAnimationFrame(drag.frame);
      drag = null;
      line.hidden = true;
    }
    function drop() {
      if (!drag) return;
      const { i, gap, moved } = drag;
      endDrag();
      if (!moved) return tap(i);
      if (gap >= 0) {
        const [text] = order.splice(i, 1);
        order.splice(gap > i ? gap - 1 : gap, 0, text);
        if (arranged()) return;
      }
      draw();
    }
    function passed() {
      over = true;
      selected = -1;
      playSound("passed_challenge");
      giveUpBtn.disabled = true;
      stopTimer(timer);
      draw();
      list.classList.add("done", "over");
      feedback.className = "feedback good";
      feedback.textContent = "Well done!";
      // The pass is recorded now, but the player stays on the finished prayer to read it until they
      // tap Continue (spec 8.4).
      const result = R.completeGloryPrayer(state, language === "latin");
      state.challenge = null;
      saveGame();
      const next = button("Continue", () => {
        closeModal();
        afterMove(result.restored, nextChallenge);
      }, "primary");
      actions.replaceChildren(next);
      next.focus({ preventScroll: true });
    }
    // When time runs out or the player gives up (spec 8.4), the prayer is shown in its right order. For a
    // Prayer Challenge it counts as missed; for penance there is no penalty. The next prayer in the deck is
    // drawn and saved at once, so leaving and coming back cannot retry the prayer just shown.
    function showAnswer(message) {
      over = true;
      stopTimer(timer);
      if (!penance) R.prayerMissed(state);
      state.challenge = { kind: "prayer", prayer: R.drawPrayer(state, DATA, options.easyPrayers, rng), penance };
      saveGame();
      renderHud();
      selected = -1;
      order.splice(0, order.length, ...prayer.segments);
      draw();
      list.classList.add("over");
      feedback.className = "feedback bad";
      feedback.textContent = message;
      const next = button("Try another prayer", () => openChallenge(), "primary");
      actions.replaceChildren(next);
      next.focus({ preventScroll: true });
    }
    function giveUp() {
      if (!over) showAnswer("Here is the prayer in the right order.");
    }
    function timeUp() {
      showAnswer("Time's up. Here is the prayer in the right order.");
    }
    function submit() {
      if (over) return;
      if (!R.isInOrder(order, prayer.segments)) {
        feedback.className = "feedback bad";
        feedback.textContent = "Not quite, keep trying.";
        return;
      }
      over = true;
      selected = -1;
      draw();
      list.classList.add("done", "over");
      submitBtn.disabled = true;
      giveUpBtn.disabled = true;
      feedback.className = "feedback good";
      feedback.textContent = "Correct! Your penance is complete.";
      playSound("pennance_complete");
      actions.replaceChildren(button("Continue", () => {
        closeModal();
        const result = R.finishPenance(state, rng);
        state.challenge = null;
        afterMove(result.restored, nextChallenge);
      }, "primary"));
    }
    const submitBtn = penance ? button("Submit", submit, "primary") : null;
    const giveUpBtn = button("Give Up", giveUp);
    // Feedback and buttons stay at the bottom of the screen, so Continue is always in view (spec 8.4).
    const actions = el("div", { class: "row" }, giveUpBtn, submitBtn);
    const foot = el("div", { class: "sheet-foot" }, feedback, actions);
    draw();
    openModal(
      badge,
      sheetHead(penance ? "Penance" : beadLabel(state.position)),
      el("h2", {}, prayer.title),
      list,
      foot);
    $("modal-wrap").classList.add("full");
    if (!penance) {
      timer = startTimer(R.prayerSeconds(prayer.segments.length), (elapsed, remaining) => setBadge(badge, remaining), timeUp);
    }
  }

  // ---- The Cross: Moral Dilemma (spec 9) ---------------------------------------------------------

  function openDilemma(dIndex) {
    const d = DATA.dilemmas[dIndex];
    const choices = R.shuffle([d.right, d.wrong], rng);
    const result = el("div");
    const buttons = choices.map(text => button(text, () => choose(text)));
    function choose(text) {
      buttons.forEach(b => { b.disabled = true; });
      const right = text === d.right;
      buttons[choices.indexOf(d.right)].classList.add("right");
      if (!right) buttons[choices.indexOf(text)].classList.add("wrong");
      if (right) addToCodex("dilemmas", d.id);
      // The victory bells ring the moment the right choice is made and carry on into the victory screen
      // (spec 13).
      const sound = playSound(right ? "final_victory" : "question_wrong");
      result.append(
        el("p", { class: "feedback " + (right ? "good" : "bad") }, right
          ? "That is the right choice."
          : "That is not the right choice. Go to the confessional by the waters of Baptism to receive the Sacrament of Reconciliation and do penance."),
        teaching(d),
        el("div", { class: "back-row" }, button("Continue", () => right ? victory(sound) : penance(), "primary")));
      result.scrollIntoView({ behavior: "smooth", block: "start" });
    }
    openModal(
      el("p", { class: "kicker" }, "The Cross"),
      el("h2", {}, d.title),
      el("p", {}, d.dilemma),
      el("p", {}, el("strong", {}, d.question)),
      el("div", { class: "stack" }, buttons),
      result);
  }
  function teaching(d) {
    return el("div", { class: "teaching" },
      el("h3", {}, "The Catholic answer"), el("p", {}, d.catholicAnswer),
      el("h3", {}, "Key principle"), el("p", {}, d.keyPrinciple),
      el("h3", {}, "Catechism references"), el("p", {}, d.ccc),
      el("h3", {}, "Sources"), el("p", {}, linkify(d.sources)));
  }
  function penance() {
    closeModal();
    const from = state.position;
    R.startPenance(state);
    state.challenge = { kind: "prayer", prayer: R.drawPrayer(state, DATA, options.easyPrayers, rng), penance: true };
    saveGame();
    animateFall(from, openChallenge);
  }
  // `fanfare` is the victory music, already started when the right choice was made.
  function victory(fanfare) {
    closeModal();
    const unlocks = R.codexUnlocks(state);
    const score = scoreText(state);
    codex.beaten = true;
    if (unlocks.perfect) codex.complete = true;
    if (unlocks.allSaints) codex.allSaints = true;
    if (unlocks.olderDilemmas) codex.olderDilemmas = true;
    store(KEYS.codex, codex);
    store(KEYS.save, null);
    state = null;
    $("hud").hidden = true;
    renderBoard(R.CROSS, R.CROSS);
    showPanel(
      el("h1", {}, "Victory"),
      el("p", { class: "sub" }, "You have reached the Cross."),
      el("p", { class: "center" }, "Through prayer, the help of the saints, and the teaching of the Church, you have climbed the way of life."),
      el("p", { class: "center" }, score),
      unlocks.perfect ? el("p", { class: "center" }, "You made no mistakes along the way.") : null,
      ...R.codexMessages(unlocks).map(text => el("p", { class: "center" }, text)),
      el("p", { class: "center" }, el("em", {}, "Ave Maria")),
      el("div", { class: "menu-buttons back-row" }, button("Main Menu", () => {
        if (fanfare) fanfare.pause(); // the victory music ends with the victory screen
        showMenu();
      }, "primary")));
  }

  // ---- Panels: menu, options, codex, about ------------------------------------------------------

  function showPanel(...children) {
    const panel = $("panel");
    panel.textContent = "";
    panel.append(...children.filter(c => c !== null && c !== undefined));
    panel.scrollTop = 0;
    $("panel-wrap").hidden = false;
    $("version").hidden = true;
  }
  function hidePanel() {
    $("panel-wrap").hidden = true;
    $("version").hidden = true;
  }

  function showMenu() {
    closeModal();
    $("hud").hidden = true;
    const save = savedGame();
    setZoomed(true);
    renderBoard(save ? save.position : 0, save ? save.position : 0);
    showPanel(
      el("h1", {}, "The Way of Life"),
      el("div", { class: "menu-buttons" },
        button("Start Game", () => {
          if (savedGame()) confirmNewGame(); else startGame();
        }, "primary"),
        el("button", { type: "button", class: "btn", disabled: !save, onclick: resumeGame }, "Resume Game"),
        button("Options", showOptions),
        button("Codex", () => showCodex("prayers")),
        button("About", showAbout),
        button("Quit", showQuit)));
    $("version").hidden = false;
  }
  function confirmNewGame() {
    showPanel(
      el("h2", {}, "Start a new game?"),
      el("p", {}, "Your saved game will be replaced."),
      el("div", { class: "row" }, button("Cancel", showMenu), button("Start New Game", startGame, "primary")));
  }
  function showQuit() {
    // A web page cannot close itself (spec 11.1).
    showPanel(
      el("p", { class: "center", style: "font-size: 1.22em; line-height: 1.6" },
        "Thanks for playing!", el("br"), "Please close the browser to exit the game.", el("br"), "AVE MARIA"),
      el("div", { class: "menu-buttons back-row" }, button("Back to Menu", showMenu)));
  }
  // Text size scales all panel and challenge text; window size caps the challenge modal (spec 11.2).
  const TEXT_SIZES = { small: 0.85, medium: 1, large: 1.15 };
  const MODAL_SIZES = { compact: ["480px", "50%"], standard: ["580px", "70%"], large: ["680px", "100%"] };
  function applyOptions() {
    const root = document.documentElement.style;
    const [width, height] = MODAL_SIZES[options.modalSize] || MODAL_SIZES.compact;
    root.setProperty("--ts", TEXT_SIZES[options.textSize] || TEXT_SIZES.small);
    root.setProperty("--modal-w", width);
    root.setProperty("--modal-h", height);
  }
  function showOptions() {
    const radio = (key, value, label) => el("label", {},
      el("input", {
        type: "radio", name: key, checked: options[key] === value,
        onchange: () => { options[key] = value; store(KEYS.options, options); applyOptions(); },
      }), label);
    showPanel(
      el("h2", {}, "Options"),
      el("div", { class: "options" },
        el("p", {}, "Text size:"),
        radio("textSize", "small", "Small"),
        radio("textSize", "medium", "Medium"),
        radio("textSize", "large", "Large"),
        el("p", {}, "Challenge window size:"),
        radio("modalSize", "compact", "Compact"),
        radio("modalSize", "standard", "Standard"),
        radio("modalSize", "large", "Large"),
        el("p", {}, "Catechism questions:"),
        radio("advancedQuestions", false, "Normal questions only"),
        radio("advancedQuestions", true, "Include advanced questions from the Catechism of the Catholic Church"),
        el("p", {}, "Prayers:"),
        radio("easyPrayers", false, "All prayers"),
        radio("easyPrayers", true, "Easy prayers only: " + DATA.prayers.filter(p => p.easy).map(p => p.english.title.replace(/^The /, "")).join(", ")),
        el("p", {}, "Saints:"),
        radio("lesserSaints", false, "Well-known saints only"),
        radio("lesserSaints", true, "Include lesser-known saints"),
        el("p", {}, "Moral dilemmas at the Cross:"),
        radio("includeOlder", false, "All Ages only"),
        radio("includeOlder", true, "Include Ages 13 and up")),
      el("div", { class: "back-row" }, button("Back", showMenu)));
  }
  function showAbout() {
    showPanel(
      el("h2", {}, "About"),
      el("p", {}, "The Way of Life is a game of the Catholic faith. Climb the rosary from the waters of Baptism to the Cross: answer catechism questions, name the saints, pray in English or Latin, and face a moral choice at the summit."),
      el("p", {}, "The game collects no personal data. ",
        el("a", { href: "https://johnamarante.github.io/the-way-of-life/privacy.html", target: "_blank", rel: "noopener" }, "Privacy policy")),
      el("p", {}, "Picture credits (Creative Commons images from Wikimedia Commons):"),
      el("ul", { class: "credits" }, DATA.credits.map(c => el("li", {},
        `${c.subject}: `, el("a", { href: c.url, target: "_blank", rel: "noopener" }, c.image), ` (${c.license})`))),
      el("div", { class: "back-row" }, button("Back", showMenu)));
  }

  // Prayers, questions and the all-ages dilemmas are always open, for learning; saints open as they are
  // won, and the rest with the victories in spec 10.
  function showCodex(tab) {
    const tabs = [["prayers", "Prayers"], ["saints", "Saints"], ["questions", "Questions"], ["dilemmas", "Dilemmas"]];
    const tabRow = el("select", { class: "codex-pick", "aria-label": "Codex section", onchange: e => showCodex(e.target.value) },
      tabs.map(([key, label]) => el("option", { value: key, selected: key === tab }, label)));
    let count = null;
    const list = el("div", { class: "codex-list" });
    const entry = (open, summary, body) => open
      ? el("details", { class: "entry" }, el("summary", {}, summary), el("div", { class: "body" }, body))
      : el("details", { class: "entry locked", onclick: e => e.preventDefault() }, el("summary", {}, summary));

    if (tab === "prayers") {
      DATA.prayers.forEach(p => list.append(entry(true, p.english.title,
        el("div", { class: "prayer-text" },
          el("h3", {}, "English"), p.english.segments.map(s => el("p", {}, s)),
          el("h3", {}, "Latin: " + p.latin.title), p.latin.segments.map(s => el("p", {}, s))))));
    } else if (tab === "saints") {
      const isOpen = s => codex.allSaints || codex.saints.includes(s.name);
      const known = DATA.saints.filter(isOpen).length;
      count = `${known} of ${DATA.saints.length} saints`;
      // Alphabetical by name, without the "St." every entry would otherwise start with (spec 10).
      const shortName = name => name.replace(/^St\. /, "");
      const sortKey = name => shortName(name).replace(/^The /, "");
      const sorted = [...DATA.saints].sort((a, b) => sortKey(a.name).localeCompare(sortKey(b.name), "en", { sensitivity: "base" }));
      sorted.forEach(s => {
        const open = isOpen(s);
        list.append(entry(open,
          [open ? el("img", { class: "thumb", src: PORTRAITS[s.portrait], alt: "" }) : el("span", { class: "thumb blank" }, "?"),
            open ? shortName(s.name) : "Locked saint"],
          el("div", {}, s.year ? el("p", {}, el("em", {}, "Born: "), s.year) : null,
            el("img", { class: "portrait", src: PORTRAITS[s.portrait], alt: s.name }),
            el("ol", { class: "clues" }, s.clues.map(c => el("li", {}, c))))));
      });
    } else if (tab === "questions") {
      const normal = DATA.questions.filter(q => !q.advanced);
      const advanced = DATA.questions.filter(q => q.advanced);
      count = `${normal.length} questions, ${advanced.length} advanced questions`;
      const show = q => list.append(q.pictures.length
        ? entry(true, [el("img", { class: "thumb", src: OBJECT_PICTURES[q.pictures[0]], alt: "" }), q.correct],
          el("div", {}, el("div", { class: "picture-row" }, q.pictures.map(p => el("img", { src: OBJECT_PICTURES[p], alt: q.correct }))), el("p", {}, q.answer)))
        : entry(true, q.question, el("p", {}, q.answer)));
      normal.forEach(show);
      if (advanced.length) list.append(el("h3", { class: "codex-heading" }, "Advanced questions"));
      advanced.forEach(show);
    } else {
      const isOpen = d => d.allAges || codex.olderDilemmas || codex.dilemmas.includes(d.id);
      count = `${DATA.dilemmas.filter(isOpen).length} of ${DATA.dilemmas.length} dilemmas`;
      DATA.dilemmas.forEach(d => {
        const open = isOpen(d);
        list.append(entry(open, open ? d.title : "Locked dilemma",
          el("div", {}, el("p", {}, d.dilemma), el("p", {}, el("strong", {}, d.question)),
            el("p", {}, el("em", {}, "Right choice: "), d.right), teaching(d))));
      });
    }
    showPanel(el("h2", {}, "Codex"), tabRow, count ? el("p", { class: "codex-count" }, count) : null, list,
      el("div", { class: "back-row" }, button("Back", showMenu)));
  }

  // ---- Title screen (spec 1.3): the whole board, the only time it is all in view ----------------

  function showTitle() {
    const title = $("title");
    const save = savedGame();
    renderBoard(save ? save.position : 0, save ? save.position : 0);
    if (window.matchMedia("(hover: none)").matches) $("title-prompt").textContent = "Tap for Main Menu";
    // The words step aside while the mouse is outside the window, leaving the board unobstructed.
    document.addEventListener("mouseout", e => { if (!e.relatedTarget) title.classList.add("away"); });
    document.addEventListener("mouseover", () => title.classList.remove("away"));
    const open = () => {
      if (title.hidden) return;
      title.hidden = true;
      showMenu();
    };
    title.addEventListener("click", open);
    title.addEventListener("keydown", e => {
      if (e.key === "Enter" || e.key === " ") { e.preventDefault(); open(); }
    });
    title.focus({ preventScroll: true });
  }

  $("menu-btn").addEventListener("click", leaveGame);
  applyOptions();
  showTitle();
  // Animate later moves of the board, but not its first placement.
  requestAnimationFrame(() => requestAnimationFrame(() => board.classList.add("moving")));
})();
