// Tests for rules.js. Run from the repo root after `python tools/build_game_data.py`:
//   node browser/rules.test.js
"use strict";
const assert = require("assert");
const path = require("path");
const fs = require("fs");
const R = require("./rules.js");

const data = JSON.parse(fs.readFileSync(path.join(__dirname, "..", "android", "app", "src", "main", "assets", "game-data.json"), "utf8"));
let seed = 42;
const rng = () => ((seed = (seed * 1103515245 + 12345) % 2147483648) / 2147483648);
let passed = 0;
function test(name, fn) {
  try { fn(); passed++; } catch (e) { console.error("FAIL", name, "\n ", e.message); process.exitCode = 1; }
}
const saintIndex = name => data.saints.findIndex(s => s.name === name);
function judge(guess) {
  const r = R.judgeGuess(guess, data.saints, data.equivalents);
  return r.result === "accepted" ? data.saints[r.saint].name : r.result;
}

test("board: decades, glory beads, destinations", () => {
  assert.deepStrictEqual([0, 9, 10, 11, 43, 44, 54, 55, 57, 58].map(R.decadeOf), [1, 1, 1, 2, 4, 5, 5, 6, 6, null]);
  assert.deepStrictEqual([10, 21, 32, 43, 54].map(R.isGlory), [true, true, true, true, true]);
  assert.strictEqual(R.isGlory(55), false);
  assert.strictEqual(R.gloryDestination(10, false), 11);
  assert.strictEqual(R.gloryDestination(10, true), 16);
  assert.strictEqual(R.gloryDestination(54, false), 55);
  assert.strictEqual(R.gloryDestination(54, true), 58);
});

test("prayer times match spec 8.3", () => {
  const expected = { 4: 25, 5: 30, 6: 40, 7: 45, 8: 50, 10: 65, 11: 70, 12: 80, 13: 85, 14: 90, 20: 130 };
  for (const [n, t] of Object.entries(expected)) assert.strictEqual(R.prayerSeconds(Number(n)), t, `segments ${n}`);
});

test("scramble never returns the correct order", () => {
  const segs = ["a", "b", "c", "d"];
  for (let i = 0; i < 200; i++) assert.ok(!R.isInOrder(R.scramble(segs, rng), segs));
});

test("name matching: spec 6.4 worked examples", () => {
  const cases = {
    "ignatius loyola": "St. Ignatius of Loyola", "Ignatius of Loyola": "St. Ignatius of Loyola", "iGNATIUS lOYOLA": "St. Ignatius of Loyola",
    "ignatius": "vague", "augustine": "St. Augustine of Hippo", "bernadette": "St. Bernadette Soubirous", "teresa": "vague",
    "borromeo": "St. Charles Borromeo", "boromeo": "St. Charles Borromeo", "charles": "vague", "carlos": "vague",
    "acutis": "St. Carlo Acutis", "paul": "St. Paul", "mary": "The Blessed Virgin Mary",
    "mother teresa": "St. Teresa of Calcutta (Mother Teresa)", "Padre Pio (Pio of Pietrelcina)": "St. Padre Pio (Pio of Pietrelcina)",
    "juan": "vague", "jean": "vague", "juan diego": "St. Juan Diego", "simon": "vague",
    "Thérèse": "St. Thérèse of Lisieux", "therese": "St. Thérèse of Lisieux",
    "teres of lisieux": "St. Thérèse of Lisieux", "terese of lisyeux": "St. Thérèse of Lisieux", "terez of leezyuh": "St. Thérèse of Lisieux", "Ávila": "St. Teresa of Ávila",
    "michael": "St. Michael the Archangel", "archangel": "vague", "louis 9": "St. Louis IX", "William": "St. William of Gellone",
    "zzzz": "none", "  St. ": "ignored",
  };
  for (const [guess, want] of Object.entries(cases)) assert.strictEqual(judge(guess), want, `guess ${JSON.stringify(guess)}`);
});

test("every saint's own name and aliases resolve to that saint", () => {
  data.saints.forEach((s, i) => {
    assert.strictEqual(judge(s.name), s.name, s.name);
    s.variants.forEach(v => assert.strictEqual(judge(v.join(" ")), s.name, `${s.name}: ${v.join(" ")}`));
  });
});

test("streak triggers a saint challenge on the fifth correct answer", () => {
  const s = R.newGame(data, rng);
  const results = [1, 2, 3, 4, 5].map(() => R.answerCorrect(s));
  assert.deepStrictEqual(results.map(r => r.saintChallenge), [false, false, false, false, true]);
  assert.strictEqual(s.streak, 0);
  assert.strictEqual(s.position, 5);
  R.answerCorrect(s); R.answerWrong(s);
  assert.strictEqual(s.streak, 0);
});

test("cards: used once per decade, restored on a new decade, streak unchanged", () => {
  const s = R.newGame(data, rng);
  R.winSaint(s, "St. Paul");
  s.streak = 3;
  assert.ok(R.useCard(s, "St. Paul"));
  assert.strictEqual(s.streak, 3);
  assert.strictEqual(s.position, 1);
  assert.strictEqual(R.useCard(s, "St. Paul"), null, "second use in the same decade is refused");
  R.moveTo(s, 10); // Glory bead still belongs to decade 1
  assert.deepStrictEqual(s.used, ["St. Paul"]);
  const restored = R.completeGloryPrayer(s, true).restored;
  assert.strictEqual(s.position, 16);
  assert.deepStrictEqual(restored, ["St. Paul"]);
  assert.deepStrictEqual(s.used, []);
});

test("questions are drawn without repeats, then reshuffled; advanced only when included", () => {
  const normal = data.questions.filter(q => !q.advanced).length;
  const advanced = data.questions.length - normal;
  assert.ok(normal >= 90 && advanced >= 200, `${normal} normal, ${advanced} advanced`);
  const s = R.newGame(data, rng);
  const seen = new Set();
  for (let i = 0; i < normal; i++) seen.add(R.drawQuestion(s, data, false, rng));
  assert.strictEqual(seen.size, normal, "each normal question once per pass");
  assert.ok([...seen].every(i => !data.questions[i].advanced), "no advanced questions by default");
  const t = R.newGame(data, rng);
  const all = new Set();
  for (let i = 0; i < data.questions.length; i++) all.add(R.drawQuestion(t, data, true, rng));
  assert.strictEqual(all.size, data.questions.length, "with advanced on, every question once per pass");
  const old = R.newGame(data, rng);
  old.qDeck = old.qDeck.slice(0, 83); // a deck saved before the bank grew
  R.drawQuestion(old, data, false, rng);
  assert.strictEqual(old.qDeck.length, data.questions.length, "an old deck is replaced");
});

test("dilemmas respect the age filter and do not repeat until exhausted", () => {
  const s = R.newGame(data, rng);
  const allAges = data.dilemmas.filter(d => d.allAges).length;
  const ids = [];
  for (let i = 0; i < allAges; i++) ids.push(data.dilemmas[R.pickDilemma(s, data, false, rng)].id);
  assert.strictEqual(new Set(ids).size, allAges);
  assert.ok(ids.every(id => data.dilemmas.find(d => d.id === id).allAges));
  const next = data.dilemmas[R.pickDilemma(s, data, false, rng)].id;
  assert.notStrictEqual(next, ids[ids.length - 1], "the dilemma just failed is not drawn first after a reshuffle");
});

test("penance: back to bead 1, then a random Decade 5 bead with cards restored", () => {
  const s = R.newGame(data, rng);
  R.winSaint(s, "St. Paul");
  R.moveTo(s, 55); s.used = ["St. Paul"]; s.streak = 2;
  R.moveTo(s, 58);
  R.startPenance(s);
  assert.strictEqual(s.position, 0);
  assert.ok(s.penance);
  const { index, restored } = R.finishPenance(s, rng);
  assert.ok(index >= 44 && index <= 53);
  assert.strictEqual(s.decade, 5);
  assert.deepStrictEqual(restored, ["St. Paul"]);
  assert.strictEqual(s.streak, 2, "penance keeps the streak");
  assert.ok(!s.penance);
});

test("score: questions and prayer challenges, perfect only with every one right", () => {
  const s = R.newGame(data, rng);
  assert.ok(R.isPerfect(s), "an empty score counts as perfect");
  R.answerCorrect(s); R.answerCorrect(s);
  R.winSaint(s, "St. Paul"); R.useCard(s, "St. Paul"); // a saint card is neither right nor wrong
  R.moveTo(s, 10); R.completeGloryPrayer(s, false);
  assert.deepStrictEqual([s.questionsRight, s.questionsAsked, s.prayersPassed, s.prayersTried], [2, 2, 1, 1]);
  assert.ok(R.isPerfect(s));
  R.answerWrong(s);
  assert.deepStrictEqual([s.questionsRight, s.questionsAsked], [2, 3]);
  assert.ok(!R.isPerfect(s));
  const t = R.newGame(data, rng);
  R.prayerMissed(t);
  assert.deepStrictEqual([t.prayersPassed, t.prayersTried], [0, 1]);
  assert.ok(!R.isPerfect(t));
  const u = R.newGame(data, rng);
  R.moveTo(u, 58);
  R.startPenance(u);
  R.finishPenance(u, rng);
  assert.ok(!R.isPerfect(u), "a wrong choice at the Cross spoils a perfect game");
});

test("prayer deck: every prayer once per pass, no repeat across a reshuffle, easy filter", () => {
  const n = data.prayers.length;
  const s = R.newGame(data, rng);
  for (let round = 0; round < 5; round++) {
    const drawn = Array.from({ length: n }, () => R.drawPrayer(s, data, false, rng));
    assert.strictEqual(new Set(drawn).size, n, "each prayer once per pass");
    assert.notStrictEqual(R.drawPrayer(s, data, false, rng), drawn[n - 1], "no repeat across the reshuffle");
    s.pPos -= 1; // put that prayer back so the next pass lines up
  }
  const easy = data.prayers.map((p, i) => p.easy ? i : -1).filter(i => i >= 0);
  assert.strictEqual(easy.length, 6);
  const t = R.newGame(data, rng);
  for (let round = 0; round < 5; round++) {
    const drawn = easy.map(() => R.drawPrayer(t, data, true, rng));
    assert.deepStrictEqual([...drawn].sort((a, b) => a - b), easy, "each easy prayer once per pass");
  }
  const u = R.newGame(data, rng);
  const first = R.drawPrayer(u, data, false, rng);
  assert.strictEqual(first, u.pDeck[0], "the order is fixed at the start of the game");
});

test("pickSaint never offers a held saint", () => {
  const s = R.newGame(data, rng);
  data.saints.slice(1).forEach(x => R.winSaint(s, x.name));
  for (let i = 0; i < 20; i++) assert.strictEqual(R.pickSaint(s, data, rng), 0);
});

console.log(`${passed} test groups passed${process.exitCode ? ", some FAILED" : ""}`);
