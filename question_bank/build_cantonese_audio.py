"""Rebuild production Cantonese AI speech pack and fix pathological pauses.

The model is run OFFLINE at build time (not on children's devices).
Manual Jyutping takes precedence when the model guesses another reading.
Audio is rendered as 24 kHz PCM16, normalized, and long model silences are
contracted conservatively. Originals are backed up under ignored build/ so
the postprocessor is repeatable. Audio must still be reviewed by humans.
"""
from pathlib import Path
import json,re,time, shutil
from collections import Counter
import numpy as np
import soundfile as sf
from canto_tts import CantoTTS
from canto_hk_g2p import segment

root=Path(__file__).resolve().parent.parent
items=json.loads((root/"question_bank/export/cantonese_beginner.json").read_text("utf8"))
assert len(items)==222,len(items)
orders=[i for i in items if i["category"]=="点心茶楼" and i["kind"]=="word"]
tasks=[(i["id"],i["traditional"],i["jyutping"]) for i in items]
tasks.extend((f"order_{i['id']}","唔該，我想食"+i["traditional"],None) for i in orders[:10])
assert len(tasks)==232,len(tasks)
assets=root/"app/src/main/assets/cantonese_audio"
assets.mkdir(parents=True,exist_ok=True)
backups=root/"build/diagnostics/cantonesepack_original"
backups.mkdir(parents=True,exist_ok=True)
raw=root/"build/diagnostics/cantonesepack_raw"
raw.mkdir(parents=True,exist_ok=True)
engine=CantoTTS()
print("CANTONESE_ENGINE_READY",len(tasks),"TASKS",flush=True)
syllable_re=re.compile(r"<o-[^>]*>\s*<r-[^>]*>\s*<t[1-6]>")

def reading(phoneme):
    parts=re.findall(r"<[^>]+>",phoneme)
    result=[]
    pending=""
    for part in parts:
        if part.startswith("<o-") or part.startswith("<r-"):
            pending+=part[3:-1]
        elif re.fullmatch(r"<t[1-6]>",part):
            result.append(pending+part[2:-1])
            pending=""
    return " ".join(result)

def manual_phoneme(model,expected):
    rendered=reading(model)
    if rendered==expected:return model,False
    syllables=expected.split()
    parsed=rendered.split()
    if len(parsed)==len(syllables) and all(a[:-1]==b[:-1] for a,b in zip(parsed,syllables)):
        tones=iter(syllables)
        corrected=re.sub(r"<t[1-6]>",lambda m:f"<t{next(tones)[-1]}>",model)
        if reading(corrected)==expected:return corrected,True
    found=syllable_re.findall(model)
    if len(found)!=len(syllables):
        raise ValueError(f"Manual Jyutping syllable mismatch: expected {expected}, got {rendered}")
    idx=iter(syllables)
    def repl(_):
        syllable=next(idx)
        seg=segment(syllable)
        if seg is None:raise ValueError(f"Unknown Jyutping syllable: {syllable}")
        return f"<o-{seg.onset or ''}><r-{seg.rime}><t{seg.tone}>"
    patched=syllable_re.sub(repl,model)
    if reading(patched)!=expected:
        raise ValueError(f"Corrected G2P mismatch: {reading(patched)} vs {expected}")
    return patched,True

def repair(x,rate):
    x=np.asarray(x,dtype=np.float32).reshape(-1)
    if not len(x):raise ValueError("Empty waveform")
    peak=float(np.max(np.abs(x)))
    if peak<.004:raise ValueError(f"Silent recording peak {peak}")
    # RMS frame of 10ms; include margin to protect quiet syllable beginnings.
    frame=max(1,int(rate*.01))
    frames=len(x)//frame
    level=np.sqrt(np.mean(x[:frames*frame].reshape(-1,frame)**2,axis=1))
    threshold=max(.006,min(.014,float(np.percentile(level,95))*.055))
    mask=level<threshold
    changes=np.diff(np.r_[False,mask,False].astype(np.int8))
    segments=list(zip(np.where(changes==1)[0],np.where(changes==-1)[0]))
    removed=0
    # Trim only fully internal silent spans >=350ms; preserve 180ms for natural rhythm.
    for a,b in reversed(segments):
        if a<8 or b>frames-8:continue
        if (b-a)*.01<.34:continue
        desired=18
        trim=(b-a)-desired
        if trim<=0:continue
        # Leave 9 frames of context on each side, cut silent middle only.
        cut_left=(a+9)*frame
        cut_right=(b-9)*frame
        if cut_right<=cut_left:continue
        x=np.r_[x[:cut_left],x[cut_right:]]
        removed+=(cut_right-cut_left)/rate
    # Leading/trailing silence only, never clip syllable onset.
    frame=max(1,int(rate*.01))
    frames=len(x)//frame
    lv=np.sqrt(np.mean(x[:frames*frame].reshape(-1,frame)**2,axis=1))
    active=np.where(lv>=threshold)[0]
    if len(active):
        start=max(0,(int(active[0])-8)*frame)
        stop=min(len(x),(int(active[-1])+9)*frame)
        x=x[start:stop]
    # Moderate loudness normalization with peak protection; no hard clipping.
    rms=float(np.sqrt(np.mean(x*x)))
    peak=float(np.max(np.abs(x)))
    gain=min(3.5,.89/max(peak,.01),.105/max(rms,.01))
    if gain>0:x=x*gain
    # A single short syllable should not feel abruptly cut off in the UI.
    # Add a tiny silence guard, without stretching its formants or pitch.
    if len(x)/rate<.42:
        pad=max(0,int((.46-len(x)/rate)*rate/2))
        x=np.pad(x,(pad,pad))
    return x,round(removed,3),round(gain,2)

manifest=[]
mismatches=[]
for index,(key,chinese,expected) in enumerate(tasks,1):
    path=assets/(key+".wav")
    original=backups/(key+".wav")
    produced=engine.to_phoneme(chinese)
    effective=produced
    corrected=False
    if expected and reading(produced)!=expected:
        effective,corrected=manual_phoneme(produced,expected)
        mismatches.append(dict(id=key,generated=reading(produced),expected=expected))
    if not original.exists():
        if path.exists() and path.stat().st_size>4000:
            shutil.copyfile(path,original)
        else:
            tempfile=raw/(key+".wav")
            before=engine._backend.to_phoneme
            try:
                if corrected:
                    engine._backend.to_phoneme=lambda sentence: effective
                engine.synthesize(chinese,str(tempfile),quality="duration_filter",max_attempts=2)
            finally:
                engine._backend.to_phoneme=before
            shutil.copyfile(tempfile,original)
            tempfile.unlink(missing_ok=True)
    audio,rate=sf.read(original,always_2d=True,dtype="float32")
    mono=audio.mean(axis=1)
    if rate==48000:mono=mono[::2];rate=24000
    if rate!=24000:raise ValueError((key,rate))
    cleaned,removed,gain=repair(mono,rate)
    if not (.20<len(cleaned)/rate<12):
        raise ValueError((key,len(cleaned)/rate))
    sf.write(path,cleaned,rate,subtype="PCM_16")
    manifest.append(dict(id=key,text=chinese,jyutping=expected,
        engine_g2p=reading(produced),
        effective_jyutping=reading(effective),
        tone_correction=corrected,
        duration_seconds=round(len(cleaned)/rate,3),
        original_seconds=round(len(mono)/rate,3),
        removed_pause_seconds=removed,
        normalized_gain=gain,
        bytes=path.stat().st_size))
    if index%5==0 or index==1:
        print(f"CANTONESE_AUDIO {index}/{len(tasks)} last={key} pause_removed={removed}s",flush=True)
        (root/"build/diagnostics/canto_progress.json").write_text(
            json.dumps(dict(done=index,total=len(tasks),last=key,
                            changed_readings=len(mismatches)),ensure_ascii=False),encoding="utf8")
manifest_path=assets/"manifest.json"
manifest_path.write_text(json.dumps(dict(
    producer="canto-tts / typangaa/canto-tts-nano",
    upstream="https://github.com/typangaa/canto-tts",
    upstream_license="Apache-2.0",
    label="Offline AI Cantonese - internally timed/normalized; human proofreading pending",
    sample_rate=24000,channels=1,items=manifest),ensure_ascii=False,indent=2)+"\n",encoding="utf8")
(root/"build/diagnostics/canto_g2p_differences.json").write_text(
    json.dumps(mismatches,ensure_ascii=False,indent=2),encoding="utf8")
print("AUDIO_PACK_READY",len(manifest),"CORRECTED_PRONUNCIATIONS",len(mismatches),
      "REMOVED_SILENCE_SECONDS",round(sum(i["removed_pause_seconds"] for i in manifest),2),
      "BYTES",sum(i["bytes"] for i in manifest),flush=True)
