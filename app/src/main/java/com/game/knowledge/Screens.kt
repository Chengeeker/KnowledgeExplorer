package com.game.knowledge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.SystemClock
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(total:Int,played:Int,scopeSummary:String,onStudy:()->Unit,onQuiz:()->Unit,onDuel:()->Unit,onCantonese:()->Unit,onSettings:()->Unit) {
    val feedback=LocalKidFeedback.current
    val reading=LocalShowPinyin.current
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val tabletLandscape=ResponsiveWindowPolicy.tabletLandscape(maxWidth.value,maxHeight.value)
        if(tabletLandscape){
            // Ruby almost doubles text height. In landscape allow the home to
            // scroll rather than forcing four game entries into fixed heights.
            Column(Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal=23.dp,vertical=if(reading) 11.dp else 16.dp),
                verticalArrangement=Arrangement.spacedBy(if(reading) 10.dp else 15.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        PinyinText("🔎 万象探险家",fontSize=if(reading) 27.sp else 34.sp,
                            fontWeight=FontWeight.Black,color=Ink)
                        PinyinText("在好奇心里，发现整个世界！",fontSize=if(reading) 12.sp else 15.sp,
                            color=Sea,fontWeight=FontWeight.Bold)
                    }
                    KidPill("⚙️ 设置",onSettings)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
                    StoryCard(Modifier.weight(1f),Color(0xFFE1F6E9)){
                        PinyinText("📖 百科探索地图",fontSize=if(reading) 17.sp else 20.sp,color=Ink,
                            fontWeight=FontWeight.Black)
                        PinyinText("离线收录 $total 道趣味知识 · 已完成 $played 轮闯关",
                            fontSize=if(reading) 12.sp else 14.sp,color=Ink)
                    }
                    StoryCard(Modifier.weight(1f),Color(0xFFFFFAED)){
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                PinyinText("🎯 当前题目范围",fontSize=if(reading) 15.sp else 17.sp,
                                    color=Ink,fontWeight=FontWeight.Black)
                                PinyinText(scopeSummary,fontSize=if(reading) 11.sp else 13.sp,color=Ink)
                            }
                            KidPill("修改",onSettings)
                        }
                    }
                }
                PinyinText("🌈 选择你的探险模式",color=Ink,
                    fontSize=if(reading) 18.sp else 21.sp,fontWeight=FontWeight.Black)
                // Four equally prominent game entries: 2×2 tablet grid.
                // The previous three-card weighted row used nearly all height
                // and squeezed Cantonese into a tiny ribbon underneath.
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    MainModeTile("🌿","科普小课堂","自由探索知识，3 秒后揭晓答案",
                        Color(0xFFD5F5DE),onStudy,
                        Modifier.weight(1f).heightIn(min=if(reading) 174.dp else 150.dp),
                        tablet=true)
                    MainModeTile("🏆","知识大闯关","四选一闯关，每题都能学到新知识",
                        Color(0xFFFFECD0),onQuiz,
                        Modifier.weight(1f).heightIn(min=if(reading) 174.dp else 150.dp),
                        tablet=true)
                }
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    MainModeTile("⚔️","知识对决","平板双人同屏 PK，或挑战四档 AI",
                        Color(0xFFDDECFB),onDuel,
                        Modifier.weight(1f).heightIn(min=if(reading) 174.dp else 150.dp),
                        tablet=true)
                    MainModeTile("🦁","粤语乐园","听粤语、对话闯关、广东文化和小游戏",
                        Color(0xFFF1E5FF),onCantonese,
                        Modifier.weight(1f).heightIn(min=if(reading) 174.dp else 150.dp),
                        tablet=true)
                }
                PinyinText("所有模式使用同一套离线百科题库，无需联网。",
                    fontSize=if(reading) 11.sp else 13.sp,color=Ink.copy(alpha=.72f))
            }
        } else {
        val wide=maxWidth>=680.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal=if(wide) 28.dp else 18.dp,
                vertical=if(reading) 11.dp else 18.dp),
            verticalArrangement=Arrangement.spacedBy(if(reading) 11.dp else 16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    PinyinText("🔎 万象探险家",fontSize=when {
                            reading && wide -> 27.sp
                            reading -> 24.sp
                            wide -> 36.sp
                            else -> 29.sp
                        },
                        fontWeight=FontWeight.Black,color=Ink)
                    PinyinText("在好奇心里，发现整个世界！",
                        fontSize=if(reading) 12.sp else 16.sp,color=Sea,fontWeight=FontWeight.Bold)
                }
                KidPill("⚙️ 设置",onSettings)
            }
            StoryCard(Modifier.fillMaxWidth(),tint=Color(0xFFE1F6E9)) {
                Row(verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    PinyinText("📖",fontSize=if(reading) 34.sp else 46.sp)
                    Column(Modifier.weight(1f)) {
                        PinyinText("百科探索地图",
                            fontSize=if(reading) 18.sp else 21.sp,
                            fontWeight=FontWeight.Black,color=Ink)
                        PinyinText("离线收录 $total 道趣味知识 · 已完成 $played 轮闯关",
                            fontSize=if(reading) 12.sp else 14.sp,color=Ink)
                        PinyinText("动物 · 自然 · 地理 · 文化 · 科技 · 生活",
                            fontSize=if(reading) 11.sp else 13.sp,color=Sea)
                    }
                }
            }
            StoryCard(Modifier.fillMaxWidth(),tint=Color(0xFFFFFAED)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        PinyinText("🎯 当前题目范围",color=Ink,fontWeight=FontWeight.Black,
                            fontSize=if(reading) 14.sp else 16.sp)
                        PinyinText(scopeSummary,color=Ink.copy(alpha=.78f),
                            fontSize=if(reading) 11.sp else 13.sp)
                    }
                    KidPill("修改",onSettings)
                }
            }
            if(wide) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(15.dp)) {
                    MainModeTile("🌿","科普小课堂","不用答题，先看一看，再点击揭晓知识",
                        Color(0xFFD5F5DE),onStudy,Modifier.weight(1f))
                    MainModeTile("🏆","知识大闯关","四选一答题，发现今天的新本领",
                        Color(0xFFFFECD0),onQuiz,Modifier.weight(1f))
                }
            } else {
                MainModeTile("🌿","科普小课堂","不用答题，先看一看，再点击揭晓知识",
                    Color(0xFFD5F5DE),onStudy,Modifier.fillMaxWidth())
                MainModeTile("🏆","知识大闯关","四选一答题，发现今天的新本领",
                    Color(0xFFFFECD0),onQuiz,Modifier.fillMaxWidth())
            }
            MainModeTile("⚔️","知识对决","平板双人同屏 PK，或者挑战四档 AI",
                Color(0xFFDDECFB),onDuel,Modifier.fillMaxWidth())
            MainModeTile("🦁","粤语乐园","听粤语、点心小掌柜、词语宝典与自愿跟读",
                Color(0xFFF1E5FF),onCantonese,Modifier.fillMaxWidth())
            PinyinText("所有模式使用相同的百科题库，游戏无需联网。",
                fontSize=if(reading) 11.sp else 13.sp,
                color=Ink.copy(alpha=.75f),modifier=Modifier.padding(bottom=10.dp))
        }

        }
    }
}
@Composable
private fun MainModeTile(emoji:String,title:String,subtitle:String,
                         color:Color,onClick:()->Unit,modifier:Modifier,
                         tablet:Boolean=false) {
    val feedback=LocalKidFeedback.current
    val reading=LocalShowPinyin.current
    StoryCard(modifier.clickable {feedback?.tap();onClick()},color) {
        if(tablet && reading) {
            Column(Modifier.fillMaxWidth().padding(vertical=2.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(3.dp)) {
                PinyinText(emoji,fontSize=36.sp,textAlign=TextAlign.Center)
                PinyinText(title,color=Ink,fontSize=18.sp,
                    fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
                PinyinText(subtitle,color=Ink,fontSize=11.sp,
                    maxLines=4,textAlign=TextAlign.Center)
            }
        } else {
            Row(Modifier.fillMaxWidth(),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(if(reading) 8.dp else 12.dp)) {
                PinyinText(emoji,fontSize=if(tablet) 55.sp else if(reading) 35.sp else 48.sp,
                    modifier=Modifier.width(if(reading) 43.dp else 65.dp),
                    textAlign=TextAlign.Center)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    PinyinText(title,color=Ink,
                        fontSize=if(reading) 19.sp else 23.sp,
                        fontWeight=FontWeight.Black)
                    PinyinText(subtitle,color=Ink,
                        fontSize=if(reading) 12.sp else 14.sp,
                        lineHeight=if(reading) 19.sp else 20.sp)
                }
                PinyinText("›",fontSize=if(reading) 23.sp else 32.sp,
                    color=Sea,fontWeight=FontWeight.Black)
            }
        }
    }
}

@Composable
fun StudyScreen(questions:List<KnowledgeQuestion>,narrator:MandarinNarrator,
                autoRead:Boolean,onBack:()->Unit,onAgain:()->Unit,onHome:()->Unit,
                onCompleted:()->Unit) {
    var index by remember(questions) { mutableIntStateOf(0) }
    var reveal by remember(index) { mutableStateOf(false) }
    if(questions.isEmpty()) return
    if(index >= questions.size) {
        LaunchedEffect(questions) { onCompleted() }
        ResultPage("🌈 发现新知识！","你已经看完 ${questions.size} 张百科知识卡！",
            onHome,onAgain,"再探索一组")
        return
    }
    val q=questions[index]
    val thinkingStartedAt=remember(q.id) {SystemClock.elapsedRealtime()}
    var thinkingClock by remember(q.id) {mutableLongStateOf(thinkingStartedAt)}
    LaunchedEffect(q.id) {
        while(!ExplanationGate.canReveal(thinkingStartedAt,SystemClock.elapsedRealtime(),ExplanationGate.STUDY_WAIT_MS)) {
            delay(150L)
            thinkingClock=SystemClock.elapsedRealtime()
        }
        thinkingClock=SystemClock.elapsedRealtime()
    }
    val waitSeconds=ExplanationGate.secondsRemaining(thinkingStartedAt,thinkingClock,ExplanationGate.STUDY_WAIT_MS)
    LaunchedEffect(q.id,autoRead,narrator.ready) {
        if(autoRead && narrator.ready) narrator.say(q.question)
    }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val tabletLandscape=ResponsiveWindowPolicy.tabletLandscape(maxWidth.value,maxHeight.value)
        if(tabletLandscape) {
            Column(Modifier.fillMaxSize().padding(horizontal=23.dp,vertical=13.dp),
                verticalArrangement=Arrangement.spacedBy(13.dp)) {
                TopLine("📖 科普小课堂",onBack,"${index+1}/${questions.size}",vibrateOnBack=false)
                LinearProgressIndicator(progress={index.toFloat()/questions.size},
                    modifier=Modifier.fillMaxWidth(),color=Sea,trackColor=Color.White)
                Row(Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement=Arrangement.spacedBy(17.dp)) {
                    EncyclopediaIllustration(q,Modifier.weight(.88f).fillMaxHeight())
                    TabletStudyBody(q,reveal,narrator,thinkingStartedAt,waitSeconds,
                        onReveal={reveal=true},onNext={index++},
                        onPrevious={index=(index-1).coerceAtLeast(0)},
                        hasPrevious=index>0,modifier=Modifier.weight(1.12f).fillMaxHeight())
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal=14.dp,vertical=12.dp),
                verticalArrangement=Arrangement.spacedBy(13.dp)) {
                TopLine("📖 科普小课堂",onBack,"${index+1}/${questions.size}",vibrateOnBack=false)
                LinearProgressIndicator(progress={index.toFloat()/questions.size},
                    modifier=Modifier.fillMaxWidth(),color=Sea,trackColor=Color.White)
                EncyclopediaIllustration(q,Modifier.fillMaxWidth().height(180.dp))
                StudyBody(q,reveal,narrator,thinkingStartedAt,waitSeconds,
                    onReveal={reveal=true},onNext={index++},
                    onPrevious={index=(index-1).coerceAtLeast(0)},
                    hasPrevious=index>0,modifier=Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StudyBody(q:KnowledgeQuestion,reveal:Boolean,narrator:MandarinNarrator,
                      startedAt:Long,waitSeconds:Long,onReveal:()->Unit,
                      onNext:()->Unit,onPrevious:()->Unit,hasPrevious:Boolean,
                      modifier:Modifier) {
    StoryCard(modifier) {
        PinyinText("🌟 ${q.grade}年级 · ${q.domain} · ${q.topic}",fontSize=13.sp,
            color=Sea,fontWeight=FontWeight.Bold)
        PinyinText(q.question,fontSize=24.sp,lineHeight=33.sp,color=Ink,
            fontWeight=FontWeight.Black)
        if(reveal) {
            HorizontalDivider(color=Color(0xFFD2E8DB))
            PinyinText("原来答案是……",fontSize=16.sp,color=Sea,fontWeight=FontWeight.Bold)
            PinyinText(q.answer,fontSize=27.sp,color=Ink,fontWeight=FontWeight.Black)
            if(q.explanation.isNotBlank())
                PinyinText(q.explanation,fontSize=17.sp,color=Ink,lineHeight=26.sp)
            KidLink("🔊 听答案和解释",{narrator.say(q.answer+"。"+q.explanation)},
                enabled=narrator.ready)
            LargeAction("继续探索 →",onNext,Modifier.fillMaxWidth(),Coral)
        } else {
            PinyinText(
                if(waitSeconds>0) "🧠 先观察和思考，${waitSeconds} 秒后再揭晓答案"
                else "✨ 思考时间到了，看看你猜得对不对！",
                fontSize=14.sp,color=Ink,fontWeight=FontWeight.Bold
            )
            KidLink("🔊 朗读题目",{narrator.say(q.question)},
                enabled=narrator.ready)
            LargeAction(if(waitSeconds>0) "🔒 先思考 ${waitSeconds} 秒" else "✨ 揭晓答案",{
                // Never trust only a possibly stale UI countdown.
                if(ExplanationGate.canReveal(startedAt,SystemClock.elapsedRealtime(),ExplanationGate.STUDY_WAIT_MS)) {
                    narrator.say(q.answer+"。"+q.explanation)
                    onReveal()
                }
            },Modifier.fillMaxWidth(),Sea,enabled=waitSeconds==0L)
        }
        if(hasPrevious)
            KidLink("← 上一张知识卡",onPrevious,Modifier.align(Alignment.Start))
    }
}

@Composable
fun ChallengeScreen(questions:List<KnowledgeQuestion>,narrator:MandarinNarrator,
                    autoRead:Boolean,onBack:()->Unit,onAgain:()->Unit,
                    onFinished:(Int)->Unit,onHome:()->Unit,onCompleted:()->Unit) {
    if(questions.isEmpty()) return
    var index by remember(questions) { mutableIntStateOf(0) }
    var score by remember(questions) { mutableIntStateOf(0) }
    val feedback=LocalKidFeedback.current
    var choice by remember(index,questions) { mutableStateOf<String?>(null) }
    val mistakes=remember(questions) { mutableStateListOf<KnowledgeQuestion>() }
    if(index>=questions.size) {
        LaunchedEffect(questions) {onFinished(score);onCompleted()}
        QuizResultScreen(questions.size,score,mistakes,onAgain,onHome)
        return
    }
    val q=questions[index]
    val thinkingStartedAt=remember(q.id) {SystemClock.elapsedRealtime()}
    var thinkingClock by remember(q.id) {mutableLongStateOf(thinkingStartedAt)}
    LaunchedEffect(q.id) {
        while(!ExplanationGate.canReveal(thinkingStartedAt,SystemClock.elapsedRealtime(),ExplanationGate.QUIZ_WAIT_MS)) {
            delay(150L)
            thinkingClock=SystemClock.elapsedRealtime()
        }
        thinkingClock=SystemClock.elapsedRealtime()
    }
    val waitSeconds=ExplanationGate.secondsRemaining(thinkingStartedAt,thinkingClock,ExplanationGate.QUIZ_WAIT_MS)
    LaunchedEffect(q.id,autoRead,narrator.ready) {
        if(autoRead && narrator.ready) narrator.say(q.question)
    }
    val options=remember(q.id) {q.options.shuffled()}
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val tabletLandscape=ResponsiveWindowPolicy.tabletLandscape(maxWidth.value,maxHeight.value)
        if(tabletLandscape){
            Column(Modifier.fillMaxSize().padding(horizontal=23.dp,vertical=12.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                TopLine("🏆 知识大闯关",onBack,"${index+1}/${questions.size}  ·  ⭐$score",vibrateOnBack=false)
                LinearProgressIndicator(progress={index.toFloat()/questions.size},
                    modifier=Modifier.fillMaxWidth(),color=Coral,trackColor=Color.White)
                Row(Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement=Arrangement.spacedBy(17.dp)) {
                    EncyclopediaIllustration(q,Modifier.weight(.75f).fillMaxHeight())
                    TabletChallengeBody(q,options,choice,narrator,thinkingStartedAt,waitSeconds,
                        onChoose={value->
                            if(choice==null){
                                choice=value
                                if(value==q.answer){feedback?.success();score++}
                                else {feedback?.error();mistakes.add(q)}
                            }
                        },onNext={index++},
                        modifier=Modifier.weight(1.25f).fillMaxHeight())
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal=14.dp,vertical=11.dp),
                verticalArrangement=Arrangement.spacedBy(11.dp)) {
                TopLine("🏆 知识大闯关",onBack,"${index+1}/${questions.size}  ·  ⭐$score",vibrateOnBack=false)
                LinearProgressIndicator(progress={index.toFloat()/questions.size},
                    modifier=Modifier.fillMaxWidth(),color=Coral,trackColor=Color.White)
                EncyclopediaIllustration(q,Modifier.fillMaxWidth().height(130.dp))
                ChallengeBody(q,options,choice,narrator,thinkingStartedAt,waitSeconds,
                    Modifier.fillMaxWidth(),
                    onChoose={value->
                        if(choice==null) {
                            choice=value
                            if(value==q.answer) {feedback?.success();score++}
                            else {feedback?.error();mistakes.add(q)}
                        }
                    },onNext={index++})
            }
        }
    }
}

@Composable
private fun ChallengeBody(q:KnowledgeQuestion,options:List<String>,choice:String?,
                          narrator:MandarinNarrator,startedAt:Long,waitSeconds:Long,
                          modifier:Modifier,onChoose:(String)->Unit,onNext:()->Unit) {
    StoryCard(modifier) {
        PinyinText("${q.grade}年级 · ${q.domain} · ${q.topic}",fontSize=13.sp,color=Sea,
            fontWeight=FontWeight.Bold)
        PinyinText(q.question,color=Ink,fontSize=22.sp,lineHeight=30.sp,fontWeight=FontWeight.Black)
        options.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                pair.forEach { option ->
                    ChoiceButton(option,if(choice==null) null else when(option) {
                        q.answer -> true
                        choice -> false
                        else -> null
                    },choice==null,{onChoose(option)},Modifier.weight(1f))
                }
            }
        }
        if(choice!=null) {
            PinyinText(if(choice==q.answer) "✅ 答对啦！" else "🌱 学到新知识啦！",
                color=Sea,fontSize=19.sp,fontWeight=FontWeight.Black)
            PinyinText("正确答案：${q.answer}",fontSize=17.sp,fontWeight=FontWeight.Bold,color=Ink)
            KidLink("🔊 朗读答案和解析",{narrator.say(q.answer+"。"+q.explanation)},
                enabled=narrator.ready)
            if(q.explanation.isNotBlank())
                PinyinText(q.explanation,fontSize=15.sp,color=Ink,lineHeight=22.sp)
            LargeAction("下一题 →",onNext,Modifier.fillMaxWidth(),Coral)
        } else {
            PinyinText("选择你认为正确的答案",fontSize=13.sp,color=Ink.copy(alpha=.75f))
            KidLink("🔊 朗读题目",{narrator.say(q.question)},enabled=narrator.ready)
            PinyinText(
                if(waitSeconds>0) "🧠 再想想：${waitSeconds} 秒后才可以跳过；四个选项现在就能作答。"
                else "想不出来也没关系，可以跳过并认真看看解析。",
                fontSize=13.sp,color=Ink.copy(alpha=.8f))
            KidLink(
                if(waitSeconds>0) "🔒 ${waitSeconds} 秒后可跳过" else "💡 实在不会，查看答案",
                {if(ExplanationGate.canReveal(startedAt,SystemClock.elapsedRealtime(),ExplanationGate.QUIZ_WAIT_MS))
                    onChoose("__skip__")},
                enabled=waitSeconds==0L)
        }
    }
}

@Composable
fun ResultPage(title:String,details:String,onBack:()->Unit,onAgain:()->Unit,againLabel:String) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val tabletLandscape=ResponsiveWindowPolicy.tabletLandscape(maxWidth.value,maxHeight.value)
        if(tabletLandscape) {
            Row(Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement=Arrangement.spacedBy(22.dp)){
                StoryCard(Modifier.weight(.85f).fillMaxHeight(),PaleMint){
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                        PinyinText("🌻",fontSize=145.sp)
                    }
                }
                StoryCard(Modifier.weight(1.15f).fillMaxHeight()){
                    Column(Modifier.fillMaxSize(),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.SpaceEvenly){
                        PinyinText(title,fontSize=32.sp,fontWeight=FontWeight.Black,
                            color=Ink,textAlign=TextAlign.Center)
                        PinyinText(details,fontSize=21.sp,color=Ink,
                            textAlign=TextAlign.Center)
                        LargeAction(againLabel,onAgain,Modifier.fillMaxWidth(),Coral)
                        KidPill("🏠 返回首页",onBack,Modifier.fillMaxWidth())
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(23.dp),horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(18.dp)) {
                PinyinText("🌻",fontSize=90.sp)
                PinyinText(title,fontSize=30.sp,fontWeight=FontWeight.Black,color=Ink)
                PinyinText(details,fontSize=18.sp,color=Ink,textAlign=TextAlign.Center)
                LargeAction(againLabel,onAgain,Modifier.fillMaxWidth(),Coral)
                KidLink("返回首页",onBack)
            }
        }
    }
}
