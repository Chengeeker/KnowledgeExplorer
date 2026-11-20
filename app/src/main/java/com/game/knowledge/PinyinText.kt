package com.game.knowledge

import android.icu.text.Transliterator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import kotlin.math.max

/** Global opt-in ruby annotation, inherited by all screens and dialogs. */
val LocalShowPinyin = compositionLocalOf { false }

/** Pure guard; keep Latin, numbers, symbols and emoji unchanged. */
internal object PinyinPolicy {
    fun isHan(c:Char):Boolean =
        c.code in 0x3400..0x9FFF || c.code in 0xF900..0xFAFF

    fun hasHan(text:String):Boolean = text.any(::isHan)

    // Ruby needs a shared baseline for Chinese, numbers and Latin letters.
    // Keep each visible Unicode code point in its own inline cell.
    internal data class Glyph(val offset:Int,val value:String)
    fun glyphs(text:String):List<Glyph> {
        val result=ArrayList<Glyph>(text.length)
        var index=0
        while(index<text.length) {
            val begin=index
            var code=Character.codePointAt(text,index)
            index+=Character.charCount(code)
            // Emoji variation selectors, skin tones and joined glyphs stay together.
            while(index<text.length) {
                val next=Character.codePointAt(text,index)
                if(next in 0xFE00..0xFE0F || next in 0x1F3FB..0x1F3FF || next==0x200D) {
                    index+=Character.charCount(next)
                    if(next==0x200D && index<text.length) {
                        code=Character.codePointAt(text,index)
                        index+=Character.charCount(code)
                    }
                } else break
            }
            result.add(Glyph(begin,text.substring(begin,index)))
        }
        return result
    }
    fun isVisibleGlyph(value:String):Boolean = value.isNotBlank()

    /** A short dictionary of common context-sensitive polyphonic readings. */
    val phraseOverrides:Map<String,List<String>> = mapOf(
        "音乐" to listOf("yīn","yuè"),
        "快乐" to listOf("kuài","lè"),
        "长大" to listOf("zhǎng","dà"),
        "长高" to listOf("zhǎng","gāo"),
        "长城" to listOf("cháng","chéng"),
        "银行" to listOf("yín","háng"),
        "行长" to listOf("háng","zhǎng"),
        "重庆" to listOf("chóng","qìng"),
        "重复" to listOf("chóng","fù"),
        "重新" to listOf("chóng","xīn"),
        "重量" to listOf("zhòng","liàng"),
        "重要" to listOf("zhòng","yào"),
        "重阳" to listOf("chóng","yáng"),
        "睡觉" to listOf("shuì","jiào"),
        "觉得" to listOf("jué","de"),
        "感觉" to listOf("gǎn","jué"),
        "朝阳" to listOf("zhāo","yáng"),
        "朝代" to listOf("cháo","dài"),
        "朝着" to listOf("cháo","zhe"),
        "朝辞" to listOf("zhāo","cí"),
        "数数" to listOf("shǔ","shù"),
        "数字" to listOf("shù","zì"),
        "答案" to listOf("dá","àn"),
        "答题" to listOf("dá","tí"),
        "好学" to listOf("hào","xué"),
        "爱好" to listOf("ài","hào"),
        "方便" to listOf("fāng","biàn"),
        "便宜" to listOf("pián","yi"),
        "发现" to listOf("fā","xiàn"),
        "头发" to listOf("tóu","fa"),
        "行为" to listOf("xíng","wéi"),
        "行动" to listOf("xíng","dòng"),
        "银行" to listOf("yín","háng"),
        "地方" to listOf("dì","fang"),
        "地上" to listOf("dì","shang")
    )
}

/**
 * Android ICU Han-Latin adds tone marks and, for many phrases, uses contextual
 * readings. Work on entire Han runs instead of transliterating isolated glyphs.
 * A few high-frequency polyphonic phrases are corrected explicitly above.
 *
 * Using Android's built-in ICU keeps the game fully offline: no network or
 * runtime library download, and no new Gradle dependency.
 */
internal object PinyinRomanizer {
    private val transformer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { Transliterator.getInstance("Han-Latin") }.getOrNull()
    }
    private val cache=object: LinkedHashMap<String,List<String?>>(96,0.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,List<String?>>) =
            size>120
    }
    @Synchronized
    fun readings(text:String):List<String?> {
        cache[text]?.let {return it}
        val values=MutableList<String?>(text.length) {null}
        var start=0
        while(start<text.length) {
            if(!PinyinPolicy.isHan(text[start])) {start++;continue}
            var end=start+1
            while(end<text.length && PinyinPolicy.isHan(text[end])) end++
            val piece=text.substring(start,end)
            val phrase=runCatching {transformer?.transliterate(piece)}.getOrNull().orEmpty()
            val tokens=phrase.trim().split(Regex("\\s+")).filter(String::isNotBlank)
            if(tokens.size==piece.length) {
                for(k in piece.indices) values[start+k]=tokens[k].lowercase()
            } else {
                for(k in piece.indices) {
                    val single=runCatching {
                        transformer?.transliterate(piece[k].toString())
                    }.getOrNull()?.trim()
                    if(!single.isNullOrBlank()) values[start+k]=single.lowercase()
                }
            }
            start=end
        }
        // Longest keys first: context reading overrides win over a lone glyph.
        for((phrase,reading) in PinyinPolicy.phraseOverrides.entries.sortedByDescending {it.key.length}) {
            var index=text.indexOf(phrase)
            while(index>=0) {
                reading.forEachIndexed { offset,syllable -> values[index+offset]=syllable }
                index=text.indexOf(phrase,index+phrase.length)
            }
        }
        cache[text]=values
        return values
    }
}

/**
 * One shared text entry point. When off, render the EXACT original Material 3
 * Text layout. When on, each Chinese character has its own tonal-pinyin ruby,
 * with inline placeholders so lines still wrap in normal Compose text layout.
 */
@Composable
fun PinyinText(
    text:String,
    modifier:Modifier=Modifier,
    color:Color=Color.Unspecified,
    fontSize:TextUnit=TextUnit.Unspecified,
    fontStyle:FontStyle?=null,
    fontWeight:FontWeight?=null,
    fontFamily:FontFamily?=null,
    letterSpacing:TextUnit=TextUnit.Unspecified,
    textDecoration:TextDecoration?=null,
    textAlign:TextAlign?=null,
    lineHeight:TextUnit=TextUnit.Unspecified,
    overflow:TextOverflow=TextOverflow.Clip,
    softWrap:Boolean=true,
    maxLines:Int=Int.MAX_VALUE,
    minLines:Int=1,
    onTextLayout:(TextLayoutResult)->Unit={},
    style:TextStyle=LocalTextStyle.current
) {
    val show=LocalShowPinyin.current
    if(!show || !PinyinPolicy.hasHan(text)) {
        MaterialText(text=text,modifier=modifier,color=color,fontSize=fontSize,
            fontStyle=fontStyle,fontWeight=fontWeight,fontFamily=fontFamily,
            letterSpacing=letterSpacing,textDecoration=textDecoration,textAlign=textAlign,
            lineHeight=lineHeight,overflow=overflow,softWrap=softWrap,
            maxLines=maxLines,minLines=minLines,onTextLayout=onTextLayout,style=style)
        return
    }
    val readings=remember(text) { PinyinRomanizer.readings(text) }
    val base=(if(fontSize.type==TextUnitType.Sp) fontSize.value
        else if(style.fontSize.type==TextUnitType.Sp) style.fontSize.value
        else 16f).coerceAtLeast(10f)
    // Compact ruby typography: old 1.45-em glyph cells caused the home cards
    // to overflow and forced visually awkward wrapping at common phone widths.
    val rubySize=(base*.40f).coerceIn(6f,10f)
    val boxHeight=(base*1.68f).sp
    val rubyHeight=(base*.52f).sp
    val glyphHeight=(base*1.06f).sp
    // Every visible character uses an equal-height inline cell. Previously
    // digits / Latin remained in the outer Text while Han sat in ruby cells;
    // this caused numerals to visually sink below the Chinese glyph baseline.
    val glyphs=remember(text) {PinyinPolicy.glyphs(text)}
    val segments=buildAnnotatedString {
        glyphs.forEach { cell ->
            if(PinyinPolicy.isVisibleGlyph(cell.value)) {
                appendInlineContent("cell-${cell.offset}",cell.value)
            } else append(cell.value)
        }
    }
    val inline=buildMap<String,InlineTextContent> {
        glyphs.forEach { cell ->
            val glyph=cell.value
            if(!PinyinPolicy.isVisibleGlyph(glyph)) return@forEach
            val han=glyph.length==1 && PinyinPolicy.isHan(glyph[0])
            val reading=if(han) readings[cell.offset] else null
            val ascii=glyph.length==1 && glyph[0].code in 33..126
            val width=when {
                !reading.isNullOrBlank() ->
                    max(base*1.09f,reading.length*rubySize*.50f+1f)
                ascii && glyph[0].isDigit() -> base*.65f
                ascii && glyph[0].isLetter() -> base*.69f
                ascii -> base*.53f
                glyph.length>1 -> base*1.28f
                glyph[0].code in 0x3000..0x303F ||
                    glyph[0].code in 0xFF01..0xFF60 -> base*1.03f
                else -> base*1.05f
            }
            put("cell-${cell.offset}",InlineTextContent(
                Placeholder(width=width.sp,height=boxHeight,
                    placeholderVerticalAlign=PlaceholderVerticalAlign.AboveBaseline)
            ) {
                Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.Bottom) {
                    if(!reading.isNullOrBlank()) {
                        MaterialText(reading,fontSize=rubySize.sp,lineHeight=rubyHeight,
                            color=if(color==Color.Unspecified) Color(0xFF47616A) else color.copy(alpha=.85f),
                            fontWeight=FontWeight.Medium,maxLines=1,softWrap=false,
                            textAlign=TextAlign.Center)
                    }
                    MaterialText(glyph,fontSize=base.sp,lineHeight=glyphHeight,
                        fontWeight=fontWeight,color=color,maxLines=1,softWrap=false,
                        textAlign=TextAlign.Center)
                }
            })
        }
    }
    MaterialText(text=segments,modifier=modifier,color=color,fontSize=fontSize,
        fontStyle=fontStyle,fontWeight=fontWeight,fontFamily=fontFamily,
        letterSpacing=letterSpacing,textDecoration=textDecoration,textAlign=textAlign,
        lineHeight=max(base*1.78f,
            if(lineHeight.type==TextUnitType.Sp) lineHeight.value else 0f).sp,
        overflow=overflow,softWrap=softWrap,maxLines=maxLines,
        minLines=minLines,onTextLayout=onTextLayout,style=style,
        inlineContent=inline)
}
