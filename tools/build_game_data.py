"""Builds the game data shared by the Android and browser versions.

Reads the CSVs, the bead map and the portrait credits from the repo root and produces one JSON
document. Run it directly to refresh the Android assets:

    python tools/build_game_data.py

browser/build.py imports build_data() and embeds the same document in index.html.
"""
import csv
import json
import re
import shutil
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ANDROID_ASSETS = ROOT / "android" / "app" / "src" / "main" / "assets"
OBJECTS = ROOT / "objects"
FILLER = {"st", "saint", "of", "the", "and"}


def read_csv(name):
    with open(ROOT / name, encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def split_list(value):
    return [v.strip() for v in (value or "").split(";") if v.strip()]


def normalize(text):
    """Spec section 6.4: lowercase, strip accents, punctuation to spaces, drop filler words."""
    text = unicodedata.normalize("NFKD", text.lower())
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = re.sub(r"[^\w\s]|_", " ", text)
    return [w for w in text.split() if w not in FILLER]


def saint_variants(row):
    name = row["Saint"]
    variants = [normalize(re.sub(r"\(.*?\)", "", name))]
    paren = re.search(r"\((.*?)\)", name)
    if paren:
        variants.append(normalize(paren.group(1)))
        variants.append(normalize(name.replace("(", " ").replace(")", " ")))
    variants += [normalize(a) for a in split_list(row.get("Accepted Names"))]
    unique = []
    for v in variants:
        if v and v not in unique:
            unique.append(v)
    return unique


def display_name(name):
    return re.sub(r"^St\.\s+", "", name)


def build_questions():
    """Normal questions (St. Joseph catechism) first, then the advanced set from the Catechism of the
    Catholic Church (spec 4.1, 11.2), then the "What is this?" picture questions from both files (spec 4.5),
    so adding pictures never shifts the place of an older question in a saved game. Normal question numbers
    stay below 1000, advanced ones start at 1001, so ids never collide."""
    questions = read_questions("St_Joseph_First_Communion_Catechism_Quiz.csv", advanced=False)
    questions += read_questions("Catechism_Advanced_Questions.csv", advanced=True)
    questions = [q for q in questions if not q["pictures"]] + [q for q in questions if q["pictures"]]
    ids = [q["id"] for q in questions]
    assert len(ids) == len(set(ids)), "question numbers must be unique"
    assert all((q["id"] > 1000) == q["advanced"] for q in questions), "advanced questions are numbered from 1001"
    return questions


def read_questions(name, advanced):
    questions = []
    for row in read_csv(name):
        letter = row["Correct Answer"].strip().upper()
        true_false = not row["Answer C"].strip() and not row["Answer D"].strip()
        if true_false:
            options = ["True", "False"]
            correct = "True" if letter == "A" else "False"
        else:
            options = [row[f"Answer {c}"].strip() for c in "ABCD"]
            correct = row[f"Answer {letter}"].strip()
        picture = row.get("Picture", "").strip()
        questions.append({
            "id": int(row["Number"]),
            "question": row["Question"].strip(),
            "answer": row["Answer"].strip(),
            "options": options,
            "correct": correct,
            "trueFalse": true_false,
            "advanced": advanced,
            # Spec 4.5: one of these photos of the object is shown, picked at random each time.
            "pictures": object_pictures(picture) if picture else [],
        })
    return questions


def object_pictures(name):
    """Photos of an object in objects/, named after it: "Brown Scapular" -> brown_scapular-1.jpg, -2.jpg..."""
    slug = re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")
    files = sorted(p.name for p in OBJECTS.glob(f"{slug}-*.jpg"))
    assert 2 <= len(files) <= 5, f"{name}: needs 2 to 5 pictures named objects/{slug}-<n>.jpg, found {len(files)}"
    return files


def build_saints():
    saints = []
    for row in read_csv("Catholic_Saints_Trivia.csv"):
        name = row["Saint"].strip()
        display = display_name(name)
        saints.append({
            "name": name,
            "year": row["Year Born"].strip(),
            "clues": [row[f"Clue {i}"].strip() for i in range(1, 11)],
            "variants": saint_variants(row),
            "spellings": [w for s in split_list(row.get("Accepted Spellings")) for w in normalize(s)],
            "invocation": row.get("Invocation", "").strip() or f"Saint {display}, pray for me!",
            "portrait": row.get("Portrait", "").strip(),
            # Spec 11.2: lesser-known saints only come up with the option that includes them.
            "lesser": row.get("Lesser Known", "").strip().lower() == "true",
        })
    return saints


def build_prayers():
    rows = read_csv("Catholic_Prayers_Segments.csv")
    assert len(rows) % 2 == 0, "prayers must come in English/Latin pairs"

    def part(row):
        segments = [row[f"Segment {i}"].strip() for i in range(1, 21) if row.get(f"Segment {i}", "").strip()]
        return {"title": row["Title"].strip(), "segments": segments}

    prayers = []
    for i in range(0, len(rows), 2):
        english, latin = rows[i], rows[i + 1]
        assert english["Language"].strip().lower() == "english" and latin["Language"].strip().lower() == "latin"
        # Spec 11.2: the Easy prayers option uses only prayers marked TRUE; both languages must agree.
        easy = english["EasyPrayer"].strip().lower() == "true"
        assert easy == (latin["EasyPrayer"].strip().lower() == "true"), f"EasyPrayer differs for {english['Title']}"
        prayers.append({"easy": easy, "english": part(english), "latin": part(latin)})
    return prayers


def build_dilemmas():
    dilemmas = []
    for row in read_csv("Catholic_Moral_Dilemmas.csv"):
        dilemmas.append({
            "id": int(row["Number"]),
            "title": row["Title"].strip(),
            "allAges": row["Age Level"].strip().lower() == "all ages",
            "dilemma": row["Dilemma"].strip(),
            "question": row["Question"].strip(),
            "right": row["Right Choice"].strip(),
            "wrong": row["Wrong Choice"].strip(),
            "catholicAnswer": row["Catholic Answer"].strip(),
            "keyPrinciple": row["Key Principle"].strip(),
            "ccc": row["CCC References"].strip(),
            "sources": row["Sources"].strip(),
        })
    return dilemmas


def build_equivalents():
    return [[w.strip().lower() for w in row["Group"].split(";") if w.strip()]
            for row in read_csv("Saint_Name_Equivalents.csv")]


def build_credits():
    """Portraits and object pictures under Creative Commons licenses that need a credit line (from
    saint_portrait_sources.md and object_picture_sources.md)."""
    credits = []
    row = re.compile(r"^\|\s*\d+\s*\|\s*([^|]+?)\s*\|[^|]*\|\s*\[([^\]]*)\]\(([^)]+)\)\s*\|[^|]*\|\s*([^|]+?)\s*\|\s*Yes\s*\|")
    for name in ("saint_portrait_sources.md", "object_picture_sources.md"):
        for line in (ROOT / name).read_text(encoding="utf-8").splitlines():
            m = row.match(line)
            if m:
                credits.append({"subject": m.group(1), "image": m.group(2), "url": m.group(3), "license": m.group(4)})
    return credits


def build_board():
    beads = json.loads((ROOT / "rosary_beads.json").read_text(encoding="utf-8"))
    return {
        "imageWidth": beads["imageWidth"],
        "imageHeight": beads["imageHeight"],
        "beads": [{"x": b["x"], "y": b["y"], "r": b["r"], "glory": b["type"] == "glory"} for b in beads["beads"]],
        "cross": {k: beads["cross"][k] for k in ("x", "y", "r")},
    }


def build_data():
    data = {
        "board": build_board(),
        "questions": build_questions(),
        "saints": build_saints(),
        "prayers": build_prayers(),
        "dilemmas": build_dilemmas(),
        "equivalents": build_equivalents(),
        "credits": build_credits(),
    }
    missing = [s["portrait"] for s in data["saints"] if not (ROOT / "portraits" / s["portrait"]).exists()]
    assert not missing, f"missing portraits: {missing}"
    assert len(data["board"]["beads"]) == 58
    return data


def main():
    data = build_data()
    ANDROID_ASSETS.mkdir(parents=True, exist_ok=True)
    (ANDROID_ASSETS / "game-data.json").write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    portraits = ANDROID_ASSETS / "portraits"
    portraits.mkdir(exist_ok=True)
    for saint in data["saints"]:
        shutil.copyfile(ROOT / "portraits" / saint["portrait"], portraits / saint["portrait"])
    pictures = ANDROID_ASSETS / "objects"
    pictures.mkdir(exist_ok=True)
    for q in data["questions"]:
        for picture in q["pictures"]:
            shutil.copyfile(OBJECTS / picture, pictures / picture)
    # Sound effects (spec 13): the file name says when each one plays.
    sounds = ANDROID_ASSETS / "sounds"
    sounds.mkdir(exist_ok=True)
    for sound in sorted((ROOT / "sounds").glob("*.mp3")):
        shutil.copyfile(sound, sounds / sound.name)
    print(f"Wrote game-data.json: {len(data['questions'])} questions, {len(data['saints'])} saints, "
          f"{len(data['prayers'])} prayers, {len(data['dilemmas'])} dilemmas, {len(data['credits'])} credits; "
          f"copied {len(data['saints'])} portraits and {sum(len(q['pictures']) for q in data['questions'])} object pictures.")


if __name__ == "__main__":
    main()
