#!/usr/bin/env python3
"""Create original, locally rendered flat-cartoon vector illustrations per encyclopedia subject."""
from pathlib import Path
from xml.sax.saxutils import escape
out=Path(__file__).resolve().parent.parent/"app/src/main/res/drawable"
out.mkdir(parents=True,exist_ok=True)
D={
"science": ("#A4E7CF",[
("#FFF9E4","M40,20 L68,20 L68,29 L63,29 L63,48 L83,78 Q87,90 76,92 L32,92 Q21,90 25,78 L45,48 L45,29 L40,29 Z"),
("#138F95","M36,72 Q54,64 75,72 L80,82 Q81,86 75,86 L33,86 Q27,86 31,81 Z"),
("#FFFFFF","M39,24 L69,24 L69,29 L39,29 Z"),
("#FFBE61","M30,15 A6,6 0,1 1,30.1 15 Z"),
("#FFFFFF","M60,52 A4,4 0,1 1,60.1 52 Z")]),
"geography":("#B7DCF4",[
("#138F95","M54,15 A39,39 0,1 1,53.99 15 Z"),
("#A0E3BB","M25,36 L40,25 L48,30 L48,40 L42,46 L43,58 L34,68 L26,57 L21,49 Z"),
("#A0E3BB","M64,23 L78,35 L73,45 L84,49 L80,62 L69,63 L65,78 L57,68 L59,54 L53,48 Z"),
("#F9FAEE","M52,12 L56,12 L56,96 L52,96 Z")]),
"life":("#F7DEA5",[
("#FFFFFF","M26,43 L54,20 L82,43 L82,85 Q82,89 77,89 L31,89 Q26,89 26,84 Z"),
("#F0936B","M19,44 L54,14 L89,44 L82,50 L54,27 L26,50 Z"),
("#77BDD3","M37,51 L49,51 L49,67 L37,67 Z"),
("#138F95","M58,58 L71,58 L71,89 L58,89 Z"),
("#FAD06A","M66,73 A2,2 0,1 1,66.1 73 Z")]),
"safety":("#CAEFDA",[
("#138F95","M54,11 L87,25 L84,65 Q78,84 54,95 Q30,84 24,65 L21,25 Z"),
("#FFFFFF","M54,21 L78,31 L75,64 Q70,76 54,84 Q38,76 33,64 L30,31 Z"),
("#FFAE78","M48,40 L60,40 L60,50 L70,50 L70,61 L60,61 L60,72 L48,72 L48,61 L38,61 L38,50 L48,50 Z")]),
"math":("#C2D6FC",[
("#FFBC65","M31,22 L54,12 L77,22 L86,49 L75,80 L54,95 L32,82 L21,49 Z"),
("#FFFFFF","M41,35 A5,5 0,1 1,41.1 35 Z"),
("#FFFFFF","M69,35 A5,5 0,1 1,69.1 35 Z"),
("#FFFFFF","M38,63 L70,63 L70,72 L38,72 Z"),
("#138F95","M51,55 L57,55 L57,80 L51,80 Z")]),
"culture":("#FFE0AD",[
("#F0936B","M52,13 L57,13 L57,26 L52,26 Z"),
("#D8594A","M35,28 Q54,19 73,28 Q85,53 73,76 Q54,85 35,76 Q23,53 35,28 Z"),
("#FFB96D","M47,28 L52,28 L52,78 L47,78 Z"),
("#FFB96D","M61,28 L66,28 L66,78 L61,78 Z"),
("#9F3D43","M30,30 Q54,22 78,30 L78,36 Q54,30 30,36 Z"),
("#9F3D43","M34,70 Q54,77 74,70 L74,79 Q54,85 34,79 Z"),
("#D8594A","M52,79 L58,79 L58,96 L52,96 Z")]),
"art":("#FFD4BC",[
("#FFEABB","M52,15 C19,13 9,45 21,73 C32,95 72,93 80,76 Q84,68 72,64 Q59,59 74,47 Q86,36 70,23 Q60,15 52,15 Z"),
("#138F95","M35,40 A6,6 0,1 1,35.1 40 Z"),
("#FC9369","M52,29 A6,6 0,1 1,52.1 29 Z"),
("#A2D985","M68,41 A6,6 0,1 1,68.1 41 Z"),
("#9FA8EB","M30,60 A6,6 0,1 1,30.1 60 Z"),
("#FFFFFF","M56,70 A9,9 0,1 1,56.1 70 Z")]),
"literature":("#FFE1C8",[
("#138F95","M15,38 Q34,23 52,37 L52,88 Q32,76 15,87 Z"),
("#236B88","M55,37 Q73,23 92,38 L92,87 Q75,76 55,88 Z"),
("#FFFFFF","M23,42 Q38,35 50,43 L50,80 Q36,73 23,79 Z"),
("#FFF5D8","M58,43 Q75,35 85,42 L85,79 Q72,73 58,80 Z"),
("#F0936B","M36,43 L40,44 L40,72 L36,71 Z")]),
"eco":("#C7EFC8",[
("#8D6A4A","M51,57 L58,57 L58,94 L51,94 Z"),
("#18997A","M55,63 Q20,60 24,29 Q48,20 55,63 Z"),
("#68BA86","M55,63 Q89,54 86,27 Q63,20 55,63 Z"),
("#FFF5C3","M26,85 A8,8 0,1 1,26.1 85 Z")]),
"tech":("#BDD6FB",[
("#138F95","M30,25 Q30,20 36,20 L72,20 Q78,20 78,26 L78,68 L30,68 Z"),
("#FFF9DF","M37,32 L71,32 L71,60 L37,60 Z"),
("#214E57","M40,40 A4,4 0,1 1,40.1 40 Z"),
("#214E57","M64,40 A4,4 0,1 1,64.1 40 Z"),
("#FC9D6C","M43,53 Q54,61 65,53 L64,57 Q54,65 44,57 Z"),
("#F5B862","M16,71 L92,71 L84,86 L24,86 Z"),
("#F5B862","M50,9 L58,9 L58,20 L50,20 Z")]),
"history":("#F3E3B5",[
("#D68B62","M13,36 L54,15 L95,36 L93,43 L15,43 Z"),
("#FFF9E5","M23,44 L35,44 L35,79 L23,79 Z"),
("#FFF9E5","M48,44 L60,44 L60,79 L48,79 Z"),
("#FFF9E5","M73,44 L85,44 L85,79 L73,79 Z"),
("#B97456","M18,79 L90,79 L90,88 L18,88 Z"),
("#B97456","M11,88 L97,88 L97,96 L11,96 Z")]),
"law":("#D1E8D8",[
("#FFB967","M51,17 L58,17 L58,87 L51,87 Z"),
("#187A83","M24,35 L85,35 L85,41 L24,41 Z"),
("#187A83","M36,35 L40,35 L22,67 L18,67 Z"),
("#187A83","M70,35 L74,35 L91,67 L87,67 Z"),
("#F0936B","M12,67 L45,67 Q43,78 28,78 Q14,78 12,67 Z"),
("#F0936B","M63,67 L96,67 Q94,78 79,78 Q65,78 63,67 Z"),
("#187A83","M34,86 L74,86 L74,94 L34,94 Z")]),
}
header='''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
android:width="140dp" android:height="140dp"
android:viewportWidth="108" android:viewportHeight="108">'''
for name,(bg,paths) in D.items():
    body=[header,f'<path android:fillColor="{bg}" android:pathData="M54,2 A52,52 0,1 1,53.99 2 Z"/>']
    for fill,path in paths:
        body.append('<path android:fillColor="'+fill+'" android:pathData="'+escape(path)+'"/>')
    body.append('</vector>')
    (out/f"art_{name}.xml").write_text("\n".join(body),encoding="utf8")
print("Generated",len(D),"illustrated vectors in",out)
