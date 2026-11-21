package com.game.knowledge

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val HUB="hub"
private const val WORDS="words"
private const val LISTEN="listen"
private const val DIMSUM="dimsum"
private const val SPEAK="speak"
private const val BRIDGE="bridge"
private const val TREASURE="treasure"
private const val DIALOGUE="dialogue"

/**
 * Opt-in, game-first Cantonese learning: no forced voice scoring, no automatic
 * network uploads, no upload of children's speech, and no Mandarin TTS fallback.
 * No external website shortcuts or bundled example WAV files.
 */
@Composable
fun CantonesePlayground(onBack:()->Unit,onNarrating:(Boolean)->Unit) {
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("cantonese_learning",Context.MODE_PRIVATE)}
    val all=remember {runCatching {CantoneseBank.load(context)}.getOrElse {emptyList()}}
    val words=remember(all) {all.filter {it.kind=="word"}}
    val expressions=remember(all) {all.filter {it.kind=="phrase"}}
    val voice=remember {CantoneseSpeaker(context.applicationContext)}
    val recorder=remember {CantoneseVoiceRecorder(context.applicationContext)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var page by rememberSaveable { mutableStateOf(HUB) }
    var showJyutping by rememberSaveable { mutableStateOf(prefs.getBoolean("jyutping",false)) }
    var traditional by rememberSaveable { mutableStateOf(prefs.getBoolean("traditional",false)) }
    var slow by rememberSaveable { mutableStateOf(prefs.getBoolean("slow",false)) }
    var favorites by remember {mutableStateOf(prefs.getStringSet("known",emptySet())?.toSet() ?: emptySet())}
    var completed by remember {mutableIntStateOf(prefs.getInt("rounds",0))}
    var sampleRevision by remember {mutableIntStateOf(0)}
    var recordingKey by remember {mutableStateOf<String?>(null)}
    var recordingPractice by remember {mutableStateOf(false)}
    var notice by remember {mutableStateOf("")}
    var pending by remember {mutableStateOf<Pair<CantoneseWord,Boolean>?>(null)}

    fun finishRecording(save:Boolean=true) {
        val saved=recorder.stop(save)
        recordingKey=null
        sampleRevision++
        notice=if(saved!=null) "✅ 录音保存在本机，可以马上试听"
            else if(save) "录音太短或失败了，再试一次吧"
            else "录音已取消"
    }
    fun beginRecording(word:CantoneseWord,practice:Boolean) {
        voice.stop()
        val file=if(practice) recorder.practiceFile() else voice.sampleFile(word.id)
        val key=if(practice) "practice" else word.id
        val result=recorder.start(key,file)
        recordingKey=if(result) key else null
        recordingPractice=practice
        notice=if(result) "🎙️ 正在录音，讲完后点击停止保存"
            else "无法启动录音，请检查麦克风或关闭其他录音应用"
    }
    val permission=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) {granted->
        val request=pending
        pending=null
        if(granted && request!=null) beginRecording(request.first,request.second)
        else if(!granted) notice="未授权录音。其他学习和听音功能照常可用。"
    }
    fun requestRecord(word:CantoneseWord,practice:Boolean) {
        if(recordingKey!=null) return
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)
            ==PackageManager.PERMISSION_GRANTED) beginRecording(word,practice)
        else {
            pending=word to practice
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    DisposableEffect(voice,recorder,lifecycle) {
        voice.onSpeakingChanged=onNarrating
        val observer=LifecycleEventObserver {_,event->
            if(event==Lifecycle.Event.ON_STOP) {
                voice.stop()
                if(recorder.isRecording()) finishRecording(save=false)
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            voice.close()
            recorder.close()
            onNarrating(false)
        }
    }
    BackHandler(enabled=page!=HUB) {
        if(recordingKey!=null) finishRecording(save=false)
        voice.stop()
        page=HUB
    }

    fun open(next:String) {
        if(recordingKey!=null) finishRecording(save=false)
        voice.stop()
        notice=""
        page=next
    }
    fun rememberWord(word:CantoneseWord) {
        favorites=favorites+word.id
        prefs.edit().putStringSet("known",favorites).apply()
    }
    fun finishRound() {
        completed++
        prefs.edit().putInt("rounds",completed).apply()
    }

    // Keep page header and next action on-screen for landscape quizzes.
    // Hub, lessons, speaking exercises and phones retain normal scrolling.
    CompositionLocalProvider(LocalShowPinyin provides false) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val tabletQuiz=ResponsiveWindowPolicy.tabletLandscape(maxWidth.value,maxHeight.value) &&
                page in setOf(LISTEN,DIMSUM,BRIDGE,TREASURE,DIALOGUE)
            Column(Modifier.fillMaxSize()
                .then(if(tabletQuiz) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(horizontal=16.dp,vertical=12.dp),
                verticalArrangement=Arrangement.spacedBy(if(tabletQuiz) 8.dp else 13.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                KidPill("← 返回",{
                    if(page==HUB) onBack() else open(HUB)
                })
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("🦁 粤语乐园",fontSize=26.sp,fontWeight=FontWeight.Black,color=Ink)
                    Text("听得明 · 讲得出 · 玩得开心",
                        fontSize=13.sp,color=Sea,fontWeight=FontWeight.Bold)
                }
            }
            Column(Modifier.fillMaxWidth()
                .then(if(tabletQuiz) Modifier.weight(1f) else Modifier),
                verticalArrangement=Arrangement.spacedBy(if(page==HUB) 14.dp else 0.dp)) {
            when(page) {
                HUB -> {
                    StoryCard(Modifier.fillMaxWidth(),Color(0xFFFFF2D5)) {
                        Text("🌟 今日想玩啲咩？",fontWeight=FontWeight.Black,
                            fontSize=22.sp,color=Ink)
                        Text("先听、先玩，不用考试！普通话和粤语各有自己的读法。",
                            color=Ink,fontSize=14.sp)
                        Text("已认识 ${favorites.size}/${all.size} 个表达 · 完成 ${completed} 轮小游戏",
                            color=Sea,fontSize=14.sp,fontWeight=FontWeight.Bold)
                    }
                    CantoModeCard("🍎","粤语词语宝典","看图片、听读音、学生活中的词语",
                        Color(0xFFE5F8EC)) {open(WORDS)}
                    CantoModeCard("👂","粤语小耳朵","听一个粤语词，找出正确的图片",
                        Color(0xFFE0F1FF)) {open(LISTEN)}
                    CantoModeCard("🥟","点心小掌柜","客人想吃什么？帮茶楼点菜",
                        Color(0xFFFFE9D9)) {open(DIMSUM)}
                    CantoModeCard("🎤","跟我讲粤语","听示范、自己录音回放，想讲才讲",
                        Color(0xFFF5E8FF)) {open(SPEAK)}
                    Text("✨ 更多趣味闯关",fontSize=20.sp,
                        fontWeight=FontWeight.Black,color=Ink)
                    CantoModeCard("🧩","粤普变变变","从普通话的意思，找到对应的粤语词",
                        Color(0xFFE4F6EC)) {open(BRIDGE)}
                    CantoModeCard("🗺️","生活寻宝","听提示，在广东生活场景中找物品",
                        Color(0xFFFFF1D8)) {open(TREASURE)}
                    CantoModeCard("💬","对话小剧场","遇到不同情况，挑出合适的粤语短句",
                        Color(0xFFEEE8FF)) {open(DIALOGUE)}
                    StoryCard(Modifier.fillMaxWidth(),Color.White) {
                        Text("🔤 学习小设置",fontSize=18.sp,
                            fontWeight=FontWeight.Black,color=Ink)
                        CantoneseHttpTtsPanel(voice)
                        CantoneseSystemTtsPanel(voice)
                        CantoVoiceVolume(
                            current=voice.volumePercent,
                            onChange=voice::updateVolumePercent,
                            onPreview={
                                all.firstOrNull {it.id=="phrase_hello"}?.let {
                                    voice.speak(it,slow)
                                }
                            })
                        CantoToggle("显示粤拼（数字 1～6 是粤语声调）",showJyutping) {
                            showJyutping=it;prefs.edit().putBoolean("jyutping",it).apply()
                        }
                        CantoToggle("显示繁体字",traditional) {
                            traditional=it;prefs.edit().putBoolean("traditional",it).apply()
                        }
                        CantoToggle("慢速粤语播放",slow) {
                            slow=it;prefs.edit().putBoolean("slow",it).apply()
                        }
                        Text("发音状态：${voice.message}",color=Ink,
                            fontSize=13.sp,lineHeight=19.sp)
                        Text("粤语示范音优先播放家长录音；选择在线音色后优先联网生成，关闭则使用系统 TTS。",
                            fontSize=12.sp,color=Ink.copy(alpha=.7f))
                    }
                }
                WORDS -> CantoLibrary(words,showJyutping,traditional,slow,voice,
                    favorites,::rememberWord,
                    sampleRevision,onRecord={requestRecord(it,false)},
                    recordingKey=recordingKey,
                    onStopRecord={finishRecording()},
                    onNotice={notice=it})
                LISTEN -> {
                    val playable=remember(words,voice.ready,sampleRevision) {
                        words.filter {voice.hasAudio(it)}
                    }
                    if(playable.size<4) {
                        StoryCard(Modifier.fillMaxWidth(),Color(0xFFFFF0DE)) {
                            Text("🔊 先准备粤语声音",fontSize=22.sp,
                                color=Ink,fontWeight=FontWeight.Black)
                            Text("听音找图至少需要 4 个可用的粤语音频，目前有 ${playable.size} 个。请返回粤语乐园选择粤语在线或系统 TTS 引擎，或录制家长示范音。",
                                color=Ink)
                            LargeAction("🍎 去词语宝典", {open(WORDS)},Modifier.fillMaxWidth())
                        }
                    } else CantoQuiz(
                        mode=LISTEN,all=words,playable=playable,voice=voice,
                        slow=slow,showJyutping=showJyutping,traditional=traditional,
                        onRecognized=::rememberWord,onCompleted=::finishRound,
                    tabletLandscape=tabletQuiz)
                }
                DIMSUM -> {
                    val food=remember(words) {words.filter {it.category=="点心茶楼"}}
                    CantoQuiz(
                        mode=DIMSUM,all=food,playable=food,voice=voice,
                        slow=slow,showJyutping=showJyutping,traditional=traditional,
                        onRecognized=::rememberWord,onCompleted=::finishRound,
                    tabletLandscape=tabletQuiz)
                }
                BRIDGE -> CantoneseChallengeScreen(
                    mode=CantoneseChallengeMode.BRIDGE,words=all,
                    showJyutping=showJyutping,traditional=traditional,slow=slow,
                    voice=voice,onRecognized=::rememberWord,onCompleted=::finishRound,
                    tabletLandscape=tabletQuiz)
                TREASURE -> CantoneseChallengeScreen(
                    mode=CantoneseChallengeMode.TREASURE,words=all,
                    showJyutping=showJyutping,traditional=traditional,slow=slow,
                    voice=voice,onRecognized=::rememberWord,onCompleted=::finishRound,
                    tabletLandscape=tabletQuiz)
                DIALOGUE -> CantoneseChallengeScreen(
                    mode=CantoneseChallengeMode.DIALOGUE,words=all,
                    showJyutping=showJyutping,traditional=traditional,slow=slow,
                    voice=voice,onRecognized=::rememberWord,onCompleted=::finishRound,
                    tabletLandscape=tabletQuiz)
                SPEAK -> CantoSpeakScreen(
                    entries=all,showJyutping=showJyutping,traditional=traditional,
                    slow=slow,voice=voice,recorder=recorder,
                    recordingKey=recordingKey,recordingPractice=recordingPractice,
                    onRequestRecord=::requestRecord,onStopRecord={finishRecording()},
                    onDeleteSample={
                        voice.stop()
                        voice.sampleFile(it.id).delete()
                        sampleRevision++
                        notice="已删除这条本地示范音"
                    })
            }
            } // quiz body fills remaining height only on tablets
            if(notice.isNotBlank()) {
                Text(notice,color=Ink,fontSize=14.sp,
                    fontWeight=FontWeight.SemiBold)
            }
            if(!tabletQuiz) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CantoModeCard(emoji:String,title:String,subtitle:String,
                          tint:Color,onClick:()->Unit) {
    val feedback=LocalKidFeedback.current
    StoryCard(Modifier.fillMaxWidth().clickable {feedback?.tap();onClick()},tint) {
        Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(emoji,fontSize=39.sp)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(title,fontSize=20.sp,fontWeight=FontWeight.Black,color=Ink)
                Text(subtitle,fontSize=13.sp,color=Ink)
            }
            Text("›",fontSize=28.sp,color=Sea)
        }
    }
}

/** This slider affects Cantonese lessons, recorded samples and preview only. */
@Composable
private fun CantoVoiceVolume(current:Int,onChange:(Int)->Unit,onPreview:()->Unit) {
    val feedback=LocalKidFeedback.current
    val percent=CantoneseVolumePolicy.clamp(current)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
        .background(Color(0xFFE7F5EF)).padding(horizontal=13.dp,vertical=11.dp),
        verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("🔊 粤语语音音量",fontSize=17.sp,fontWeight=FontWeight.Black,
                color=Ink,modifier=Modifier.weight(1f))
            Text("${percent}%",fontSize=17.sp,fontWeight=FontWeight.Black,color=Sea)
        }
        Slider(value=percent.toFloat(),
            onValueChange={onChange(CantoneseVolumePolicy.step(it))},
            valueRange=CantoneseVolumePolicy.MIN_PERCENT.toFloat()..
                CantoneseVolumePolicy.MAX_PERCENT.toFloat(),
            steps=39,
            onValueChangeFinished={feedback?.tap()},
            modifier=Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("静音 0%",color=Ink.copy(alpha=.7f),fontSize=12.sp)
            Text("原音量 100%",color=Ink.copy(alpha=.7f),fontSize=12.sp)
            Text("增强 200%",color=Ink.copy(alpha=.7f),fontSize=12.sp)
        }
        Text(if(percent>100) "已开启粤语录音增强；增强可能受设备限制，声音过大可随时调低。"
            else if(percent==0) "粤语语音已静音，不影响游戏音效。"
            else "仅调整粤语发音，不影响普通话朗读、背景音乐和答题音效。",
            color=Ink.copy(alpha=.75f),fontSize=12.sp)
        KidPill("▶ 试听当前音量 · 你好",{
            feedback?.tap()
            onPreview()
        },Modifier.fillMaxWidth())
    }
}

@Composable
private fun CantoToggle(label:String,checked:Boolean,onChange:(Boolean)->Unit) {
    val feedback=LocalKidFeedback.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(Color(0xFFF4F9F6)).clickable {feedback?.tap();onChange(!checked)}
        .padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),color=Ink,fontSize=14.sp)
        Text(if(checked) "🟢 开" else "⚪ 关",
            color=Ink,fontWeight=FontWeight.Bold,fontSize=13.sp)
    }
}

@Composable
private fun CantoReading(word:CantoneseWord,traditional:Boolean,
                         showJyutping:Boolean,large:Boolean=false) {
    Text(word.emoji,fontSize=if(large) 62.sp else 32.sp,textAlign=TextAlign.Center)
    Text(if(traditional) word.traditional else word.hanzi,
        color=Ink,fontSize=if(large) 30.sp else 23.sp,
        fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
    if(showJyutping) Text(word.jyutping,color=Sea,fontSize=if(large) 20.sp else 15.sp,
        fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
    Text(word.meaning,color=Ink,fontSize=if(large) 17.sp else 14.sp,
        textAlign=TextAlign.Center)
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CantoLibrary(words:List<CantoneseWord>,showJyutping:Boolean,
                         traditional:Boolean,slow:Boolean,voice:CantoneseSpeaker,
                         known:Set<String>,onRecognized:(CantoneseWord)->Unit,
                         sampleRevision:Int,onRecord:(CantoneseWord)->Unit,
                         recordingKey:String?,onStopRecord:()->Unit,
                         onNotice:(String)->Unit) {
    var category by remember {mutableStateOf(CantoneseBank.categories.first())}
    var index by remember(category) {mutableIntStateOf(0)}
    val group=words.filter {it.category==category}
    val word=group.getOrNull(index) ?: return
    StoryCard(Modifier.fillMaxWidth(),Color.White) {
        Text("📚 词语宝典 · ${index+1}/${group.size}",color=Sea,
            fontSize=18.sp,fontWeight=FontWeight.Black)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement=Arrangement.spacedBy(6.dp),
            verticalArrangement=Arrangement.spacedBy(5.dp)) {
            CantoneseBank.categories.forEach { name ->
                SelectPill(name,name==category){category=name}
            }
        }
        Column(Modifier.fillMaxWidth().padding(vertical=9.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(7.dp)) {
            CantoReading(word,traditional,showJyutping,large=true)
            Text(word.tip,color=Ink.copy(alpha=.76f),fontSize=13.sp,
                textAlign=TextAlign.Center)
        }
        if(voice.hasAudio(word))
            LargeAction("🔊 听粤语 ${if(slow) "（慢速）" else ""}",
                {voice.speak(word,slow)},Modifier.fillMaxWidth())
        else Text("🔇 请先选择粤语在线音色或系统 TTS 引擎，或由家长录制示范音。",
            color=Color(0xFF9A532C),fontSize=13.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            KidPill("← 上一个",{index=(index-1+group.size)%group.size},Modifier.weight(1f))
            KidPill(if(word.id in known) "✅ 已认识" else "🌟 我认识了",
                {onRecognized(word)},Modifier.weight(1f))
            KidPill("下一个 →",{index=(index+1)%group.size},Modifier.weight(1f))
        }
        if(recordingKey==word.id) {
            LargeAction("⏹️ 停止并保存家长示范音",onStopRecord,Modifier.fillMaxWidth(),
                color=Coral)
        } else {
            KidPill(
                if(voice.hasSample(word)) "🎙️ 重录家长示范音" else "🎙️ 家长录粤语示范音",
                {onRecord(word)},Modifier.fillMaxWidth())
        }
        if(voice.hasSample(word)) {
            Text("当前使用家长示范音；删除后将恢复系统 TTS 朗读。",
                color=Sea,fontSize=12.sp)
        } else if(voice.system.ready) {
            Text("✅ 系统 TTS 已就绪，请试听确认粤语发音。",
                color=Sea,fontSize=12.sp)
        }
        Text("可选的家长录音只保存在本机，不上传网络；录制前请确认发音正确。",
            color=Ink.copy(alpha=.6f),fontSize=12.sp)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CantoQuiz(mode:String,all:List<CantoneseWord>,playable:List<CantoneseWord>,
                      voice:CantoneseSpeaker,slow:Boolean,showJyutping:Boolean,
                      traditional:Boolean,onRecognized:(CantoneseWord)->Unit,
                      onCompleted:()->Unit,tabletLandscape:Boolean=false) {
    val round=remember(playable,mode) {CantoneseRules.newRound(playable,5)}
    var index by remember(round) {mutableIntStateOf(0)}
    var answered by remember(index) {mutableStateOf<String?>(null)}
    var wrong by remember(index) {mutableIntStateOf(0)}
    var score by remember(round) {mutableIntStateOf(0)}
    var ended by remember(round) {mutableStateOf(false)}
    val finished=index>=round.size
    LaunchedEffect(index,voice.ready,round) {
        if(!finished && !ended) {
            val item=round[index]
            if(voice.hasAudio(item)) {
                voice.speak(item,slow,
                    if(mode==DIMSUM && !voice.hasSample(item))
                        "唔該，我想食${item.traditional}" else null)
            }
        }
    }
    if(finished) {
        StoryCard(if(tabletLandscape) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
            Color(0xFFE4F7E7)) {
            Text("🎉 真叻！完成啦！",fontSize=25.sp,color=Ink,
                fontWeight=FontWeight.Black)
            Text("这轮认识了 ${round.size} 个词 · 第一次就选对了 ${score} 题",
                color=Ink,fontSize=17.sp)
            Text("答错也没关系，喜欢才会慢慢学会！",color=Sea)
            LargeAction("🔁 再玩一轮",{
                index=0;score=0;answered=null;ended=false
            },Modifier.fillMaxWidth())
        }
        return
    }
    val target=round[index]
    val options=remember(round,index,all) {CantoneseRules.options(all,target)}
    StoryCard(if(tabletLandscape) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
        if(mode==DIMSUM) Color(0xFFFFF0DC) else Color(0xFFE8F5FF)) {
        Column(Modifier.fillMaxWidth().then(if(tabletLandscape)
            Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(if(mode==DIMSUM) "🥟 点心小掌柜" else "👂 粤语小耳朵",
            fontSize=22.sp,fontWeight=FontWeight.Black,color=Ink)
        Text("第 ${index+1}/${round.size} 题",
            color=Sea,fontWeight=FontWeight.Bold)
        Text(if(mode==DIMSUM) "客人来点单啦！找找客人想吃的点心。"
            else "仔细听，看看说的是哪一样东西。",
            color=Ink,fontSize=16.sp)
        if(mode==DIMSUM) {
            Text("顾客：唔该，我想食「${if(traditional) target.traditional else target.hanzi}」",
                fontSize=18.sp,fontWeight=FontWeight.Bold,color=Ink,
                textAlign=TextAlign.Center)
        }
        if(voice.hasAudio(target))
            LargeAction("🔊 再听一次粤语",{voice.speak(target,slow,
                if(mode==DIMSUM && !voice.hasSample(target))
                    "唔該，我想食${target.traditional}" else null)},
                Modifier.fillMaxWidth())
        else Text("请先选择粤语在线音色或系统 TTS 引擎，再试听粤语。",
            color=Color(0xFF9A532C),fontSize=13.sp)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement=Arrangement.spacedBy(8.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val answeredCorrect=answered==target.id
                val isChosen=answered==option.id
                val tint=if(answeredCorrect && option.id==target.id)
                    Color(0xFFB6E6B8) else if(isChosen && !answeredCorrect)
                    Color(0xFFFFDAC8) else Color.White
                val feedback=LocalKidFeedback.current
                Column(Modifier.widthIn(min=125.dp).weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(tint)
                    .clickable(enabled=!answeredCorrect) {
                        // Stop speech before success/error: otherwise the audio
                        // mixer suppresses feedback while TTS is still playing.
                        voice.stop()
                        if(option.id==target.id) {
                            if(answered==null && wrong==0) score++
                            answered=target.id
                            onRecognized(target)
                            feedback?.success()
                        } else {
                            wrong++
                            answered=option.id
                            feedback?.error()
                        }
                    }
                    .padding(12.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(option.emoji,fontSize=45.sp)
                    Text(if(traditional) option.traditional else option.hanzi,
                        fontSize=18.sp,fontWeight=FontWeight.Black,color=Ink,
                        textAlign=TextAlign.Center)
                    if(showJyutping) Text(option.jyutping,fontSize=12.sp,
                        color=Sea,textAlign=TextAlign.Center)
                }
            }
        }
        if(answered==target.id) {
            Text("✅ 答对了！${target.hanzi} · ${target.meaning}",
                fontSize=17.sp,fontWeight=FontWeight.Black,color=Ink)

        } else if(answered!=null) {
            Text("🌱 差一点，再听一次，再选选看！",
                color=Ink,fontSize=15.sp)
        } else {
            Text("不会也没关系，可以反复听，也可以和家人一起猜。",
                color=Ink.copy(alpha=.7f),fontSize=13.sp)
        }
        } // all question content stays inside the bounded scroll area
        if(answered==target.id) {
            LargeAction(if(index==round.lastIndex) "🏅 完成这一轮" else "➡️ 下一题",{
                if(index==round.lastIndex && !ended) {
                    ended=true
                    onCompleted()
                }
                index++
            },Modifier.fillMaxWidth())
        }
    }
}
@Composable
private fun CantoSpeakScreen(
    entries:List<CantoneseWord>,showJyutping:Boolean,traditional:Boolean,slow:Boolean,
    voice:CantoneseSpeaker,recorder:CantoneseVoiceRecorder,
    recordingKey:String?,recordingPractice:Boolean,
    onRequestRecord:(CantoneseWord,Boolean)->Unit,
    onStopRecord:()->Unit,onDeleteSample:(CantoneseWord)->Unit
) {
    var kind by remember {mutableStateOf("word")}
    var index by remember(kind) {mutableIntStateOf(0)}
    val choices=entries.filter {it.kind==kind}
    val item=choices.getOrNull(index) ?: return
    val practice=recorder.practiceFile()
    // A recording belongs only to the card it was made for.
    LaunchedEffect(kind,index) {
        voice.stop()
        if(recordingKey==null) practice.delete()
    }
    StoryCard(Modifier.fillMaxWidth(),Color(0xFFF1EAFB)) {
        Text("🎤 跟我讲粤语",color=Ink,fontSize=23.sp,fontWeight=FontWeight.Black)
        Text("想开口就模仿；不想讲也可以只听。这里不给发音打分。",
            color=Ink,fontSize=14.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            SelectPill("👋 学词语",kind=="word"){kind="word"}
            SelectPill("💬 生活短句",kind=="phrase"){kind="phrase"}
        }
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            CantoReading(item,traditional,showJyutping,large=true)
            Text(item.tip,color=Ink.copy(alpha=.7f),fontSize=13.sp)
        }
        if(voice.hasAudio(item))
            LargeAction("🔊 第一步：听示范音",{
                voice.speak(item,slow)
            },Modifier.fillMaxWidth())
        else Text("请先选择粤语在线音色或系统 TTS 引擎，也可先录制家长示范音。",
            color=Color(0xFF934A2D),fontSize=14.sp)
        if(recordingKey=="practice") {
            LargeAction("⏹️ 停止我的录音",onStopRecord,Modifier.fillMaxWidth(),color=Coral)
        } else {
            LargeAction("🎙️ 第二步：我也试着讲",{
                onRequestRecord(item,true)
            },Modifier.fillMaxWidth(),color=Sea,enabled=recordingKey==null)
        }
        if(practice.isFile && recordingKey==null) {
            KidPill("▶ 听听刚才自己讲的",{
                voice.playFile(practice)
            },Modifier.fillMaxWidth())
        }
        if(recordingKey==item.id) {
            KidPill("⏹️ 家长示范音录完啦",onStopRecord,Modifier.fillMaxWidth())
        } else {
            KidPill("👨‍👩‍👧 家长示范录音",{
                onRequestRecord(item,false)
            },Modifier.fillMaxWidth())
        }
        if(voice.hasSample(item)) {
            KidPill("🗑️ 删除这条家长示范音",{onDeleteSample(item)},
                Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            KidPill("← 上一个",{index=(index-1+choices.size)%choices.size},
                Modifier.weight(1f))
            KidPill("下一个 →",{index=(index+1)%choices.size},Modifier.weight(1f))
        }
        Text("麦克风仅在你点击录音时申请权限。示范音留在应用私有目录；自己的练习录音属于临时文件，退出粤语专区后删除。没有云端上传或自动发音评分。",
            color=Ink.copy(alpha=.7f),fontSize=12.sp)
    }
}
