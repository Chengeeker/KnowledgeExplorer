"""Build the independent offline Cantonese learning corpus.

Two reviewed source lists retain full word meanings, Jyutping and
age-appropriate examples. Never copy whole dictionary entries.
"""
from pathlib import Path
from collections import Counter
import json,re

root=Path(__file__).resolve().parent
sources=[root/"source/cantonese_beginner.psv",
         root/"source/cantonese_expanded.psv"]
out=root/"export/cantonese_beginner.json"
items=[]
for source in sources:
    lines=source.read_text(encoding="utf8").splitlines()
    assert lines.pop(0)=="id|kind|category|hanzi|traditional|jyutping|meaning|emoji|tip"
    for rowno,line in enumerate(lines,2):
        fields=line.split("|")
        if len(fields)!=9: raise ValueError(f"{source.name}:{rowno} has {len(fields)} fields")
        id_,kind,category,hanzi,traditional,jyutping,meaning,emoji,tip=fields
        syllables=jyutping.split()
        assert kind in ("word","phrase"),(source,rowno,kind)
        assert syllables and all(re.fullmatch(r"[a-z]+[1-6]",v) for v in syllables),(source,rowno,jyutping)
        assert id_.isascii() and id_.replace("_","").isalnum()
        assert all((hanzi,traditional,meaning,emoji,tip))
        items.append(dict(id=id_,kind=kind,category=category,
                          hanzi=hanzi,traditional=traditional,
                          jyutping=jyutping,meaning=meaning,emoji=emoji,tip=tip))
assert len(items)==222,len(items)
assert len(set(x["id"] for x in items))==len(items)
dupes=Counter((x["hanzi"],x["kind"]) for x in items)
assert all(c==1 for c in dupes.values()),[k for k,v in dupes.items() if v>1]
words=Counter(x["category"] for x in items if x["kind"]=="word")
assert words=={
    "水果乐园":18,"点心茶楼":18,"动物朋友":18,
    "家人称呼":18,"生活小事":18,"身边世界":18,
    "交通出行":12,"校园日常":12,"认识身体":12,
    "运动游戏":12,"岭南文化":12,"温暖的家":12},words
assert sum(x["kind"]=="phrase" for x in items)==42
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(items,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
print("CANTONESE_ASSETS_OK","words",180,"phrases",42,
      "categories",len(words),"output",out)
