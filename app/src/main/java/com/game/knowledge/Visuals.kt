package com.game.knowledge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF214E57)
val Sea = Color(0xFF138F95)
val Sky = Color(0xFF68B9D4)
val Coral = Color(0xFFFF996A)
val Yellow = Color(0xFFFFD478)
val Paper = Color(0xFFFFFCF5)
val PaleMint = Color(0xFFE7F8EE)
val SoftBlue = Color(0xFFE6F5FF)

@Composable
fun KnowledgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=lightColorScheme(
        primary=Sea, onPrimary=Color.White, secondary=Coral,
        background=Paper, surface=Paper, onBackground=Ink, onSurface=Ink
    ), content=content)
}

@Composable
fun PaintedBackground(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(Color(0xFFDBF8ED), Color(0xFFFFF8E8), Color(0xFFFFF9F1))
    )), content=content)
}

@Composable
fun StoryCard(modifier: Modifier = Modifier, tint: Color = Color.White,
              content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(25.dp))
        .background(tint).border(1.dp,Color(0xFFD5EADD),RoundedCornerShape(25.dp))
        .padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp), content=content)
}

@Composable
fun LargeAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                color: Color=Sea, enabled:Boolean=true) {
    val feedback = LocalKidFeedback.current
    val shape = RoundedCornerShape(19.dp)
    Box(modifier.heightIn(min=56.dp).clip(shape)
        .background(if(enabled) color else color.copy(alpha=.38f))
        .border(2.dp,Color.White.copy(alpha=.8f),shape)
        .clickable(enabled=enabled) { feedback?.tap();onClick() }
        .padding(horizontal=16.dp,vertical=11.dp),contentAlignment=Alignment.Center) {
        PinyinText(text,fontSize=18.sp,fontWeight=FontWeight.ExtraBold,
            color=Color.White,textAlign=TextAlign.Center)
    }
}

@Composable
fun KidPill(label:String,onClick:()->Unit,modifier:Modifier=Modifier,
            color:Color=Color.White,height:androidx.compose.ui.unit.Dp=42.dp,
            vibrateOnClick:Boolean=true) {
    val feedback=LocalKidFeedback.current
    val shape=RoundedCornerShape(17.dp)
    Box(modifier.heightIn(min=height).clip(shape).background(color)
        .border(1.5.dp,Color(0xFFD6E9DD),shape)
        .clickable {if(vibrateOnClick) feedback?.tap();onClick()}
        .padding(horizontal=13.dp,vertical=8.dp),contentAlignment=Alignment.Center) {
        PinyinText(label,color=Ink,fontWeight=FontWeight.ExtraBold,fontSize=14.sp,
            textAlign=TextAlign.Center)
    }
}

@Composable
fun KidLink(label:String,onClick:()->Unit,modifier:Modifier=Modifier,
            enabled:Boolean=true,vibrateOnClick:Boolean=true) {
    val feedback=LocalKidFeedback.current
    PinyinText(label,color=if(enabled) Sea else Ink.copy(alpha=.4f),
        fontWeight=FontWeight.ExtraBold,fontSize=14.sp,
        modifier=modifier.clip(RoundedCornerShape(13.dp))
            .clickable(enabled=enabled) {if(vibrateOnClick) feedback?.tap();onClick()}
            .padding(horizontal=11.dp,vertical=10.dp))
}

@Composable
fun TopLine(title:String,onBack:()->Unit, extra: String?=null,
            vibrateOnBack:Boolean=true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(9.dp)) {
        KidPill("← 返回",onBack,vibrateOnClick=vibrateOnBack)
        PinyinText(title,fontSize=22.sp,fontWeight=FontWeight.Black,
            maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
        if(extra!=null) PinyinText(extra,fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Sea)
    }
}

@Composable
fun SelectPill(text: String, active:Boolean,onClick:()->Unit) {
    val feedback=LocalKidFeedback.current
    val shape=RoundedCornerShape(16.dp)
    Box(Modifier.padding(vertical=3.dp).clip(shape)
        .background(if(active) Yellow else Color.White)
        .border(if(active) 2.dp else 1.dp,
            if(active) Color(0xFFFFBB5B) else Color(0xFFD4E5DD),shape)
        .clickable { feedback?.tap();onClick() }
        .padding(horizontal=12.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
        PinyinText(text,fontSize=14.sp,color=Ink,
            fontWeight=if(active) FontWeight.ExtraBold else FontWeight.Medium)
    }
}

@Composable
fun TitleLine(title:String,desc:String?=null) {
    PinyinText(title,fontWeight=FontWeight.Black,color=Ink,fontSize=17.sp)
    if(desc!=null) PinyinText(desc,color=Ink.copy(alpha=.69f),fontSize=13.sp)
}

/** Offline illustration: category-colored landscape + semantic subject art.
 * Stable on phones and tablets, no Internet or remote image hotlinks.
 */
@Composable
fun EncyclopediaIllustration(question: KnowledgeQuestion?, modifier:Modifier=Modifier,
                              iconOverride:String?=null) {
    val domain=question?.domain.orEmpty()
    val accent=when(domain) {
        "自然科学" -> Color(0xFFABE3C8)
        "地理" -> Color(0xFFC1E0FA)
        "逻辑与数学","信息科技" -> Color(0xFFBFD6FC)
        "历史文化","传统文化" -> Color(0xFFFFE1B2)
        "艺术体育","语文文学" -> Color(0xFFFFD9C6)
        "社会与法治","安全与健康" -> Color(0xFFCDECD8)
        else -> Color(0xFFD0EBF5)
    }
    val art=when(domain) {
        "自然科学" -> R.drawable.art_science
        "生活常识" -> R.drawable.art_life
        "安全与健康" -> R.drawable.art_safety
        "逻辑与数学" -> R.drawable.art_math
        "传统文化" -> R.drawable.art_culture
        "艺术体育" -> R.drawable.art_art
        "地理" -> R.drawable.art_geography
        "语文文学" -> R.drawable.art_literature
        "环境保护" -> R.drawable.art_eco
        "信息科技" -> R.drawable.art_tech
        "历史文化" -> R.drawable.art_history
        "社会与法治" -> R.drawable.art_law
        else -> R.drawable.art_science
    }
    BoxWithConstraints(modifier.clip(RoundedCornerShape(24.dp)).background(accent),
        contentAlignment=Alignment.Center) {
        val compact=maxHeight < 160.dp
        val iconSize=if(compact) 77.dp else if(maxHeight >= 260.dp) 205.dp else 132.dp
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width; val h=size.height
            drawCircle(Color.White.copy(alpha=.36f),radius=h*.40f,
                center=Offset(w*.72f,h*.34f))
            drawCircle(Color.White.copy(alpha=.28f),radius=h*.18f,
                center=Offset(w*.14f,h*.20f))
            drawRoundRect(Color.White.copy(alpha=.3f),
                topLeft=Offset(w*.08f,h*.80f),size=androidx.compose.ui.geometry.Size(w*.84f,h*.13f),
                cornerRadius=CornerRadius(25f,25f))
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.Center) {
            Image(painter=painterResource(id=art),contentDescription=domain,
                modifier=Modifier.size(iconSize))
            Row(verticalAlignment=Alignment.CenterVertically) {
                PinyinText(iconOverride ?: question?.let(::pictureFor) ?: "🔍",
                    fontSize=if(compact) 17.sp else 23.sp)
                Spacer(Modifier.width(5.dp))
                PinyinText(if(question==null) "出发！探索奇妙世界" else question.topic,
                    fontWeight=FontWeight.ExtraBold,
                    fontSize=if(compact) 12.sp else 14.sp,
                    color=Ink.copy(alpha=.86f),maxLines=1,
                    overflow=TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ChoiceButton(label:String,selected:Boolean?,enabled:Boolean,
                 onClick:()->Unit,modifier:Modifier=Modifier,compact:Boolean=false) {
    val fill=when(selected) {
        true -> Color(0xFFB0E9CA)
        false -> Color(0xFFFFD2C5)
        null -> Color.White
    }
    Box(modifier
        .heightIn(min=if(compact) 53.dp else 63.dp)
        .clip(RoundedCornerShape(if(compact) 14.dp else 18.dp))
        .background(fill).border(1.5.dp,Color(0xFFC9E3DA),RoundedCornerShape(if(compact) 14.dp else 18.dp))
        .clickable(enabled=enabled,onClick=onClick)
        .padding(horizontal=9.dp,vertical=9.dp),
        contentAlignment=Alignment.Center) {
        PinyinText(label,color=Ink,fontSize=if(compact) 15.sp else 18.sp,
            fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,
            maxLines=if(LocalShowPinyin.current) Int.MAX_VALUE else if(compact) 3 else 4,
            overflow=TextOverflow.Ellipsis,
            lineHeight=if(compact) 18.sp else 23.sp)
    }
}
