package com.game.knowledge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun QuizResultScreen(
    total:Int,score:Int,mistakes:List<KnowledgeQuestion>,
    onAgain:()->Unit,onHome:()->Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val tabletLandscape=ResponsiveWindowPolicy.tabletLandscape(maxWidth.value,maxHeight.value)
        if(tabletLandscape){
            Row(Modifier.fillMaxSize().padding(22.dp),
                horizontalArrangement=Arrangement.spacedBy(18.dp)){
                Column(Modifier.weight(.8f).fillMaxHeight(),
                    verticalArrangement=Arrangement.SpaceEvenly,
                    horizontalAlignment=Alignment.CenterHorizontally){
                    Text("🏆 闯关完成！",color=Ink,
                        fontWeight=FontWeight.Black,fontSize=31.sp)
                    StoryCard(Modifier.fillMaxWidth(),Color(0xFFFFF2D8)){
                        Text("答对 $score/$total 道",fontSize=28.sp,
                            fontWeight=FontWeight.Black,color=Ink)
                        Text("正确率 ${score*100/total}% · 错题 ${mistakes.size} 道",
                            fontSize=17.sp,color=Ink)
                    }
                    LargeAction("再挑战一轮",onAgain,Modifier.fillMaxWidth(),Coral)
                    KidPill("🏠 返回首页",onHome,Modifier.fillMaxWidth())
                }
                StoryCard(Modifier.weight(1.2f).fillMaxHeight()) {
                    Text(if(mistakes.isEmpty()) "🌟 全部答对，太棒啦！"
                        else "📌 本轮错题与知识解析",
                        color=Ink,fontSize=23.sp,fontWeight=FontWeight.Black)
                    Column(Modifier.fillMaxWidth().weight(1f)
                        .verticalScroll(rememberScrollState()),
                        verticalArrangement=Arrangement.spacedBy(13.dp)){
                        if(mistakes.isEmpty())
                            Text("每轮都会优先抽取最近没做过的题，继续探索新知识吧！",
                                fontSize=17.sp,color=Sea)
                        mistakes.forEach {q->
                            Text("• ${q.question}",fontSize=17.sp,
                                fontWeight=FontWeight.Black,color=Ink)
                            Text("正确答案：${q.answer}",fontSize=16.sp,
                                fontWeight=FontWeight.Bold,color=Sea)
                            Text("💡 ${q.explanation}",fontSize=15.sp,
                                lineHeight=23.sp,color=Ink)
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(20.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){
                Text("🏆 闯关完成！",fontSize=29.sp,fontWeight=FontWeight.Black,color=Ink)
                StoryCard(Modifier.fillMaxWidth(),Color(0xFFFFF2D8)){
                    Text("答对 $score/$total 道",fontWeight=FontWeight.Black,
                        fontSize=26.sp,color=Ink)
                    Text("正确率 ${score*100/total}% · 错题 ${mistakes.size} 道",
                        fontSize=16.sp,color=Ink)
                }
                if(mistakes.isNotEmpty()) StoryCard(Modifier.fillMaxWidth()){
                    Text("📌 本轮错题回顾",fontSize=20.sp,fontWeight=FontWeight.Black)
                    mistakes.forEach {q->
                        Text("• ${q.question}",fontSize=15.sp,
                            fontWeight=FontWeight.Bold,color=Ink)
                        Text("答案：${q.answer}",fontSize=15.sp,color=Sea)
                        Text("💡 解析：${q.explanation}",fontSize=13.sp,
                            color=Ink.copy(alpha=.84f),lineHeight=19.sp)
                    }
                }
                LargeAction("再挑战一轮",onAgain,Modifier.fillMaxWidth(),Coral)
                KidLink("返回首页",onHome)
            }
        }
    }
}

@Composable
internal fun DuelResultScreen(
    winner:Int,vsAi:Boolean,speed:Boolean,leftScore:Int,rightScore:Int,
    leftTime:Long,rightTime:Long,onAgain:()->Unit,onHome:()->Unit
) {
    val result=when(winner){
        -1 -> "🤝 不分高下，平局！"
        0 -> "🎉 蓝方探险家获胜！"
        else -> if(vsAi) "🤖 AI 获胜，下次再来！" else "🎉 橙方探险家获胜！"
    }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val tabletLandscape=ResponsiveWindowPolicy.tabletLandscape(maxWidth.value,maxHeight.value)
        if(tabletLandscape) {
            Row(Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(.9f).fillMaxHeight(),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.SpaceEvenly) {
                    Text("🏆 知识对决结束",fontSize=30.sp,
                        fontWeight=FontWeight.Black,color=Ink)
                    LargeAction("再战一局",onAgain,Modifier.fillMaxWidth(),Coral)
                    KidPill("🏠 返回首页",onHome,Modifier.fillMaxWidth())
                }
                StoryCard(Modifier.weight(1.1f).fillMaxHeight(),Color(0xFFFFF0D5)){
                    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.SpaceEvenly,
                        horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(result,fontSize=25.sp,fontWeight=FontWeight.Black,
                            color=Ink,textAlign=TextAlign.Center)
                        Text("🔵 蓝方：$leftScore 分 · ${leftTime/1000.0} 秒",
                            fontSize=20.sp,color=Ink)
                        Text("${if(vsAi) "🤖 AI" else "🟠 橙方"}：$rightScore 分 · ${rightTime/1000.0} 秒",
                            fontSize=20.sp,color=Ink)
                        Text(if(speed) "先答对的孩子赢得这一题"
                            else "正确题数优先；平分时比较总用时",
                            fontSize=16.sp,color=Ink,textAlign=TextAlign.Center)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(15.dp)){
                Text("🏆 知识对决结束",fontWeight=FontWeight.Black,
                    fontSize=30.sp,color=Ink)
                StoryCard(Modifier.fillMaxWidth(),Color(0xFFFFF0D5)){
                    Text(result,fontWeight=FontWeight.Black,fontSize=23.sp,color=Ink)
                    Text("🔵 蓝方：$leftScore 分 · ${leftTime/1000.0} 秒",
                        fontSize=17.sp,color=Ink)
                    Text("${if(vsAi) "🤖 AI" else "🟠 橙方"}：$rightScore 分 · ${rightTime/1000.0} 秒",
                        fontSize=17.sp,color=Ink)
                    Text(if(speed) "先答对的孩子赢得这一题"
                        else "正确题数优先；平分时比较总用时",
                        fontSize=14.sp,color=Ink)
                }
                LargeAction("再战一局",onAgain,Modifier.fillMaxWidth(),Coral)
                KidLink("返回首页",onHome)
            }
        }
    }
}
