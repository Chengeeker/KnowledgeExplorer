import json,re,collections
from pathlib import Path
root=Path(__file__).resolve().parents[1]
a=json.loads((root/"question_bank/export/questions.json").read_text(encoding="utf8"))
patterns={"对应的正确答案":r"对应的正确答案", "本题考查":r"本题考查", "原题判断":r"由原判断题转换", "因为":r"因为", "所以":r"所以", "就是":r"就是", "符号":r"符号", "所属":r"属于", "所作":r"所作", "被称为":r"被称为"}
lines=[]
for k,p in patterns.items():
    x=[q for q in a if re.search(p,q["explanation"])]
    lines.append((k,len(x),collections.Counter(q["topic"] for q in x).most_common(14)))
lines.append(("EXPLANATION_QUESTION_SUBSTRINGS",sum(q["answer"] in q["explanation"] for q in a)))
lines.append(("BELOW_15",sum(len(q["explanation"])<15 for q in a)))
lines.append(("BOILERPLATE_EXAMPLES",[(q["id"],q["topic"],q["question"],q["explanation"]) for q in a if "对应的正确答案" in q["explanation"]][:40]))
lines.append(("JUDGMENT_EXAMPLES",[(q["id"],q["question"],q["answer"],q["explanation"]) for q in a if "由原判断题转换" in q["explanation"]][:40]))
lines.append(("DOMAINS_TOPICS",collections.Counter((q["domain"],q["topic"]) for q in a).most_common()))
out=root/"build/diagnostics/explanation_patterns.txt"
out.write_text("\n".join(str(x) for x in lines),encoding="utf8")
print(out)
