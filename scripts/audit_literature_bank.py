from collections import Counter
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
questions=json.loads((root/"question_bank/export/questions.json").read_text("utf8"))
literature=[q for q in questions if q["domain"]=="语文文学"]
topic_counts=Counter(q["topic"] for q in literature)
grade_counts=Counter(q["grade"] for q in literature)
assert len(questions)==1370 and len(literature)==253
assert grade_counts=={1:65,2:48,3:79,4:41,5:20}
assert topic_counts=={"古诗启蒙":65,"古诗阅读":48,"古诗鉴赏":57,"古诗作者":29,
                      "文学作品":20,"传统文学深入":20,"语言文字":14}
assert all(q["grade"] in (1,2) and q["domain"]=="语文文学"
    for q in questions if q["id"].startswith("LGP"))
report={"total":len(questions),"literature":len(literature),"grades":dict(grade_counts),
        "topics":dict(topic_counts),"examples":{t:[{"q":q["question"],"a":q["answer"]}
            for q in literature if q["topic"]==t][:6] for t in topic_counts}}
target=root/"build/diagnostics/literature_audit_v0111.json"
target.write_text(json.dumps(report,ensure_ascii=False,indent=2),"utf8")
print("LITERATURE_OK_253_1370",target)
