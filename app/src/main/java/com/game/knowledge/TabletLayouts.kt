package com.game.knowledge

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tablet layouts: the card spans the available screen height.
 * Its information area scrolls independently; core actions stay reachable.
 */
@Composable
internal fun TabletStudyBody(
    q:KnowledgeQuestion,reveal:Boolean,narrator:MandarinNarrator,
    startedAt:Long,waitSeconds:Long,
    onReveal:()->Unit,onNext:()->Unit,onPrevious:()->Unit,hasPrevious:Boolean,
    modifier:Modifier
) {
    StoryCard(modifier,Color.White) {
        Column(Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Text("🌟 ${q.grade}年级 · ${q.domain} · ${q.topic}",
                color=Sea,fontWeight=FontWeight.ExtraBold,fontSize=15.sp)
            Text(q.question,color=Ink,fontWeight=FontWeight.Black,
                fontSize=28.sp,lineHeight=38.sp)
            if(reveal){
                HorizontalDivider(color=Color(0xFFD2E8DB))
                Text("💡 原来是这样！",color=Sea,
                    fontSize=19.sp,fontWeight=FontWeight.Black)
                Text(q.answer,color=Ink,fontSize=30.sp,
                    fontWeight=FontWeight.Black)
                Text(q.explanation,color=Ink,fontSize=17.sp,lineHeight=26.sp)
            } else {
                Text(
                    if(waitSeconds>0) "🧠 观察插画，先猜一猜。还有 ${waitSeconds} 秒就能揭晓。"
                    else "✨ 现在可以揭晓答案啦！",
                    color=Ink,fontSize=17.sp,lineHeight=25.sp)
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween) {
            KidLink(if(reveal) "🔊 听答案和解释" else "🔊 朗读题目",{
                if(reveal) narrator.say(q.answer+"。"+q.explanation)
                else narrator.say(q.question)
            },enabled=narrator.ready)
            if(hasPrevious) KidLink("← 上一张",onPrevious)
        }
        if(reveal)
            LargeAction("继续探索 →",onNext,Modifier.fillMaxWidth(),Coral)
        else
            LargeAction(
                if(waitSeconds>0) "🔒 请先思考 ${waitSeconds} 秒" else "✨ 揭晓答案",
                {
                    if(ExplanationGate.canReveal(startedAt,SystemClock.elapsedRealtime(),
                            ExplanationGate.STUDY_WAIT_MS)){
                        narrator.say(q.answer+"。"+q.explanation)
                        onReveal()
                    }
                },Modifier.fillMaxWidth(),Sea,enabled=waitSeconds==0L)
    }
}

@Composable
internal fun TabletChallengeBody(
    q:KnowledgeQuestion,options:List<String>,choice:String?,
    narrator:MandarinNarrator,startedAt:Long,waitSeconds:Long,
    onChoose:(String)->Unit,onNext:()->Unit,modifier:Modifier
) {
    StoryCard(modifier,Color.White) {
        Text("🎯 ${q.grade}年级 · ${q.domain} · ${q.topic}",
            color=Sea,fontSize=15.sp,fontWeight=FontWeight.ExtraBold)
        Column(Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Text(q.question,color=Ink,fontWeight=FontWeight.Black,
                fontSize=26.sp,lineHeight=35.sp)
            if(choice!=null) {
                HorizontalDivider(color=Color(0xFFD2E8DB))
                Text(if(choice==q.answer) "✅ 回答正确！" else "🌱 又学到新知识了！",
                    color=Sea,fontSize=20.sp,fontWeight=FontWeight.Black)
                Text("正确答案：${q.answer}",fontSize=18.sp,
                    color=Ink,fontWeight=FontWeight.Black)
                Text("💡 ${q.explanation}",color=Ink,fontSize=16.sp,lineHeight=23.sp)
            } else {
                Text("请选择正确答案；答题不需要等待倒计时。",
                    fontSize=14.sp,color=Ink.copy(alpha=.75f))
            }
        }
        // Answer choices occupy the lower portion instead of being crushed
        // against the top of a scrollable landscape page.
        options.chunked(2).forEach {pair ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(11.dp)){
                pair.forEach {option->
                    ChoiceButton(option,if(choice==null) null else when(option) {
                        q.answer->true
                        choice->false
                        else->null
                    },choice==null,{onChoose(option)},Modifier.weight(1f))
                }
            }
        }
        if(choice!=null) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                KidLink("🔊 朗读答案和解析",{
                    narrator.say(q.answer+"。"+q.explanation)
                },enabled=narrator.ready)
            }
            LargeAction("下一题 →",onNext,Modifier.fillMaxWidth(),Coral)
        } else {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween) {
                KidLink("🔊 朗读题目",{narrator.say(q.question)},enabled=narrator.ready)
                KidLink(if(waitSeconds>0) "🔒 ${waitSeconds} 秒后可跳过"
                    else "💡 看答案",{
                    if(ExplanationGate.canReveal(startedAt,SystemClock.elapsedRealtime(),
                            ExplanationGate.QUIZ_WAIT_MS)) onChoose("__skip__")
                },enabled=waitSeconds==0L)
            }
        }
    }
}
