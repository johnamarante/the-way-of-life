// Game rules for The Way of Life (spec: the-way-of-life-rules.md). Pure functions with no DOM access,
// so they can be tested in Node: `node browser/rules.test.js`.
(function (root) {
  "use strict";

  const BEAD_COUNT = 58;
  const CROSS = 58;
  const FIRST_FINAL_BEAD = 55;
  const STREAK_FOR_SAINT = 5;
  const SAINT_SECONDS = 60;
  const FILLER = new Set(["st", "saint", "of", "the", "and"]);

  // ---- Board ----------------------------------------------------------------------------------

  // Decade used for saint cards (spec 2.3): 1-5 for the decades and their Glory beads, 6 for the final beads.
  function decadeOf(index) {
    if (index >= CROSS) return null;
    if (index >= FIRST_FINAL_BEAD) return 6;
    return Math.floor(index / 11) + 1;
  }

  function isGlory(index) {
    return index < FIRST_FINAL_BEAD && index % 11 === 10;
  }

  // Where a completed Prayer Challenge on a Glory bead sends the player (spec 8.4).
  function gloryDestination(index, latin) {
    if (index === 54) return latin ? CROSS : FIRST_FINAL_BEAD;
    return index + 1 + (latin ? 5 : 0);
  }

  // ---- Prayers --------------------------------------------------------------------------------

  // Spec 8.3: 15 s for 4 segments up to 120 s for 20, rounded to the nearest 5 s (halves up), plus 10 s.
  function prayerSeconds(segmentCount) {
    const raw = 15 + (segmentCount - 4) * 105 / 16;
    return Math.floor(raw / 5 + 0.5) * 5 + 10;
  }

  function shuffle(items, rng) {
    const a = items.slice();
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(rng() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  }

  function isInOrder(arrangement, segments) {
    return arrangement.length === segments.length && arrangement.every((s, i) => s === segments[i]);
  }

  // A shuffled copy that is never already in the correct order (spec 8.1).
  function scramble(segments, rng) {
    if (new Set(segments).size < 2) return segments.slice();
    let result;
    do { result = shuffle(segments, rng); } while (isInOrder(result, segments));
    return result;
  }

  // ---- Saint name matching (spec 6.4) -----------------------------------------------------------

  function normalize(text) {
    return text.toLowerCase()
      .normalize("NFKD").replace(/[̀-ͯ]/g, "")
      .replace(/[^\p{L}\p{N}\s]|_/gu, " ")
      .split(/\s+/)
      .filter(w => w && !FILLER.has(w));
  }

  function withinOneEdit(a, b) {
    if (a === b) return true;
    if (Math.abs(a.length - b.length) > 1) return false;
    if (a.length === b.length) {
      let diff = 0;
      for (let i = 0; i < a.length; i++) if (a[i] !== b[i] && ++diff > 1) return false;
      return true;
    }
    const [short, long] = a.length < b.length ? [a, b] : [b, a];
    for (let i = 0; i < long.length; i++) {
      if (long.slice(0, i) + long.slice(i + 1) === short) return true;
    }
    return false;
  }

  function wordMatches(guessWord, nameWord, saint, equivalents, useEquivalents) {
    if (guessWord === nameWord) return true;
    if (saint.spellings.includes(guessWord)) return true;
    if (useEquivalents && equivalents.some(g => g.includes(guessWord) && g.includes(nameWord))) return true;
    return nameWord.length >= 5 && withinOneEdit(guessWord, nameWord);
  }

  // Returns { result: "ignored" | "accepted" | "vague" | "none", saint: index or -1, candidates: [indexes] }.
  function judgeGuess(guess, saints, equivalents) {
    const g = normalize(guess);
    if (g.length === 0) return { result: "ignored", saint: -1, candidates: [] };
    const exact = [];
    saints.forEach((s, i) => {
      if (s.variants.some(v => v.length === g.length && v.every((w, k) => wordMatches(g[k], w, s, equivalents, false)))) exact.push(i);
    });
    if (exact.length === 1) return { result: "accepted", saint: exact[0], candidates: exact };
    const subset = [];
    saints.forEach((s, i) => {
      if (s.variants.some(v => g.every(gw => v.some(w => wordMatches(gw, w, s, equivalents, true))))) subset.push(i);
    });
    if (subset.length === 1) return { result: "accepted", saint: subset[0], candidates: subset };
    if (subset.length > 1) return { result: "vague", saint: -1, candidates: subset };
    return { result: "none", saint: -1, candidates: [] };
  }

  // ---- Game state ---------------------------------------------------------------------------------

  function newGame(data, rng) {
    return {
      v: 1,
      position: 0,
      streak: 0,
      decade: 1,
      cards: [],
      used: [],
      qDeck: shuffle(data.questions.map((_, i) => i), rng),
      qPos: 0,
      pDeck: shuffle(data.prayers.map((_, i) => i), rng),
      pPos: 0,
      usedDilemmas: [],
      penance: false,
      challenge: null,
      questionsRight: 0,
      questionsAsked: 0,
      prayersPassed: 0,
      prayersTried: 0,
      missedCross: false,
      // Whether the game ever used Easy prayers, or ever left out the advanced questions (spec 10).
      usedEasyPrayers: false,
      usedNormalOnly: false,
    };
  }

  // Spec 10: only a game with every catechism question, Prayer Challenge and the choice at the Cross
  // right opens the whole Codex.
  function isPerfect(state) {
    return state.questionsRight === state.questionsAsked && state.prayersPassed === state.prayersTried && !state.missedCross;
  }

  // Spec 10: what winning this game opens in the Codex. Prayers, questions and the all-ages dilemmas are
  // always open; every saint needs a perfect game on All prayers, and every dilemma for ages 13 and up a
  // perfect game with advanced questions included, each for the whole game.
  function codexUnlocks(state) {
    const perfect = isPerfect(state);
    return { perfect, allSaints: perfect && !state.usedEasyPrayers, olderDilemmas: perfect && !state.usedNormalOnly };
  }

  // The victory screen's words about the Codex.
  function codexMessages(unlocks) {
    if (!unlocks.perfect) return ['Get every catechism question, Prayer Challenge and the choice at the Cross right in one game to open more of the Codex: with All prayers, every saint; with advanced questions included, every moral dilemma for ages 13 and up.'];
    return [
      unlocks.allSaints ? 'Every saint is now open in the Codex.' : 'To open every saint in the Codex, win a perfect game with All prayers for the whole game.',
      unlocks.olderDilemmas ? 'Every moral dilemma for ages 13 and up is now open in the Codex.' : 'To open every moral dilemma for ages 13 and up, win a perfect game with advanced questions included for the whole game.',
    ];
  }

  // Draws the next catechism question without repeats, reshuffling when the bank is used up (spec 4.1).
  // The deck holds every question; advanced ones are skipped unless the option includes them (spec 11.2).
  // A deck saved before the question bank changed size is replaced.
  function drawQuestion(state, data, advanced, rng) {
    if (!advanced) state.usedNormalOnly = true;
    if (!data.questions.some(q => !q.advanced)) advanced = true;
    for (;;) {
      if (state.qPos >= state.qDeck.length || state.qDeck.length !== data.questions.length) {
        state.qDeck = shuffle(data.questions.map((_, i) => i), rng);
        state.qPos = 0;
      }
      const index = state.qDeck[state.qPos++];
      if (advanced || !data.questions[index].advanced) return index;
    }
  }

  function pickSaint(state, data, rng) {
    const held = new Set(state.cards);
    let pool = data.saints.map((_, i) => i).filter(i => !held.has(data.saints[i].name));
    if (pool.length === 0) pool = data.saints.map((_, i) => i);
    return pool[Math.floor(rng() * pool.length)];
  }

  // Spec 8.1: the prayers are shuffled into a deck at the start of the game and drawn in that order, one
  // per Prayer Challenge or penance, passed or not (the language does not matter). When the deck runs out
  // it is reshuffled, never starting with the prayer drawn last. With Easy prayers on (spec 11.2), prayers
  // not marked easy are skipped.
  function drawPrayer(state, data, easyOnly, rng) {
    if (easyOnly) state.usedEasyPrayers = true;
    if (!data.prayers.some(p => p.easy)) easyOnly = false;
    const eligible = i => !easyOnly || data.prayers[i].easy;
    for (;;) {
      if (state.pPos >= state.pDeck.length) {
        const last = state.pDeck[state.pPos - 1];
        const all = data.prayers.map((_, i) => i);
        const several = all.filter(eligible).length > 1;
        do { state.pDeck = shuffle(all, rng); } while (several && state.pDeck.find(eligible) === last);
        state.pPos = 0;
      }
      const index = state.pDeck[state.pPos++];
      if (eligible(index)) return index;
    }
  }

  // Spec 9.1: filtered by age setting, no repeats until all eligible are used; after a reshuffle the
  // dilemma drawn last (the one just failed) is not drawn first.
  function pickDilemma(state, data, includeOlder, rng) {
    const eligible = data.dilemmas.filter(d => includeOlder || d.allAges).map(d => d.id);
    let pool = eligible.filter(id => !state.usedDilemmas.includes(id));
    if (pool.length === 0) {
      const lastFailed = state.usedDilemmas[state.usedDilemmas.length - 1];
      state.usedDilemmas = state.usedDilemmas.filter(id => !eligible.includes(id));
      pool = eligible.filter(id => id !== lastFailed);
      if (pool.length === 0) pool = eligible;
    }
    const id = pool[Math.floor(rng() * pool.length)];
    state.usedDilemmas.push(id);
    return data.dilemmas.findIndex(d => d.id === id);
  }

  // Moves the player and applies the new-decade card restore (spec 2.3, 7.1). Returns the names of
  // the cards restored, so the UI can flash them.
  function moveTo(state, index) {
    state.position = index;
    const decade = decadeOf(index);
    if (decade !== null && !isGlory(index) && decade !== state.decade) {
      state.decade = decade;
      const restored = state.used;
      state.used = [];
      return restored;
    }
    return [];
  }

  // The challenge the current position asks for (spec 3).
  function challengeForPosition(state, data, rng, includeOlder, easyOnly, advanced) {
    if (state.position >= CROSS) return { kind: "dilemma", d: pickDilemma(state, data, includeOlder, rng) };
    if (isGlory(state.position)) return { kind: "prayer", prayer: drawPrayer(state, data, easyOnly, rng), penance: false };
    return { kind: "question", q: drawQuestion(state, data, advanced, rng) };
  }

  // Correct catechism answer (spec 4.3, 5): advance, count the streak. Returns { saintChallenge, restored }.
  function answerCorrect(state) {
    state.questionsRight += 1;
    state.questionsAsked += 1;
    state.streak += 1;
    const restored = moveTo(state, state.position + 1);
    const saintChallenge = state.streak >= STREAK_FOR_SAINT;
    if (saintChallenge) state.streak = 0;
    return { saintChallenge, restored };
  }

  function answerWrong(state) {
    state.questionsAsked += 1;
    state.streak = 0;
  }

  // Saint card used on an unanswered question (spec 7.2): advance, mark used, streak unchanged.
  function useCard(state, saintName) {
    if (!state.cards.includes(saintName) || state.used.includes(saintName)) return null;
    state.used.push(saintName);
    return { restored: moveTo(state, state.position + 1) };
  }

  function winSaint(state, saintName) {
    if (!state.cards.includes(saintName)) state.cards.push(saintName);
  }

  function completeGloryPrayer(state, latin) {
    state.prayersPassed += 1;
    state.prayersTried += 1;
    return { restored: moveTo(state, gloryDestination(state.position, latin)) };
  }

  // A Prayer Challenge missed by running out of time or giving up (spec 8.4) counts against the score.
  function prayerMissed(state) {
    state.prayersTried += 1;
  }

  // Wrong dilemma choice (spec 9.3): back to the waters of Baptism to do penance.
  function startPenance(state) {
    state.missedCross = true;
    state.penance = true;
    state.position = 0;
  }

  // Penance prayer completed: restored to a random bead 1-10 of Decade 5, which counts as a new decade.
  function finishPenance(state, rng) {
    state.penance = false;
    const index = 44 + Math.floor(rng() * 10);
    return { restored: moveTo(state, index), index };
  }

  const Rules = {
    BEAD_COUNT, CROSS, SAINT_SECONDS, STREAK_FOR_SAINT,
    decadeOf, isGlory, gloryDestination, prayerSeconds, shuffle, scramble, isInOrder,
    normalize, withinOneEdit, judgeGuess,
    newGame, isPerfect, codexUnlocks, codexMessages, drawQuestion, pickSaint, drawPrayer, pickDilemma, moveTo, challengeForPosition,
    answerCorrect, answerWrong, useCard, winSaint, completeGloryPrayer, prayerMissed, startPenance, finishPenance,
  };

  if (typeof module !== "undefined" && module.exports) module.exports = Rules;
  else root.Rules = Rules;
})(typeof window !== "undefined" ? window : globalThis);
