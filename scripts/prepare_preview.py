from pathlib import Path
import hashlib,shutil
ROOT=Path(r"D:\App\Game\Knowledge")
SOURCE=ROOT/"question_bank"/"source"
def sha(p):
    h=hashlib.sha256()
    with p.open("rb") as f:
        for block in iter(lambda:f.read(65536),b""):
            h.update(block)
    return h.hexdigest()
for f in ROOT.glob("*.xlsx"):
    dst=SOURCE/f.name
    if dst.exists():
        assert sha(f)==sha(dst),f"Identical backup check failed: {f.name}"
        dst.unlink()
    f.rename(dst)
LOGS=ROOT/"build"/"diagnostics"
LOGS.mkdir(parents=True,exist_ok=True)
for name in ("build-log.txt","test-log.txt","validation-log.txt"):
    f=ROOT/name
    if f.exists(): shutil.move(str(f),str(LOGS/name))
for name in ("inspect_bank.py","summarize_bank.py","inspection.jsonl"):
    (ROOT/name).unlink(missing_ok=True)
apk_source=ROOT/"app"/"build"/"outputs"/"apk"/"debug"/"app-debug.apk"
assert apk_source.is_file(),f"APK not built: {apk_source}"
apk_dest=ROOT/"KnowledgeExplorer-v0.1.0-debug.apk"
shutil.copy2(apk_source,apk_dest)
print("COPIED APK",apk_dest,"size",apk_dest.stat().st_size,"sha256",sha(apk_dest))
print("ORIGINAL SPREADSHEETS PRESERVED",(len(list(SOURCE.glob("*.xlsx")))))
print("ROOT FILES",[(p.name,p.stat().st_size) for p in ROOT.iterdir() if p.is_file()])
