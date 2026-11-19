package com.game.knowledge

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(216,247,233)
        window.navigationBarColor = android.graphics.Color.rgb(255,249,240)
        setContent { KnowledgeTheme { WorldApp() } }
    }
}

private const val HOME = "home"
private const val SETTINGS = "settings"
private const val STUDY = "study"
private const val QUIZ = "quiz"
private const val DUEL = "duel"

@Composable
private fun WorldApp() {
    val context=LocalContext.current
    val activity=context as? Activity
    val prefs=remember {context.getSharedPreferences("knowledge_game",0)}
    val narrator=remember { MandarinNarrator(context.applicationContext) }
    val audio=remember { WorldAudio(context.applicationContext) }
    val feedback=remember { KidFeedback(context.applicationContext,audio) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle,narrator,audio) {
        narrator.onSpeakingChanged = audio::setNarrating
        val observer = LifecycleEventObserver { _,event ->
            when(event) {
                Lifecycle.Event.ON_START -> audio.setForeground(true)
                Lifecycle.Event.ON_STOP -> {
                    narrator.stop()
                    audio.setForeground(false)
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            narrator.onSpeakingChanged=null
            narrator.close()
            audio.close()
        }
    }
    val bank=remember {runCatching {KnowledgeBank.load(context)}.getOrElse {emptyList()}}
    var screen by rememberSaveable {mutableStateOf(HOME)}
    // The old "幼小衔接" and 1年级 were identical; migrate silently to 1年级.
    var grade by rememberSaveable {mutableIntStateOf(prefs.getInt("grade",1).let {if(it==0) 1 else it})}
    var domain by rememberSaveable {mutableStateOf(prefs.getString("domain","全部") ?: "全部")}
    var topic by rememberSaveable {mutableStateOf(prefs.getString("topic","全部") ?: "全部")}
    var kind by rememberSaveable {mutableStateOf(prefs.getString("kind","全部") ?: "全部")}
    var count by rememberSaveable {mutableIntStateOf(prefs.getInt("count",10))}
    val tablet=remember {
        val c=context.resources.configuration
        c.smallestScreenWidthDp>=600 ||
            (c.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK)>=
                android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
    }
    var pcmVoice by rememberSaveable {mutableStateOf(prefs.getBoolean("pcmVoice",tablet))}
    LaunchedEffect(pcmVoice) {narrator.setPcmEnabled(pcmVoice)}
    var autoRead by rememberSaveable {mutableStateOf(prefs.getBoolean("autoRead",true))}
    var haptics by rememberSaveable {mutableStateOf(prefs.getBoolean("haptics",true))}
    var musicEnabled by rememberSaveable {mutableStateOf(prefs.getBoolean("musicEnabled",true))}
    var musicVolume by rememberSaveable {mutableIntStateOf(prefs.getInt("musicVolume",55))}
    var soundEffects by rememberSaveable {mutableStateOf(prefs.getBoolean("soundEffects",true))}
    var withAi by rememberSaveable {mutableStateOf(prefs.getBoolean("withAi",false))}
    var duelMode by rememberSaveable {mutableStateOf(prefs.getString("duelMode","速度优先") ?: "速度优先")}
    var aiLevel by rememberSaveable {mutableIntStateOf(prefs.getInt("aiLevel",0))}
    var deck by remember {mutableStateOf<List<KnowledgeQuestion>>(emptyList())}
    var sessionNonce by rememberSaveable {mutableIntStateOf(0)}
    var exitConfirm by remember {mutableStateOf(false)}
    var noQuestions by remember {mutableStateOf(false)}
    var roundCompleted by remember { mutableStateOf(false) }
    feedback.enabled = haptics
    LaunchedEffect(musicEnabled,musicVolume,soundEffects) {
        audio.configure(musicEnabled,musicVolume,soundEffects)
    }
    val filter=QuestionFilter(grade,domain,topic,kind)

    fun save() {
        prefs.edit().putInt("grade",grade).putString("domain",domain)
            .putString("topic",topic).putString("kind",kind).putInt("count",count)
            .putBoolean("autoRead",autoRead).putBoolean("pcmVoice",pcmVoice)
            .putBoolean("haptics",haptics)
            .putBoolean("musicEnabled",musicEnabled).putInt("musicVolume",musicVolume)
            .putBoolean("soundEffects",soundEffects)
            .putBoolean("withAi",withAi).putString("duelMode",duelMode)
            .putInt("aiLevel",aiLevel).apply()
    }
    fun start(next:String) {
        val previous=prefs.getString("recentQuestionIds","").orEmpty().split(',')
            .filter { it.isNotBlank() }
        val selected=KnowledgeBank.round(bank,filter,count,avoidIds=previous.toSet())
        if(selected.isEmpty()) {noQuestions=true;return}
        prefs.edit().putString("recentQuestionIds",
            (selected.map { it.id }+previous).distinct().take(120).joinToString(",")).apply()
        deck=selected
        roundCompleted=false
        sessionNonce++
        screen=next
    }
    fun home() {narrator.stop();screen=HOME}
    LaunchedEffect(screen,deck) {
        if(screen !in listOf(STUDY,QUIZ)) narrator.stop()
        if(screen in listOf(STUDY,QUIZ,DUEL) && deck.isEmpty()) screen=HOME
    }
    // Opening an exit confirmation is not yet navigation: do not vibrate here.
    // The confirmation button emits the one intended vibration on actual exit.
    // When a Dialog is visible, let that dialog handle Android's back gesture.
    BackHandler(enabled=!exitConfirm && !noQuestions) {
        when(screen) {
            HOME -> exitConfirm=true
            STUDY,QUIZ,DUEL -> if(roundCompleted) home() else exitConfirm=true
            else -> {save();home()}
        }
    }

    CompositionLocalProvider(LocalKidFeedback provides feedback) {
        PaintedBackground {
            when(screen) {
                HOME -> HomeScreen(bank.size,prefs.getInt("played",0),
                    scopeSummary = "${if(grade==-1) "全部年级" else "${grade}年级"} · ${domain}${if(topic=="全部") "" else " · ${topic}"} · 每轮${count}题",
                    onStudy={start(STUDY)},
                    onQuiz={start(QUIZ)},
                    onDuel={start(DUEL)},
                    onSettings={screen=SETTINGS})
                SETTINGS -> KnowledgeSettings(
                    all=bank,filter=filter,count=count,
                    withAi=withAi,duelMode=duelMode,aiLevel=aiLevel,
                    autoRead=autoRead,haptics=haptics,pcmVoice=pcmVoice,
                    musicEnabled=musicEnabled,musicVolume=musicVolume,soundEffects=soundEffects,
                    voiceReady=narrator.ready,voiceStatus=narrator.message,
                    onFilter={grade=it.grade;domain=it.domain;topic=it.topic;kind=it.kind;save()},
                    onCount={count=it;save()},
                    onWithAi={withAi=it;save()},
                    onDuelMode={duelMode=it;save()},
                    onAiLevel={aiLevel=it;save()},
                    onAutoRead={autoRead=it;save()},
                    onPcmVoice={pcmVoice=it;save()},
                    onHaptics={haptics=it;feedback.enabled=it;save()},
                    onMusicEnabled={musicEnabled=it;save()},
                    onMusicVolume={musicVolume=it;save()},
                    onSoundEffects={soundEffects=it;save()},
                    onVoicePreview={narrator.say("你好！我是万象探险家，一起探索奇妙世界吧。")},
                    onBack={save();home()}
                )
                STUDY -> key(sessionNonce) {StudyScreen(deck,narrator,autoRead,
                    onBack={exitConfirm=true},onAgain={start(STUDY)},onHome={home()},
                    onCompleted={roundCompleted=true})}
                QUIZ -> key(sessionNonce) {ChallengeScreen(deck,narrator,autoRead,
                    onBack={exitConfirm=true},onAgain={start(QUIZ)},onHome={home()},
                    onCompleted={roundCompleted=true},
                    onFinished={correct->
                        prefs.edit().putInt("played",prefs.getInt("played",0)+1)
                            .putInt("correct",prefs.getInt("correct",0)+correct).apply()
                    })}
                DUEL -> key(sessionNonce) {DuelScreen(deck,withAi,duelMode=="速度优先",aiLevel,
                    onBack={exitConfirm=true},onAgain={start(DUEL)},onHome={home()},
                    onCompleted={roundCompleted=true})}
            }
            if(bank.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.Center) {
                    Text("题库读取失败",fontSize=24.sp)
                    Text("请确认 questions.json 已打包进入应用。")
                }
            }
        }
        if(exitConfirm) {
            KidExitDialog(isExitApp=screen==HOME,
                onStay={exitConfirm=false},
                onConfirm={
                    exitConfirm=false
                    // KidExitDialog's own KidPill already played the tap haptic.
                    if(screen==HOME) activity?.finish() else home()
                })
        }
        if(noQuestions) {
            Dialog(onDismissRequest={noQuestions=false}) {
                StoryCard(Modifier.fillMaxWidth(),PaleMint) {
                    Text("📭 还没有匹配的题目",fontSize=22.sp,
                        fontWeight=FontWeight.Black,color=Ink)
                    Text("当前年级、领域或主题的组合没有题目，可以回到设置里放宽范围。",
                        color=Ink)
                    LargeAction("⚙️ 前往设置",{
                        noQuestions=false;screen=SETTINGS
                    },Modifier.fillMaxWidth())
                    KidPill("先留在首页",{noQuestions=false},Modifier.fillMaxWidth())
                }
            }
        }
    }
}
