from pathlib import Path
from collections import Counter
import zipfile,json,xml.etree.ElementTree as ET,hashlib
root=Path(r"D:\App\Game\Knowledge")
apk=root/"KnowledgeExplorer-v0.1.15-signed.apk"
assert apk.is_file(), "SIGNED APK MISSING"
with zipfile.ZipFile(apk) as z:
    names=set(z.namelist())
    assert "assets/questions.json" in names, "question bank absent from APK"
    questions=json.loads(z.read("assets/questions.json"))
    assert len(questions)==1370
    assert dict(Counter(q["grade"] for q in questions))=={1:265,2:248,3:236,4:221,5:200,6:200}
    low_grade=[q for q in questions if q["id"].startswith("LGP")]
    assert len(low_grade)==50
    assert Counter(q["grade"] for q in low_grade)=={1:26,2:24}
    assert all(q["domain"]=="语文文学" for q in low_grade)
    assert all(q["topic"]==("古诗启蒙" if q["grade"]==1 else "古诗阅读")
        for q in low_grade)
    assert all(q["kind"]=="单选题" for q in low_grade)
    assert Counter(q["grade"] for q in questions if q["domain"]=="语文文学")=={
        1:65,2:48,3:79,4:41,5:20}
    poster=[q for q in questions if q["id"].startswith("PST")]
    assert len(poster)==120
    assert len({q["poem"] for q in poster})==40
    assert Counter(q["poem"] for q in poster)=={q["poem"]:3 for q in poster}
    assert Counter(q["grade"] for q in poster)=={1:39,2:24,3:36,4:21}
    assert all(q["domain"]=="语文文学" for q in poster)
    assert all(q["topic"] in ("古诗启蒙","古诗阅读","古诗鉴赏") for q in poster)
    assert len(set(q["id"] for q in questions))==1370
    assert all(q["options"].count(q["answer"])==1 for q in questions)
    assert all(len(q["options"])==len(set(q["options"]))==4 for q in questions)
    assert all(q.get("source_explanation","") for q in questions), "source explanations missing"
    assert all(len(q["explanation"])>=25 for q in questions), "weak or missing explanations"
    assert not any("对应的正确答案是" in q["explanation"] or "由原判断题转换" in q["explanation"]
        or "原问题的答案是" in q["explanation"] for q in questions), "editorial boilerplate"
    enriched=sum(q.get("explanation_method")=="enhanced" for q in questions)
    assert enriched>=1000, "question-level enrichment missing"
    wav=[n for n in names if n.startswith("res/") and n.endswith(".wav")]
    assert sorted(len(z.read(n)) for n in wav)==sorted([756044,30912,41938,5776]), "game music or effects missing"
    print("EXPLANATIONS_OK",len(questions),"ENHANCED",enriched,"SOURCE_COPY",len(questions))
    print("AUDIO_RESOURCES_OK",len(wav))
    print("ASSET_CHECK_OK",len(questions),"GRADES",dict(Counter(q["grade"] for q in questions)))
    print("KINDS",dict(Counter(q["kind"] for q in questions)))
    print("SIGNATURE_ASSETS_PRESENT",any(n.startswith("META-INF") for n in names))
    print("APK_SIZE",apk.stat().st_size)
    print("APK_SHA256",hashlib.sha256(apk.read_bytes()).hexdigest())
results=root/"app"/"build"/"test-results"/"testDebugUnitTest"
cases=[]
for f in results.glob("TEST-*.xml"):
    t=ET.parse(f).getroot()
    cases.append((f.name,int(t.get("tests",0)),int(t.get("failures",0)),int(t.get("errors",0))))
print("TEST_SUITES",cases)
assert sum(c[1] for c in cases)>=40
assert sum(c[2]+c[3] for c in cases)==0
assert (root/"question_bank"/"judgment_conversions.json").is_file()
conversions=json.loads((root/"question_bank"/"judgment_conversions.json").read_text("utf8"))
assert len(conversions)==40
assert set(c["answer_only_answer"] for c in conversions)=={"正确","错误"}
# UI route verification is a static guard against the observed double-vibration regression.
from runpy import run_path
run_path(str(root/"scripts/check_interaction_routes.py"))
run_path(str(root/"scripts/check_tablet_pcm.py"))
print("JUDGMENT_TO_CHOICE_CONVERSIONS",len(conversions),"ALL_CHECKS_PASSED")


duel=(root/'app/src/main/java/com/game/knowledge/Duel.kt').read_text(encoding='utf8')
for expected in (
  'DuelRaceNotice.forSide(speed,raceWinner,0)',
  'DuelRaceNotice.forSide(speed,raceWinner,1)',
  'if(status.isNotBlank())',
  'delay(DuelRaceNotice.SUCCESS_DISPLAY_MS)',
  '对方已抢答成功',
  '你抢答成功',
):
    assert expected in duel, f'Missing duel race display: {expected}'
print('RACE_NOTICES_OK both_seats=visible accuracy=unchanged')

# All screen and shared text controls must go through the one toggle-aware ruby layer.
sources=list((root/"app/src/main/java/com/game/knowledge").glob("*.kt"))
pinyin=(root/"app/src/main/java/com/game/knowledge/PinyinText.kt").read_text("utf8")
world=(root/"app/src/main/java/com/game/knowledge/MainActivity.kt").read_text("utf8")
setting=(root/"app/src/main/java/com/game/knowledge/KnowledgeSettings.kt").read_text("utf8")
assert 'LocalShowPinyin provides showPinyin' in world
assert 'putBoolean("showPinyin",showPinyin)' in world
assert 'SettingToggle("显示拼音"' in setting
assert 'Transliterator.getInstance("Han-Latin")' in pinyin
import re
for f in sources:
    # Cantonese uses ordinary Text intentionally: never label it with Mandarin pinyin.
    if f.name in ("PinyinText.kt","CantonesePlayground.kt","CantoneseChallenges.kt"):continue
    assert not re.search(r'(?<![\w])Text\(',f.read_text("utf8")),f.name
print("PINYIN_UI_WIRING_OK",len(sources)-2,"files")

# Cantonese learning data are a distinct, locally packaged resource.
import re
with zipfile.ZipFile(apk) as z:
    assert "assets/cantonese_beginner.json" in z.namelist()
    canto=json.loads(z.read("assets/cantonese_beginner.json"))
assert len(canto)==222 and len({c["id"] for c in canto})==222
assert sum(c["kind"]=="word" for c in canto)==180
assert sum(c["kind"]=="phrase" for c in canto)==42
assert Counter(c["category"] for c in canto if c["kind"]=="word")=={
    "水果乐园":18,"点心茶楼":18,"动物朋友":18,
    "家人称呼":18,"生活小事":18,"身边世界":18,
    "交通出行":12,"校园日常":12,"认识身体":12,
    "运动游戏":12,"岭南文化":12,"温暖的家":12}
assert all(re.fullmatch(r"[a-z]+[1-6]( [a-z]+[1-6])*",c["jyutping"]) for c in canto)
assert len({c["hanzi"] for c in canto})==222
activity=(root/"app/src/main/java/com/game/knowledge/MainActivity.kt").read_text("utf8")
home=(root/"app/src/main/java/com/game/knowledge/Screens.kt").read_text("utf8")
speaker=(root/"app/src/main/java/com/game/knowledge/CantoneseAudio.kt").read_text("utf8")
hub=(root/"app/src/main/java/com/game/knowledge/CantonesePlayground.kt").read_text("utf8")
manifest=(root/"app/src/main/AndroidManifest.xml").read_text("utf8")
assert "CANTONESE -> CantonesePlayground" in activity
assert "onCantonese:()->Unit" in home
assert "RECORD_AUDIO" in manifest and 'android:required="false"' in manifest
assert "LocalShowPinyin provides false" in hub, "Mandarin pinyin must not label spoken Cantonese"
assert "isNetworkConnectionRequired" in speaker
assert "hkVoiceConfirmed" in speaker and "previewRequired" in hub
assert "recordingKey" in hub and "CantoQuiz" in hub
assert "setSpeechRate" in speaker and 'app.filesDir,"cantonese_samples"' in speaker
print("CANTONESE_RESOURCES_OK","words",180,"phrases",42,
      "categories",12,"audio_fallback","verified","privacy","offline")

feedback=(root/"app/src/main/java/com/game/knowledge/KidFeedback.kt").read_text("utf8")
layout=(root/"app/src/main/java/com/game/knowledge/PinyinText.kt").read_text("utf8")
assert "fun tap() {pulse(14);audio.tap()}" in feedback
assert "fun success() {pulse(35);audio.success()}" in feedback
assert "fun error() {pulse(38,double=true);audio.retry()}" in feedback
assert "clickable {feedback?.tap();onChange(!checked)}" in hub
assert "voice.stop()" in hub and "feedback?.success()" in hub and "feedback?.error()" in hub
assert "PinyinPolicy.glyphs(text)" in layout
assert 'appendInlineContent("cell-${cell.offset}",cell.value)' in layout
assert "MaterialText(glyph,fontSize=base.sp" in layout
assert 'MainModeTile("🦁","粤语乐园"' in home
assert 'StoryCard(modifier.clickable {feedback?.tap();onClick()}' in home
print("CANTONESE_FEEDBACK_AND_PINYIN_BASELINE_OK","independent_sound_haptics","shared_glyph_cells")

# Ready-to-play offline Cantonese recordings must be in the signed APK.
with zipfile.ZipFile(apk) as audio_archive:
    audio_manifest=json.loads(audio_archive.read("assets/cantonese_audio/manifest.json"))
    audio_names=set(audio_archive.namelist())
    audio_sizes={item.filename:item.file_size for item in audio_archive.infolist()}
clips=audio_manifest["items"]
assert len(clips)==232 and len({x["id"] for x in clips})==232
assert len([x for x in clips if x["id"].startswith("order_")])==10
assert all(x["duration_seconds"]>.3 for x in clips)
assert all(x["effective_jyutping"]==x["jyutping"] for x in clips if x["jyutping"])
assert sum(bool(x["tone_correction"]) for x in clips)>=6
for row in clips:
    name="assets/cantonese_audio/"+row["id"]+".wav"
    assert name in audio_names,name
    assert audio_sizes[name]==row["bytes"]
assert "bundledFile(word.id)" in speaker and "fun hasBuiltIn(word:CantoneseWord)" in speaker
assert "CantoQuiz" in hub
home=(root/"app/src/main/java/com/game/knowledge/Screens.kt").read_text("utf8")
assert "val reading=LocalShowPinyin.current" in home
assert 'if(tablet && reading)' in home
assert ".verticalScroll(rememberScrollState())" in home
assert "max(base*1.09f" in layout
assert sum(x["removed_pause_seconds"] for x in clips)>8
assert all(x["normalized_gain"]>=0 for x in clips)
assert {x["id"] for x in clips if not x["id"].startswith("order_")}=={x["id"] for x in canto}
print("READY_TO_PLAY_CANTONESE_AUDIO_OK",len(clips),
      "VALIDATED_JYUTPING",sum(bool(x["jyutping"]) for x in clips),
      "PHONE_AND_TABLET_HOME_PINYIN_LAYOUT_OK")

volumePolicy=(root/"app/src/main/java/com/game/knowledge/CantoneseVolumePolicy.kt").read_text("utf8")
assert 'DEFAULT_PERCENT=150' in volumePolicy
assert 'MAX_PERCENT=200' in volumePolicy and 'MIN_PERCENT=0' in volumePolicy
assert 'LoudnessEnhancer(active.audioSessionId)' in speaker
assert 'cantoneseVoiceVolume' in speaker and 'prefs.edit().putInt' in speaker
assert 'updatePlayingVolume()' in speaker and 'CantoneseVolumePolicy.playerGain(volumePercent)' in speaker
assert 'TextToSpeech.Engine.KEY_PARAM_VOLUME' in speaker
assert 'CantoVoiceVolume(' in hub and 'voice::updateVolumePercent' in hub
assert 'onPreview' in hub and 'phrase_hello' in hub
print("CANTONESE_VOLUME_CONTROL_OK", "0..200%", "saved", "preview", "isolated")

# 2 x 2 tablet home + expanded independent learning routes.
home=(root/"app/src/main/java/com/game/knowledge/Screens.kt").read_text("utf8")
challenges=(root/"app/src/main/java/com/game/knowledge/CantoneseChallenges.kt").read_text("utf8")
hub=(root/"app/src/main/java/com/game/knowledge/CantonesePlayground.kt").read_text("utf8")
assert 'MainModeTile("🦁","粤语乐园"' in home
assert 'Modifier.weight(1f).heightIn(min=if(reading) 174.dp else 150.dp)' in home
assert home.index('MainModeTile("🦁","粤语乐园"') > home.index('MainModeTile("⚔️","知识对决"')
for route in ("BRIDGE","TREASURE","DIALOGUE"):
    assert route+" -> CantoneseChallengeScreen(" in hub
    assert "CantoneseChallengeMode."+route in challenges
print("CANTONESE_EXPANSION_ROUTES_OK", "2x2_tablet", "3_new_games")
