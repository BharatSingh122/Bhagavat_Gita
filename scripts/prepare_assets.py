#!/usr/bin/env python3
"""
Builds the JSON seed files in app/src/main/assets from the raw Gita data dumps.

Usage:
  python3 scripts/prepare_assets.py <raw_dir> app/src/main/assets

<raw_dir> must contain: chapters.json, verse.json, translation.json,
anmol_vichar.json, chalisa.json (the raw files shipped with the project brief).
"""
import json, re, sys, pathlib

raw = pathlib.Path(sys.argv[1]); out = pathlib.Path(sys.argv[2]); out.mkdir(parents=True, exist_ok=True)

def find(stem):
    m = sorted(raw.glob(f"*{stem}*.json"))
    if not m: sys.exit(f"missing {stem}.json in {raw}")
    return json.loads(m[0].read_text(encoding="utf-8"))

HINDI_AUTHOR, ENGLISH_AUTHOR = 1, 16          # Swami Ramsukhdas / Swami Sivananda
NOTE = re.compile(r"\(टिप्पणी\s*प0\s*[\d.]+\)")
VERSE_TAG = re.compile(r"।।\s*\d+\.\d+\s*।।")

def clean_block(s):
    lines = [l.strip() for l in (s or "").replace("\r", "").split("\n")]
    return "\n".join(l for l in lines if l)

def clean_translation(s):
    s = NOTE.sub("", s or ""); s = VERSE_TAG.sub("", s)
    return re.sub(r"\s+", " ", s).strip(" -–—")

# ---------- chapters ----------
chapters = [{
    "number": c["chapter_number"], "name": c["name"], "nameTranslation": c["name_translation"],
    "nameTransliterated": c["name_transliterated"], "nameMeaning": c["name_meaning"],
    "summaryEnglish": c["chapter_summary"].strip(), "summaryHindi": c["chapter_summary_hindi"].strip(),
    "versesCount": c["verses_count"]} for c in sorted(find("chapters"), key=lambda c: c["chapter_number"])]

# ---------- slokas ----------
tr = {}
for t in find("translation"):
    tr[(t["verse_id"], t["author_id"])] = t["description"]
slokas = []
for v in sorted(find("verse"), key=lambda v: (v["chapter_number"], v["verse_number"])):
    slokas.append({
        "id": v["id"], "chapter": v["chapter_number"], "verse": v["verse_number"],
        "sanskrit": clean_block(v["text"]), "transliteration": clean_block(v["transliteration"]),
        "wordMeanings": clean_block(v["word_meanings"]),
        "hindi": clean_translation(tr.get((v["id"], HINDI_AUTHOR), "")),
        "english": clean_translation(tr.get((v["id"], ENGLISH_AUTHOR), "")),
    })
by_ref = {(s["chapter"], s["verse"]): s["id"] for s in slokas}

# ---------- quotes ----------
quotes = [{"category": g["category"], "quotes": [{"id": q["id"], "text": q["text"].strip()} for q in g["quotes"]]}
          for g in find("anmol_vichar")["gita_quotes"]]

# ---------- chalisa (normalise deity keys, drop corrupted lines) ----------
DEITY_FIX = {"Bhairva": "Bhairav", "Shitla": "Sheetla", "Vindheswari": "Vindhyeshwari"}
NON_DEVANAGARI = re.compile("[؀-ۿ]")              # Arabic-script garbage in source
# Source had typos in the opening dohas of the Hanuman Chalisa (Tulsidas, public domain): use the standard text.
TEXT_FIX = {"Hanuman": [
    "श्रीगुरु चरन सरोज रज, निज मनु मुकुरु सुधारि। बरनउँ रघुबर बिमल जसु, जो दायकु फल चारि॥",
    "बुद्धिहीन तनु जानिके, सुमिरौं पवन-कुमार। बल बुधि बिद्या देहु मोहिं, हरहु कलेस बिकार॥",
]}
chalisa = []
for c in find("gemini-code")["chalisa_data"] if list(raw.glob("*gemini-code*.json")) else find("chalisa")["chalisa_data"]:
    verses = [l.strip() for l in c["verses"] if l.strip() and not NON_DEVANAGARI.search(l)]
    deity = DEITY_FIX.get(c["deity"], c["deity"])
    chalisa.append({"id": c["id"], "deity": deity, "title": c["title"], "verses": TEXT_FIX.get(deity, verses)})

# ---------- moods -> verse mapping ----------
MOODS = [
  ("anxious", "Anxious", "& Worried", ["2.14", "2.47", "2.56", "6.5", "6.35", "9.22", "12.15", "18.66"]),
  ("confused", "Confused", "& Unsure", ["2.7", "2.41", "3.30", "3.35", "4.34", "4.38", "18.47", "18.63"]),
  ("sad", "Sad", "& Grieving", ["2.11", "2.13", "2.14", "2.20", "2.22", "2.27", "5.20", "18.54"]),
  ("angry", "Angry", "& Frustrated", ["2.56", "2.62", "2.63", "3.37", "5.23", "5.26", "16.2", "16.21"]),
  ("stressed", "Stressed", "& Overwhelmed", ["2.48", "2.70", "2.71", "5.10", "6.17", "6.26", "12.13", "18.58"]),
  ("unmotivated", "Lack of", "Motivation", ["2.3", "2.37", "2.47", "3.8", "3.19", "4.42", "6.5", "18.48"]),
  ("purpose", "Seeking", "Purpose", ["2.31", "3.21", "3.35", "4.7", "7.19", "9.27", "18.45", "18.46"]),
  ("grateful", "Grateful", "& Blessed", ["7.7", "9.26", "9.27", "10.8", "10.20", "12.13", "15.15", "18.65"]),
]
moods = []
for i, (key, title, sub, refs) in enumerate(MOODS):
    ids = []
    for r in refs:
        ch, vs = map(int, r.split("."))
        if (ch, vs) not in by_ref: sys.exit(f"mood {key}: verse {r} not found")
        ids.append(by_ref[(ch, vs)])
    moods.append({"key": key, "title": title, "subtitle": sub, "order": i, "slokaIds": ids})

def dump(name, obj):
    (out / name).write_text(json.dumps(obj, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"{name}: {(out / name).stat().st_size/1024:.0f} KB")

dump("chapters.json", chapters); dump("slokas.json", slokas); dump("quotes.json", quotes)
dump("chalisa.json", chalisa); dump("moods.json", moods)
print(len(chapters), "chapters,", len(slokas), "slokas,", sum(len(g["quotes"]) for g in quotes), "quotes,", len(chalisa), "chalisa")
missing = [s["id"] for s in slokas if not s["hindi"] or not s["english"]]
print("slokas missing a translation:", missing[:20], len(missing))
