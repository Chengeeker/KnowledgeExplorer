package com.game.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/** Three learning interactions extending the existing image/voice mini-games. */
internal enum class CantoneseChallengeMode { BRIDGE, TREASURE, DIALOGUE }

internal object CantoneseChallengeRules {
    fun eligible(mode:CantoneseChallengeMode,all:List<CantoneseWord>):List<CantoneseWord> =
        when(mode) {
            CantoneseChallengeMode.DIALOGUE ->
                all.filter {it.kind=="phrase"}
            CantoneseChallengeMode.TREASURE ->
                all.filter {it.kind=="word" && it.category in setOf(
                    "交通出行","校园日常","点心茶楼","温暖的家","认识身体","岭南文化")}
            CantoneseChallengeMode.BRIDGE ->
                all.filter {it.kind=="word" && it.meaning!=it.hanzi &&
                    it.category!="家人称呼"}
        }

    fun choices(target:CantoneseWord,entries:List<CantoneseWord>,
                random:Random=Random.Default):List<CantoneseWord> {
        val same=entries.filter {it.id!=target.id && it.category==target.category}
        val others=entries.filter {it.id!=target.id && it.category!=target.category}
        require(same.size+others.size>=3)
        val chosen=(listOf(target)+same.shuffled(random).take(3)
            +others.shuffled(random).take((3-same.size).coerceAtLeast(0))).take(4)
        return chosen.shuffled(random)
    }
}

@Composable
internal fun CantoneseChallengeScreen(
    mode:CantoneseChallengeMode,
    words:List<CantoneseWord>,
    showJyutping:Boolean,
    traditional:Boolean,
    slow:Boolean,
    voice:CantoneseSpeaker,
    onRecognized:(CantoneseWord)->Unit,
    onCompleted:()->Unit,
    tabletLandscape:Boolean=false
) {
    val feedback=LocalKidFeedback.current
    var batch by remember(mode){mutableIntStateOf(0)}
    val eligible=remember(words,mode){CantoneseChallengeRules.eligible(mode,words)}
    val round=remember(eligible,batch) {
        eligible.shuffled(Random(System.nanoTime())).take(5)
    }
    if(round.size<4) {
        Text("主题正在补充中，请先体验词语宝典。",color=Ink,fontSize=16.sp)
        return
    }
    var index by remember(mode,batch){mutableIntStateOf(0)}
    var correct by remember(mode,batch){mutableIntStateOf(0)}
    var selected by remember(mode,batch,index){mutableStateOf<String?>(null)}
    var mistakes by remember(mode,batch,index){mutableIntStateOf(0)}
    val complete=index>=round.size
    val current=round.getOrNull(index)
    LaunchedEffect(current?.id,mode,batch) {
        if(current!=null && mode==CantoneseChallengeMode.TREASURE) voice.speak(current,slow)
    }
    val title=when(mode) {
        CantoneseChallengeMode.BRIDGE->"🧩 粤普变变变"
        CantoneseChallengeMode.TREASURE->"🗺️ 生活寻宝"
        CantoneseChallengeMode.DIALOGUE->"💬 对话小剧场"
    }
    val tint=when(mode) {
        CantoneseChallengeMode.BRIDGE->Color(0xFFE4F6EC)
        CantoneseChallengeMode.TREASURE->Color(0xFFFFF1D8)
        CantoneseChallengeMode.DIALOGUE->Color(0xFFEEE8FF)
    }
    StoryCard(if(tabletLandscape) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),tint) {
        Column(Modifier.fillMaxWidth().then(if(tabletLandscape)
            Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,color=Ink,fontWeight=FontWeight.Black,fontSize=23.sp)
        if(complete) {
            Text("🌟 你完成了这一轮！",color=Ink,fontWeight=FontWeight.Black,
                fontSize=22.sp)
            Text("第一次就答对了 $correct / 5 题。新词听多了就会记住。",
                color=Ink,fontSize=16.sp)

        } else if(current!=null) {
            Text("第 ${index+1}/${round.size} 题",color=Sea,
                fontWeight=FontWeight.Bold)
            val instruction=when(mode) {
                CantoneseChallengeMode.BRIDGE ->
                    "普通话说「${current.meaning}」，粤语应该选哪张卡？"
                CantoneseChallengeMode.TREASURE ->
                    "听一听并找到「${current.meaning}」这件东西！"
                CantoneseChallengeMode.DIALOGUE ->
                    "遇到这个情况：${current.meaning}。该怎样用粤语回答？"
            }
            Text(instruction,color=Ink,fontSize=17.sp,
                fontWeight=FontWeight.Bold)
            if(voice.hasAudio(current)) {
                KidPill("🔊 播放粤语提示",{
                    feedback?.tap()
                    voice.speak(current,slow)
                },Modifier.fillMaxWidth())
            }
            val candidates=remember(current.id,eligible,batch) {
                CantoneseChallengeRules.choices(current,eligible)
            }
            candidates.chunked(2).forEach {pair->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    pair.forEach {option->
                        val won=selected==current.id
                        val chosen=selected==option.id
                        val shade=when {
                            won && option.id==current.id -> Color(0xFFB9E8BD)
                            chosen -> Color(0xFFFFDDD0)
                            else -> Color.White
                        }
                        Column(Modifier.weight(1f).heightIn(min=103.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(shade)
                            .clickable(enabled=!won) {
                                feedback?.tap()
                                voice.stop()
                                if(option.id==current.id) {
                                    if(selected==null && mistakes==0) correct++
                                    selected=current.id
                                    onRecognized(current)
                                    feedback?.success()
                                    voice.speak(current,slow)
                                } else {
                                    selected=option.id
                                    mistakes++
                                    feedback?.error()
                                }
                            }.padding(11.dp),
                            horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text(option.emoji,fontSize=33.sp,textAlign=TextAlign.Center)
                            Text(if(traditional)option.traditional else option.hanzi,
                                color=Ink,fontSize=17.sp,fontWeight=FontWeight.Black,
                                textAlign=TextAlign.Center)
                            if(showJyutping) Text(option.jyutping,color=Sea,
                                fontSize=11.sp,textAlign=TextAlign.Center)
                        }
                    }
                }
            }
            if(selected==current.id) {
                Text("✅ 答对啦！${current.meaning}，下次可以在生活中试着说。",
                    color=Ink,fontSize=14.sp)

            } else {
                Text(if(mistakes>0) "差一点，再试一次就好！" else
                    "先听再猜，不会也没关系。",color=Ink,fontSize=13.sp)
            }
        }
        } // questions, choices, feedback may scroll while action stays visible
        if(complete) {
            LargeAction("🔁 换一组再玩",{
                voice.stop()
                index=0
                correct=0
                batch++
                feedback?.tap()
            },Modifier.fillMaxWidth())
        } else if(current!=null && selected==current.id) {
                LargeAction(if(index==round.lastIndex)"🏅 完成闯关" else "➡ 下一道",{
                    voice.stop()
                    if(index==round.lastIndex) onCompleted()
                    index++
                    feedback?.tap()
                },Modifier.fillMaxWidth())
        }
    }
}