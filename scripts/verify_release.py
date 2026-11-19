from pathlib import Path
from collections import Counter
import zipfile,json,xml.etree.ElementTree as ET,hashlib
root=Path(r"D:\App\Game\Knowledge")
apk=root/"KnowledgeExplorer-v0.1.8-signed.apk"
assert apk.is_file(), "SIGNED APK MISSING"
with zipfile.ZipFile(apk) as z:
    names=set(z.namelist())
    assert "assets/questions.json" in names, "question bank absent from APK"
    questions=json.loads(z.read("assets/questions.json"))
    assert len(questions)==1250
    assert dict(Counter(q["grade"] for q in questions))=={1:226,2:224,3:200,4:200,5:200,6:200}
    low_grade=[q for q in questions if q["id"].startswith("LGP")]
    assert len(low_grade)==50
    assert Counter(q["grade"] for q in low_grade)=={1:26,2:24}
    assert all(q["domain"]=="语文文学" for q in low_grade)
    assert all(q["topic"]==("古诗启蒙" if q["grade"]==1 else "古诗阅读")
        for q in low_grade)
    assert all(q["kind"]=="单选题" for q in low_grade)
    assert Counter(q["grade"] for q in questions if q["domain"]=="语文文学")=={
        1:26,2:24,3:43,4:20,5:20}
    assert len(set(q["id"] for q in questions))==1250
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
assert sum(c[1] for c in cases)>=26
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
