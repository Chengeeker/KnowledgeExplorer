"""Reproducible, idempotent addition of the 40 poems photographed by the user.
Run AFTER merge_poetry_source.py. Each poster poem gets three different
question types: author/source, couplet continuation, and poetic meaning.
"""
from collections import Counter
from pathlib import Path
import json
import random

base=Path(__file__).resolve().parent
source=base/"source/poster_poetry.psv"
bank=base/"export/questions.json"
rows=source.read_text("utf8").splitlines()
assert rows.pop(0)=="grade|poem|author|line|next|term|meaning"
poems=[]
for index,row in enumerate(rows,1):
    cells=row.split("|")
    assert len(cells)==7,(index,len(cells))
    grade,title,author,line,next_line,term,meaning=cells
    grade=int(grade)
    assert grade in range(1,5) and all(cells)
    assert term in line+next_line or term in (line+next_line+"（这首诗的情景）"),(title,term)
    poems.append(dict(grade=grade,poem=title,author=author,line=line,
        next=next_line,term=term,meaning=meaning))
assert len(poems)==40 and len({p["poem"] for p in poems})==40
assert Counter(p["grade"] for p in poems)=={1:13,2:8,3:12,4:7}
all_authors=list(dict.fromkeys(p["author"] for p in poems))
all_lines=[p["next"] for p in poems]
wrong_meanings=[
    "夜里所有人都已经睡着了", "清晨天空正在打雷下雨",
    "小朋友正在城里购买玩具", "冬天的河水完全结成了冰",
    "每个人都在教室里考试", "诗人正在海上寻找小船",
    "一群大雁飞到了沙漠深处", "一轮太阳从海底升起来",
    "大树上的果子已经全部成熟", "天空中忽然出现了一道彩虹",
    "正在山脚下点起明亮的灯笼", "公园里正在举行一场比赛"
]
new=[]
counter=Counter()
existing=[q for q in json.loads(bank.read_text("utf8"))
          if not q["id"].startswith("PST")]
assert len(existing)==1250,len(existing)
existing_texts={q["question"] for q in existing}
for index,p in enumerate(poems):
    title=p["poem"]
    grade=p["grade"]
    term=p["term"]
    choices=[
        (f"《{title}》的作者或出处是哪一项？",p["author"],
          [a for a in all_authors if a!=p["author"]],
          f"《{title}》的作者或出处是{p['author']}。这首作品也收录在家长提供的《小学生必背古诗文》挂图中，可以结合诗句和画面一起学习。"),
        (f"《{title}》里“{p['line']}”的下一句是哪一句？",p["next"],
          [s for s in all_lines if s!=p["next"]],
          f"《{title}》的原句是“{p['line']}，{p['next']}”。记忆诗句时可以联系上下句所描写的情景，不必只靠死记硬背。"),
        (f"《{title}》中“{term}”表达的意思最接近哪一项？",p["meaning"],
          [m for m in wrong_meanings if m!=p["meaning"]],
          f"《{title}》中“{term}”可以理解为“{p['meaning']}”。这道题主要考察诗句中的词语和景物，联系诗歌内容就容易理解。")
    ]
    for qi,(question,answer,wrong,explanation) in enumerate(choices):
        assert len(wrong)>=3
        assert question not in existing_texts,(title,question)
        wrong_choices=[wrong[(index*5+qi*7+j)%len(wrong)] for j in range(3)]
        assert len(set([answer]+wrong_choices))==4,(title,question)
        assert len(explanation)>=25,(title,explanation)
        counter[grade]+=1
        item={"id":f"PST{grade}-{counter[grade]:04d}",
            "grade":grade,"domain":"语文文学",
            "topic":"古诗启蒙" if grade==1 else "古诗阅读" if grade==2 else "古诗鉴赏",
            "kind":"单选题","stars":min(grade,3),
            "question":question,"answer":answer,"options":[answer]+wrong_choices,
            "explanation":explanation,
            "source":"用户提供的《小学生必背古诗文》挂图（2026-11-20）",
            "source_explanation":explanation,
            "explanation_method":"manual_poster_poetry",
            "poem":title}
        new.append(item)
        existing_texts.add(question)
assert counter=={1:39,2:24,3:36,4:21},counter
assert len(new)==120
assert Counter(q["poem"] for q in new)=={p["poem"]:3 for p in poems}
result=[]
for grade in range(1,7):
    result.extend(q for q in existing if q["grade"]==grade)
    result.extend(q for q in new if q["grade"]==grade)
assert len(result)==1370
assert len({q["id"] for q in result})==1370
assert Counter(q["grade"] for q in result)=={1:265,2:248,3:236,4:221,5:200,6:200}
bank.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n","utf8")
print("POSTER_POETRY_MERGED",len(poems),"POEMS",len(new),"NEW_QUESTIONS",
      "GRADE_COUNTS",dict(Counter(q["grade"] for q in result)))
