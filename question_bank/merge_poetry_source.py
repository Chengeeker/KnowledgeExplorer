"""Reproducible merge of age-appropriate poetry questions into Knowledge Explorer."""
from pathlib import Path
from collections import Counter
import json
root=Path(__file__).resolve().parent
source=root/"source/low_grade_poetry.psv"
bank=root/"export/questions.json"
lines=source.read_text("utf8").splitlines()
assert lines.pop(0).startswith("grade|poem|question|answer|")
extra=[]
counts=Counter()
for line_no,line in enumerate(lines,2):
    cells=line.split("|")
    assert len(cells)==8,(line_no,len(cells))
    grade_s,poem,question,answer,*rest=cells
    w1,w2,w3,expl=rest
    grade=int(grade_s)
    assert grade in (1,2) and poem.split("（")[0] in question
    assert len({answer,w1,w2,w3})==4 and len(expl)>=25,(line_no,question)
    counts[grade]+=1
    extra.append({"id":f"LGP{grade}-{counts[grade]:04d}","grade":grade,
        "domain":"语文文学","topic":"古诗启蒙" if grade==1 else "古诗阅读",
        "kind":"单选题","stars":grade,"question":question,"answer":answer,
        "options":[answer,w1,w2,w3],"explanation":expl,
        "source":"https://www.gushiwenku.cn/xuanji/xiaoxue-gushi/",
        "source_explanation":expl,"explanation_method":"manual_low_grade",
        "poem":poem})
assert counts==Counter({1:26,2:24}),counts
existing=[q for q in json.loads(bank.read_text("utf8"))
          if not q["id"].startswith("LGP")]
assert len(existing)==1200,len(existing)
merged=[]
for g in range(1,7):
    merged.extend(q for q in existing if q["grade"]==g)
    merged.extend(q for q in extra if q["grade"]==g)
assert len({q["id"] for q in merged})==1250
assert Counter(q["grade"] for q in merged)==Counter({1:226,2:224,3:200,4:200,5:200,6:200})
bank.write_text(json.dumps(merged,ensure_ascii=False,indent=2)+"\n","utf8")
print("LOW_GRADE_POETRY_MERGED",dict(counts),"TOTAL",len(merged))
