package com.game.knowledge

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

internal object DuelSeatLayout {
    // The two separate players sit on opposite short edges of the landscape tablet.
    fun rotationFor(side:Int): Float = if(side==0) 90f else -90f
}

/**
 * Match Math Planet's speed-race result feedback: only a correct answer wins
 * the race. Both physical seats must see the result from their own viewpoint.
 */
internal object DuelRaceNotice {
    const val SUCCESS_DISPLAY_MS = 1_650L
    fun forSide(speed:Boolean,winner:Int,side:Int):String {
        require(side==0 || side==1)
        if(!speed) return ""
        return when(winner) {
            -1 -> "⚡ 先答对者得分"
            side -> "✅ 你抢答成功！"
            else -> "⚡ 对方已抢答成功！"
        }
    }
}

private data class DuelFeedback(val selected:String,val correct:Boolean)

@Composable
fun DuelScreen(questions:List<KnowledgeQuestion>,vsAi:Boolean,speed:Boolean,
               aiLevel:Int,onBack:()->Unit,onAgain:()->Unit,onHome:()->Unit,
               onCompleted:()->Unit) {
    if(questions.isEmpty()) return
    val activity=LocalContext.current as? Activity
    val feedback=LocalKidFeedback.current
    DisposableEffect(activity,vsAi) {
        val previous=activity?.requestedOrientation
        if(!vsAi) activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {if(previous!=null) activity.requestedOrientation=previous}
    }
    var raceIndex by remember(questions) {mutableIntStateOf(0)}
    var leftIndex by remember(questions) {mutableIntStateOf(0)}
    var rightIndex by remember(questions) {mutableIntStateOf(0)}
    var raceWinner by remember(questions) {mutableIntStateOf(-1)}
    var done by remember(questions) {mutableStateOf(false)}
    var leftScore by remember(questions) {mutableIntStateOf(0)}
    var rightScore by remember(questions) {mutableIntStateOf(0)}
    var leftTime by remember(questions) {mutableLongStateOf(0L)}
    var rightTime by remember(questions) {mutableLongStateOf(0L)}
    var leftStarted by remember(questions) {mutableLongStateOf(SystemClock.elapsedRealtime())}
    var rightStarted by remember(questions) {mutableLongStateOf(SystemClock.elapsedRealtime())}
    var leftFeedback by remember(questions) {mutableStateOf<DuelFeedback?>(null)}
    var rightFeedback by remember(questions) {mutableStateOf<DuelFeedback?>(null)}
    var wrongLeft by remember(questions) {mutableStateOf(false)}
    var wrongRight by remember(questions) {mutableStateOf(false)}
    val finished=if(speed) done else leftIndex>=questions.size && rightIndex>=questions.size
    val leftQ=questions.getOrNull(if(speed) raceIndex else leftIndex)
    val rightQ=questions.getOrNull(if(speed) raceIndex else rightIndex)

    fun submit(side:Int,answer:String) {
        if(finished || (speed && raceWinner>=0)) return
        val q=if(side==0) leftQ else rightQ
        if(q==null) return
        if(speed && (if(side==0) wrongLeft else wrongRight)) return
        if(!speed && (if(side==0) leftFeedback!=null else rightFeedback!=null)) return
        val correct=answer==q.answer
        if(!vsAi || side==0) {
            if(correct) feedback?.success() else feedback?.error()
        }
        val duration=(SystemClock.elapsedRealtime()-(if(side==0) leftStarted else rightStarted))
            .coerceAtLeast(0L)
        if(speed) {
            if(!correct) {
                if(side==0) {
                    wrongLeft=true;leftFeedback=DuelFeedback(answer,false)
                } else {
                    wrongRight=true;rightFeedback=DuelFeedback(answer,false)
                }
                return
            }
            if(side==0) {
                leftScore++;leftTime+=duration;leftFeedback=DuelFeedback(answer,true)
            } else {
                rightScore++;rightTime+=duration;rightFeedback=DuelFeedback(answer,true)
            }
            raceWinner=side
        } else {
            if(side==0) {
                if(correct) leftScore++
                leftTime+=duration
                leftFeedback=DuelFeedback(answer,correct)
            } else {
                if(correct) rightScore++
                rightTime+=duration
                rightFeedback=DuelFeedback(answer,correct)
            }
        }
    }

    // Wrong speed answers briefly lock only the player who made the mistake.
    LaunchedEffect(wrongLeft,raceIndex) {
        if(wrongLeft) {
            delay(850)
            wrongLeft=false
            if(raceWinner<0) leftFeedback=null
        }
    }
    LaunchedEffect(wrongRight,raceIndex) {
        if(wrongRight) {
            delay(850)
            wrongRight=false
            if(raceWinner<0) rightFeedback=null
        }
    }
    // Accuracy: players advance independently, without forcing the other player forward.
    LaunchedEffect(leftFeedback,leftIndex,speed) {
        if(!speed && leftFeedback!=null) {
            delay(1300)
            leftIndex++
            leftFeedback=null
            leftStarted=SystemClock.elapsedRealtime()
        }
    }
    LaunchedEffect(rightFeedback,rightIndex,speed) {
        if(!speed && rightFeedback!=null) {
            delay(1300)
            rightIndex++
            rightFeedback=null
            rightStarted=SystemClock.elapsedRealtime()
        }
    }
    LaunchedEffect(raceWinner,raceIndex,speed) {
        if(speed && raceWinner>=0) {
            // Give both players enough time to read the successful-buzz notice.
            delay(DuelRaceNotice.SUCCESS_DISPLAY_MS)
            if(raceIndex+1>=questions.size) done=true
            else {
                raceIndex++
                raceWinner=-1
                leftFeedback=null
                rightFeedback=null
                wrongLeft=false
                wrongRight=false
                val now=SystemClock.elapsedRealtime()
                leftStarted=now
                rightStarted=now
            }
        }
    }
    val aiIndex=if(speed) raceIndex else rightIndex
    // Coroutines can outlive a recomposition. Always submit against the CURRENT
    // score, question, winning side and answer state, not a captured old closure.
    val submitCurrentAi by rememberUpdatedState(newValue = { correctAnswer: String ->
        if(vsAi && !finished && (!speed || raceWinner<0)) {
            submit(1,correctAnswer)
        }
    })
    // Each question receives a fresh fixed-range randomized response delay.
    LaunchedEffect(aiIndex,vsAi,aiLevel,speed,questions) {
        if(vsAi && !finished && rightQ!=null) {
            delay(DuelRules.reactionMs(aiLevel))
            questions.getOrNull(aiIndex)?.let { submitCurrentAi(it.answer) }
        }
    }
    if(finished) {
        LaunchedEffect(questions) {onCompleted()}
        val winner=if(speed) when {
            leftScore>rightScore->0
            rightScore>leftScore->1
            else -> -1
        } else DuelRules.winnerByAccuracy(leftScore,rightScore,leftTime,rightTime)
        DuelResultScreen(winner,vsAi,speed,leftScore,rightScore,
            leftTime,rightTime,onAgain,onHome)
        return
    }
    if(vsAi) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val tabletLandscape=ResponsiveWindowPolicy.tabletLandscape(maxWidth.value,maxHeight.value)
            if(tabletLandscape){
                val reveal=speed && raceWinner>=0 || !speed && leftFeedback!=null
                val mayReveal=if(speed) raceWinner>=0 else
                    rightFeedback!=null && (leftIndex>rightIndex ||
                        leftIndex==rightIndex && leftFeedback!=null)
                Column(Modifier.fillMaxSize().padding(horizontal=21.dp,vertical=13.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    TopLine("🤖 AI 知识对决",onBack,
                        "🔵 $leftScore : $rightScore 🤖",vibrateOnBack=false)
                    PinyinText(if(speed) "同一道题 · 第 ${raceIndex+1}/${questions.size} 题"
                        else "你的进度 ${leftIndex.coerceAtMost(questions.size)}/${questions.size} · AI ${rightIndex.coerceAtMost(questions.size)}/${questions.size}",
                        fontSize=15.sp,color=Ink,fontWeight=FontWeight.Bold)
                    if(speed && raceWinner>=0) {
                        PinyinText(if(raceWinner==0) "✅ 你抢答成功！"
                            else "⚡ AI 已抢答成功！",
                            fontSize=19.sp,color=Ink,fontWeight=FontWeight.Black)
                    }
                    Row(Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        StoryCard(Modifier.weight(1.27f).fillMaxHeight(),Color.White) {
                            if(leftQ!=null){
                                BoxWithConstraints(Modifier.fillMaxSize()) {
                                    val availablePaneHeight=maxHeight
                                    Column(Modifier.fillMaxSize()
                                        .verticalScroll(rememberScrollState())){
                                        DuelQuestion(leftQ,"🔵 你的题目",leftFeedback,reveal,
                                            speed && (raceWinner>=0||wrongLeft) ||
                                                !speed && leftFeedback!=null,
                                            Modifier.fillMaxWidth().heightIn(min=availablePaneHeight),
                                            false,{submit(0,it)},spread=true)
                                    }
                                }
                            } else {
                                Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,
                                    horizontalAlignment=Alignment.CenterHorizontally){
                                    PinyinText("✅ 你已完成，正在等待 AI！",
                                        color=Sea,fontSize=23.sp,fontWeight=FontWeight.Black)
                                }
                            }
                        }
                        StoryCard(Modifier.weight(.73f).fillMaxHeight(),SoftBlue) {
                            Column(Modifier.fillMaxSize(),
                                verticalArrangement=Arrangement.SpaceEvenly,
                                horizontalAlignment=Alignment.CenterHorizontally) {
                                PinyinText("🤖 AI 对手",fontSize=27.sp,color=Ink,
                                    fontWeight=FontWeight.Black)
                                PinyinText(listOf("简单","中等","困难","噩梦")[aiLevel.coerceIn(0,3)],
                                    fontSize=21.sp,color=Sea,fontWeight=FontWeight.Bold)
                                PinyinText("🔵 $leftScore : $rightScore 🤖",
                                    fontSize=29.sp,color=Ink,fontWeight=FontWeight.Black)
                                PinyinText(when {
                                    rightQ==null -> "AI 已完成全部题目"
                                    rightFeedback==null -> "AI 正在思考中……"
                                    mayReveal -> "AI 已答对：${rightQ.answer}"
                                    else -> "AI 已提交，等你完成同一道题再揭晓"
                                },fontSize=18.sp,color=Ink,textAlign=TextAlign.Center,
                                    lineHeight=26.sp)
                            }
                        }
                    }
                }
            } else {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(15.dp),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            TopLine("🤖 AI 知识对决",onBack,"🔵 $leftScore : $rightScore 🤖",vibrateOnBack=false)
            PinyinText(if(speed) "同一道题 · 第 ${raceIndex+1}/${questions.size} 题"
                else "你的进度 ${leftIndex.coerceAtMost(questions.size)}/${questions.size} · AI ${rightIndex.coerceAtMost(questions.size)}/${questions.size}",
                fontSize=15.sp,color=Ink,fontWeight=FontWeight.Bold)
            if(speed && raceWinner>=0) {
                PinyinText(if(raceWinner==0) "✅ 你抢答成功！"
                    else "⚡ AI 已抢答成功！",
                    fontSize=19.sp,color=Ink,fontWeight=FontWeight.Black)
            }
            if(leftQ!=null) {
                val reveal=speed && raceWinner>=0 || !speed && leftFeedback!=null
                DuelQuestion(leftQ,"🔵 你的题目",leftFeedback,reveal,
                    speed && (raceWinner>=0||wrongLeft) || !speed && leftFeedback!=null,
                    Modifier.fillMaxWidth(),false,{ submit(0,it) })
            } else PinyinText("✅ 你已完成，等待 AI……",color=Ink,fontSize=21.sp)
            StoryCard(Modifier.fillMaxWidth(),SoftBlue) {
                PinyinText("🤖 AI · ${listOf("简单","中等","困难","噩梦")[aiLevel.coerceIn(0,3)]}",
                    fontSize=16.sp,fontWeight=FontWeight.Black,color=Ink)
                val mayReveal=if(speed) raceWinner>=0 else
                    rightFeedback!=null && (leftIndex>rightIndex ||
                        leftIndex==rightIndex && leftFeedback!=null)
                PinyinText(when {
                    rightQ==null -> "AI 已完成全部题目"
                    rightFeedback==null -> "AI 正在思考中……"
                    mayReveal -> "AI 已答对：${rightQ.answer}"
                    else -> "AI 已提交，等你完成同一道题再揭晓"
                },fontSize=15.sp,color=Ink)
            }
        }

            }
        }
    } else {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            if(maxWidth<maxHeight) {
                Column(Modifier.fillMaxSize().padding(22.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.Center) {
                    PinyinText("↔️ 请将平板横过来",fontSize=24.sp,fontWeight=FontWeight.Black,color=Ink)
                    PinyinText("双人对战需要横屏，各自坐在平板短边的一侧。",color=Ink)
                    KidLink("← 返回",onBack,vibrateOnClick=false)
                }
            } else {
                Column(Modifier.fillMaxSize().padding(6.dp),
                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        KidPill("← 退出对决",onBack,vibrateOnClick=false)
                        Spacer(Modifier.weight(1f))
                        PinyinText(if(speed) "⚡ 速度优先" else "🎯 正确率优先",
                            color=Ink,fontWeight=FontWeight.Black,fontSize=13.sp)
                    }
                    Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        RotatedSeat(leftQ,0,questions.size,
                            if(speed) raceIndex else leftIndex,leftFeedback,
                            if(speed) raceWinner>=0 else leftFeedback!=null,
                            if(speed) raceWinner>=0||wrongLeft else leftFeedback!=null,
                            DuelRaceNotice.forSide(speed,raceWinner,0),
                            Modifier.weight(1f).fillMaxHeight()) {submit(0,it)}
                        DuelScoreCenter(leftScore,rightScore,questions.size,
                            if(speed) raceIndex else leftIndex,
                            if(speed) raceIndex else rightIndex)
                        RotatedSeat(rightQ,1,questions.size,
                            if(speed) raceIndex else rightIndex,rightFeedback,
                            if(speed) raceWinner>=0 else rightFeedback!=null,
                            if(speed) raceWinner>=0||wrongRight else rightFeedback!=null,
                            DuelRaceNotice.forSide(speed,raceWinner,1),
                            Modifier.weight(1f).fillMaxHeight()) {submit(1,it)}
                    }
                }
            }
        }
    }
}

@Composable
private fun DuelScoreCenter(left:Int,right:Int,total:Int,leftIndex:Int,rightIndex:Int) {
    Column(Modifier.width(79.dp).fillMaxHeight().clip(RoundedCornerShape(19.dp))
        .background(Color.White.copy(alpha=.91f)).padding(5.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.SpaceBetween) {
        PinyinText("第 ${(leftIndex+1).coerceAtMost(total)}/$total 题",
            fontSize=12.sp,color=Ink,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
        Column(horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(4.dp)) {
            PinyinText("$left : $right",fontSize=25.sp,color=Ink,
                fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
            PinyinText("🔵 VS 🟠",fontSize=11.sp,color=Ink,textAlign=TextAlign.Center)
            PinyinText("比分",fontSize=12.sp,color=Sea)
        }
        PinyinText("第 ${(rightIndex+1).coerceAtMost(total)}/$total 题",
            fontSize=12.sp,color=Ink,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
    }
}

@Composable
private fun RotatedSeat(q:KnowledgeQuestion?,side:Int,total:Int,
                        index:Int,feedback:DuelFeedback?,reveal:Boolean,
                        disabled:Boolean,status:String,modifier:Modifier,onAnswer:(String)->Unit) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(16.dp))
        .background(if(side==0) SoftBlue else Color(0xFFFFEDDB)),
        contentAlignment=Alignment.Center) {
        val logicalWidth=maxHeight
        val logicalHeight=maxWidth
        // View-space is switched before rotation so each touch target stays in its own physical half.
        Column(Modifier.requiredWidth(logicalWidth).requiredHeight(logicalHeight)
            .graphicsLayer {rotationZ=DuelSeatLayout.rotationFor(side)}
            .padding(9.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            PinyinText(if(side==0) "🔵 蓝方探险家" else "🟠 橙方探险家",
                fontSize=16.sp,color=Ink,fontWeight=FontWeight.Black)
            // Keep the notice OUTSIDE the independently scrollable question.
            // A player who loses the race can always see the result at a glance.
            if(status.isNotBlank()) {
                PinyinText(status,
                    modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha=.80f))
                        .padding(horizontal=7.dp,vertical=6.dp),
                    fontSize=if(status.contains("抢答成功")) 16.sp else 13.sp,
                    color=Ink,fontWeight=FontWeight.Black,
                    textAlign=TextAlign.Center)
            }
            if(q!=null) {
                if(logicalHeight>=440.dp) {
                    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)){
                        val availablePaneHeight=maxHeight
                        Column(Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())){
                            DuelQuestion(q,"第 ${index+1}/$total 题",
                                feedback,reveal,disabled,
                                Modifier.fillMaxWidth().heightIn(min=availablePaneHeight),
                                true,onAnswer,spread=true)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().weight(1f)
                        .verticalScroll(rememberScrollState())){
                        DuelQuestion(q,"第 ${index+1}/$total 题",
                            feedback,reveal,disabled,Modifier.fillMaxWidth(),true,onAnswer)
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
                    PinyinText("✅ 我已经完成啦！",color=Ink,fontSize=20.sp)
                }
            }
        }
    }
}

@Composable
private fun DuelQuestion(q:KnowledgeQuestion,heading:String,feedback:DuelFeedback?,
                         reveal:Boolean,disabled:Boolean,modifier:Modifier,
                         compact:Boolean,onAnswer:(String)->Unit,
                         spread:Boolean=false) {
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=if(spread) Arrangement.SpaceEvenly
            else Arrangement.spacedBy(if(compact) 7.dp else 12.dp)) {
        PinyinText(heading,color=Sea,fontSize=if(compact) 14.sp else 18.sp,
            fontWeight=FontWeight.Black)
        PinyinText(q.question,fontSize=if(compact) 18.sp else 23.sp,
            lineHeight=if(compact) 25.sp else 31.sp,
            color=Ink,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
        val choices=remember(q.id) {q.options.shuffled(Random(q.id.hashCode()))}
        choices.chunked(2).forEach { options ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    ChoiceButton(option,if(reveal) when(option) {
                        q.answer -> true
                        feedback?.selected -> false
                        else -> null
                    } else if(feedback?.selected==option) false else null,
                        !disabled,{onAnswer(option)},Modifier.weight(1f),compact)
                }
            }
        }
        if(reveal) {
            PinyinText("答案：${q.answer}",color=Sea,fontSize=if(compact) 14.sp else 17.sp,
                fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
        } else if(feedback!=null) {
            PinyinText("🌱 再试一次！",color=Ink,fontSize=14.sp)
        } else {
            PinyinText("先想好，再作答",color=Ink.copy(alpha=.75f),fontSize=13.sp)
        }
    }
}
