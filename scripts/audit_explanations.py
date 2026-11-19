import json,re,collections
from pathlib import Path
root=Path(__file__).resolve().parents[1]
qs=json.loads((root/"question_bank/export/questions.json").read_text(encoding="utf-8"))
lines=[]
def out(*x):lines.append(" ".join(map(str,x)))
out("TOTAL",len(qs))
by=collections.Counter(q['explanation'] for q in qs)
out("UNIQUE",len(by),"DUPLICATED EXPLANATIONS",sum(n for n in by.values() if n>1))
out("\nREPEATED HIGH:",str([(n,e) for e,n in by.most_common(40) if n>1]))
out("\nSAMPLE SHORTEST:")
for q in sorted(qs,key=lambda q:len(q["explanation"]))[:90]:
    out(q["id"],"年级",q["grade"],"领域",q["domain"],"主题",q["topic"],"题目",q["question"],"答案",q["answer"],"解析",q["explanation"])
out("\nSAMPLE RANDOM FIXED:")
import random
for q in random.Random(12).sample(qs,85):
    out(q["id"],"年级",q["grade"],"领域",q["domain"],"主题",q["topic"],"题目",q["question"],"答案",q["answer"],"解析",q["explanation"])
out("\nTOPICS:")
for (d,t),n in sorted(collections.Counter((q['domain'],q['topic']) for q in qs).items()):
    out(d,t,n)
dest=root/"build/diagnostics/explanations_audit.txt"
dest.parent.mkdir(parents=True,exist_ok=True)
dest.write_text("\n".join(lines),encoding="utf-8")
print(dest)
