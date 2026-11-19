#!/usr/bin/env python3
"""Convert verified multiple-choice and answer-only XLSX into an offline question bank.
Uses Python standard library. Does not invent missing distractors or modify source XLSX.
"""
from pathlib import Path
from collections import Counter
from shutil import copy2
import json, re, zipfile, xml.etree.ElementTree as ET
from explanation_enrichment import enrich, TOPIC_TIPS
BASE=Path(__file__).resolve().parent
ORIGINAL=BASE.parent
SOURCE=BASE/"source"
OUTPUT=BASE/"export"/"questions.json"
NS={"m":"http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "r":"http://schemas.openxmlformats.org/officeDocument/2006/relationships"}
def read_sheet(path, name="总题库"):
    with zipfile.ZipFile(path) as z:
        ss=[]
        if "xl/sharedStrings.xml" in z.namelist():
            root=ET.fromstring(z.read("xl/sharedStrings.xml"))
            ss=["".join(t.text or "" for t in e.findall(".//m:t",NS)) for e in root.findall("m:si",NS)]
        book=ET.fromstring(z.read("xl/workbook.xml"))
        relroot=ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))
        refs={e.attrib["Id"]:e.attrib["Target"].lstrip("/") for e in relroot}
        sheet=next(s for s in book.findall(".//m:sheet",NS) if s.attrib["name"]==name)
        target=refs[sheet.attrib["{"+NS["r"]+"}id"]]
        path_in_zip=target if target.startswith("xl/") else "xl/"+target
        tab=ET.fromstring(z.read(path_in_zip))
        for row in tab.findall(".//m:sheetData/m:row",NS):
            record={}
            for cell in row.findall("m:c",NS):
                col=re.match("[A-Z]+",cell.attrib["r"]).group()
                val=cell.find("m:v",NS)
                if cell.attrib.get("t")=="inlineStr":
                    record[col]="".join(t.text or "" for t in cell.findall(".//m:t",NS))
                elif val is None:
                    record[col]=""
                elif cell.attrib.get("t")=="s":
                    record[col]=ss[int(val.text)]
                else:
                    record[col]=val.text or ""
            yield record
def main():
    SOURCE.mkdir(parents=True,exist_ok=True)
    OUTPUT.parent.mkdir(parents=True,exist_ok=True)
    for original in ORIGINAL.glob("*.xlsx"):
        copy2(original,SOURCE/original.name)
    choices_file=next(SOURCE.glob("*四选一版.xlsx"))
    answers_file=next(SOURCE.glob("*答案版.xlsx"))
    choices=list(read_sheet(choices_file))[1:]
    answers={row.get("A"):row for row in list(read_sheet(answers_file))[1:]}
    errors=[]
    conversions=[]
    parsed=[]
    for r in choices:
        qid=r.get("A","").strip()
        if not qid: continue
        opts=[r.get(col,"").strip() for col in ("H","I","J","K")]
        answer=r.get("L","").strip()
        expected=answers.get(qid)
        if expected is None:
            errors.append(f"{qid}: missing in answer-only source")
        elif expected.get("L","").strip()!=answer or expected.get("G","").strip()!=r.get("G","").strip():
            old_question=expected.get("G","").strip()
            old_answer=expected.get("L","").strip()
            if old_question.startswith("判断正误") and old_answer in ("正确","错误"):
                conversions.append({"id":qid,"conversion_type":"判断题→四选一",
                    "four_choice_question":r.get("G","").strip(),
                    "answer_only_question":old_question,"four_choice_answer":answer,
                    "answer_only_answer":old_answer})
            else:
                errors.append(f"{qid}: unexpected question or answer difference")
        if len(opts)!=4 or len(set(opts))!=4 or any(not o for o in opts):
            errors.append(f"{qid}: incomplete or duplicate choices")
        if opts.count(answer)!=1: errors.append(f"{qid}: answer is not unique")
        if r.get("T","").strip() and "ABCD"[opts.index(answer)]!=r["T"].strip() if answer in opts else False:
            errors.append(f"{qid}: correct-choice letter mismatch")
        grade_match=re.match(r"(\d+)",r.get("B",""))
        if not grade_match: errors.append(f"{qid}: missing grade")
        parsed.append({
            "id":qid,"grade":int(grade_match.group(1)) if grade_match else 1,
            "domain":r.get("C","").strip(),"topic":r.get("D","").strip(),
            "kind":r.get("S",r.get("E","")).strip(),
            "stars":int(r.get("F","1")),"question":r.get("G","").strip(),
            "answer":answer,"options":opts,"explanation":r.get("M","").strip(),
            "source":r.get("N","").strip()
        })
    if len(parsed)!=1200: errors.append(f"Expected 1200 questions, found {len(parsed)}")
    if errors:
        print(json.dumps({"errors":errors[:35],"error_count":len(errors)},ensure_ascii=True))
        raise SystemExit(1)
    if missing := set(q['topic'] for q in parsed) - set(TOPIC_TIPS):
        raise ValueError(f"Unreviewed topic explanation hints: {sorted(missing)}")
    for q in parsed:
        q['source_explanation'] = q['explanation']
        q['explanation'], q['explanation_method'] = enrich(q)
    assert all(len(q['explanation']) >= 25 for q in parsed)
    assert all('对应的正确答案是' not in q['explanation'] for q in parsed)
    assert all('由原判断题转换' not in q['explanation'] for q in parsed)
    OUTPUT.write_text(json.dumps(parsed,ensure_ascii=False,separators=(",",":")),encoding="utf-8")
    # Supplements live outside the Excel workbooks, so every rebuild MUST
    # merge them again or the first/second-grade literature disappears.
    from runpy import run_path
    run_path(str(BASE/"merge_poetry_source.py"))
    (BASE/"judgment_conversions.json").write_text(json.dumps(conversions,ensure_ascii=False,indent=2),encoding="utf-8")
    (BASE/"review_mismatches.json").unlink(missing_ok=True)
    print(json.dumps({"judgment_to_four_choice_conversions":len(conversions),"unexpected_differences":0,"questions":len(parsed),"grades":dict(sorted(Counter(q["grade"] for q in parsed).items())),
        "domains":dict(Counter(q["domain"] for q in parsed)), "topics":len(set(q["topic"] for q in parsed)),
        "difficulty":dict(Counter(q["stars"] for q in parsed)),
        "output":str(OUTPUT),"size_bytes":OUTPUT.stat().st_size},ensure_ascii=True))
if __name__=="__main__": main()
