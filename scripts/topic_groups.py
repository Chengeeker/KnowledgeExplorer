import json
from collections import defaultdict
from pathlib import Path
root=Path(__file__).resolve().parents[1]
a=json.loads((root/"question_bank/export/questions.json").read_text(encoding="utf8"))
chosen=["国家与大洲","地名省区","动物识别","植物分类","天气工具","天气与观察","植物部位","古诗作者","省级行政区首府"]
g=defaultdict(list)
for q in a:
 if q["topic"] in chosen and "对应的正确答案是" in q["source_explanation"]:
  g[q["topic"]].append(f"{q['id']} | {q['question']} | {q['answer']} | {q['source_explanation']}")
f=root/"build/diagnostics/topic_groups.txt"
f.write_text("\n\n".join(k+"\n"+"\n".join(g[k]) for k in chosen),encoding="utf8")
print(f)
