package com.game.knowledge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow

/** All modes read the same saved question scope; no mode-specific setup screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KnowledgeSettings(
    all: List<KnowledgeQuestion>, filter: QuestionFilter, count: Int,
    withAi: Boolean, duelMode: String, aiLevel: Int,
    autoRead: Boolean, haptics: Boolean, pcmVoice:Boolean,
    musicEnabled: Boolean, musicVolume: Int, soundEffects: Boolean,
    voiceReady: Boolean, voiceStatus: String,
    onFilter: (QuestionFilter) -> Unit, onCount: (Int) -> Unit,
    onWithAi: (Boolean) -> Unit, onDuelMode: (String) -> Unit, onAiLevel: (Int) -> Unit,
    onAutoRead: (Boolean) -> Unit, onHaptics: (Boolean) -> Unit,
    onPcmVoice:(Boolean)->Unit,
    onMusicEnabled:(Boolean)->Unit,onMusicVolume:(Int)->Unit,onSoundEffects:(Boolean)->Unit,
    onVoicePreview: () -> Unit, onBack: () -> Unit
) {
    val available = KnowledgeBank.matching(all, filter).size
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = maxWidth >= 740.dp && maxWidth > maxHeight
        Column(Modifier.fillMaxSize().padding(horizontal=if(wide) 23.dp else 15.dp,vertical=13.dp)) {
            TopLine("⚙️ 探险设置",onBack)
            Spacer(Modifier.height(10.dp))
            if (wide) {
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1.1f).verticalScroll(rememberScrollState())) {
                        QuestionSettings(all,filter,count,available,onFilter,onCount)
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        DuelSettings(withAi,duelMode,aiLevel,onWithAi,onDuelMode,onAiLevel)
                        Spacer(Modifier.height(12.dp))
                        InteractionSettings(autoRead,haptics,pcmVoice,musicEnabled,musicVolume,soundEffects,
                            voiceReady,voiceStatus,onAutoRead,onHaptics,onPcmVoice,
                            onMusicEnabled,onMusicVolume,onSoundEffects,onVoicePreview)
                    }
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    QuestionSettings(all,filter,count,available,onFilter,onCount)
                    DuelSettings(withAi,duelMode,aiLevel,onWithAi,onDuelMode,onAiLevel)
                    InteractionSettings(autoRead,haptics,pcmVoice,musicEnabled,musicVolume,soundEffects,
                        voiceReady,voiceStatus,onAutoRead,onHaptics,onPcmVoice,
                        onMusicEnabled,onMusicVolume,onSoundEffects,onVoicePreview)
                }
            }
            Spacer(Modifier.height(10.dp))
            LargeAction("✅ 保存并返回首页",onBack,Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionSettings(
    all:List<KnowledgeQuestion>,filter:QuestionFilter,count:Int,available:Int,
    onFilter:(QuestionFilter)->Unit,onCount:(Int)->Unit
) {
    val domains = QuestionFilterChoices.domains(all,filter)
    val topics = QuestionFilterChoices.topics(all,filter)
    val totalPerDomain = all.filter { filter.grade == -1 || it.grade == filter.grade }
        .groupingBy {it.domain}.eachCount()
    StoryCard(Modifier.fillMaxWidth(),PaleMint) {
        TitleLine("🎒 通用题库范围","科普、闯关、知识对决共用此处设置；选好后从首页直接开始。")
        TitleLine("🎓 学习年级","题库按年级组织；不再重复设置与年级高度重合的星级难度。")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            (listOf(-1)+(1..6).toList()).forEach { g ->
                SelectPill(if(g==-1) "全部年级" else "${g}年级",filter.grade==g) {
                    onFilter(filter.copy(grade=g,domain="全部",topic="全部"))
                }
            }
        }
        TitleLine("🌍 知识领域",
            "仅显示当前年级实际有题目的领域；括号是该年级可用题量。")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            (listOf("全部")+domains).forEach { d ->
                val title=if(d=="全部") "全部" else "$d（${totalPerDomain[d] ?: 0}）"
                SelectPill(title,filter.domain==d) {
                    onFilter(QuestionFilterChoices.selectDomain(all,filter,d))
                }
            }
        }
        Text("各年级题目独立筛选，不会自动切换年级或混入高年级题。",
            fontSize=12.sp,color=Ink.copy(alpha=.8f))
        TitleLine("🔖 细分主题",
            if(filter.domain=="全部") "先选择知识领域，可以更容易找到对应细分主题。"
            else "当前领域：${filter.domain} · 共 ${topics.size} 个可选主题")
        if(filter.domain=="全部") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                (listOf("全部")+topics).forEach { t ->
                    SelectPill(t,filter.topic==t) {
                        onFilter(QuestionFilterChoices.selectTopic(all,filter,t))
                    }
                }
            }
        } else {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                (listOf("全部")+topics).forEach { t ->
                    SelectPill(t,filter.topic==t) {
                        onFilter(QuestionFilterChoices.selectTopic(all,filter,t))
                    }
                }
            }
        }
        TitleLine("📂 题库原始题型","这是题目来源分类，不会改变当前四选一的作答方式。")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            (listOf("全部")+all.map{it.kind}.distinct().sorted()).forEach { k ->
                SelectPill(k,filter.kind==k) {onFilter(filter.copy(kind=k))}
            }
        }
        TitleLine("🎲 每轮题数")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf(5,10,20,30).forEach { n ->
                SelectPill("${n}题",count==n) {onCount(n)}
            }
        }
        Text(
            if(available>0) "当前有 ${available} 道匹配题目，每局随机抽取 ${minOf(available,count)} 道且不重复。"
            else "⚠️ 当前组合没有匹配题目，请放宽领域、主题或原始题型。",
            color=if(available>0) Ink else Color(0xFFA74634),fontSize=14.sp,
            fontWeight=FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuelSettings(
    withAi:Boolean,duelMode:String,aiLevel:Int,
    onWithAi:(Boolean)->Unit,onDuelMode:(String)->Unit,onAiLevel:(Int)->Unit
) {
    StoryCard(Modifier.fillMaxWidth(),SoftBlue) {
        TitleLine("⚔️ 对决专属设置","只影响「知识对决」，不会改变科普与闯关。")
        TitleLine("🎮 对手")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            SelectPill("👫 双人同屏",!withAi) {onWithAi(false)}
            SelectPill("🤖 与 AI 对战",withAi) {onWithAi(true)}
        }
        TitleLine("🏁 比赛规则")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf("速度优先","正确率优先").forEach { mode ->
                SelectPill(mode,duelMode==mode) {onDuelMode(mode)}
            }
        }
        Text(if(duelMode=="速度优先")
            "同一道题抢答；先答对的一方得分。"
            else "分别完成同一组题；答对数量优先，同分比较总用时。",
            color=Ink,fontSize=13.sp)
        if(withAi) {
            TitleLine("🤖 AI 对手强度","调整 AI 的答题反应时间，不代表题目更难。")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                listOf("简单","中等","困难","噩梦").forEachIndexed {i,s->
                    SelectPill(s,aiLevel==i) {onAiLevel(i)}
                }
            }
        } else Text("双人对决将切换横屏，孩子分别坐在平板两端。",
            fontSize=13.sp,color=Ink)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun InteractionSettings(
    autoRead:Boolean,haptics:Boolean,pcmVoice:Boolean,
    musicEnabled:Boolean,musicVolume:Int,soundEffects:Boolean,
    voiceReady:Boolean,voiceStatus:String,
    onAutoRead:(Boolean)->Unit,onHaptics:(Boolean)->Unit,
    onPcmVoice:(Boolean)->Unit,onMusicEnabled:(Boolean)->Unit,onMusicVolume:(Int)->Unit,onSoundEffects:(Boolean)->Unit,
    onVoicePreview:()->Unit
) {
    val feedback=LocalKidFeedback.current
    StoryCard(Modifier.fillMaxWidth(),Color(0xFFFFF4DF)) {
        TitleLine("🎵 声音与触感")
        SettingToggle("🎼 背景音乐","沿用数学星球的原创轻快旋律，朗读时自动降低音量",musicEnabled) {
            feedback?.tap()
            onMusicEnabled(it)
        }
        if(musicEnabled) {
            Text("🎚️ 音乐音量 ${musicVolume}%",color=Ink,fontSize=14.sp,
                fontWeight=FontWeight.Bold)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf(0,25,50,75,100).forEach { volume ->
                    SelectPill("${volume}%",musicVolume==volume) {onMusicVolume(volume)}
                }
            }
        }
        SettingToggle("🎶 互动音效","轻触、答对、答错有不同的短音效",soundEffects) {
            feedback?.tap()
            onSoundEffects(it)
        }
        SettingToggle("📳 触摸震动","轻触、答对与答错使用不同震感",haptics) {
            feedback?.tap()
            onHaptics(it)
        }
        SettingToggle("🔊 自动朗读题目","使用设备支持的中文语音引擎",autoRead) {
            feedback?.tap()
            onAutoRead(it)
        }
        SettingToggle("🎤 PCM 缓冲朗读","平板推荐：先合成语音，再用 48 kHz 缓冲播放；关闭则改用系统直接朗读",pcmVoice) {
            feedback?.tap()
            onPcmVoice(it)
        }
        Text(voiceStatus,color=Ink.copy(alpha=.78f),fontSize=13.sp)
        KidLink(if(voiceReady) "▶ 试听中文语音" else "设备中文语音不可用",
            onVoicePreview,enabled=voiceReady)
    }
}

@Composable
private fun SettingToggle(title:String,desc:String,selected:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha=.72f))
            .clickable { onChange(!selected) }
            .padding(horizontal=13.dp,vertical=12.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title,color=Ink,fontSize=16.sp,fontWeight=FontWeight.Black)
            Text(desc,color=Ink.copy(alpha=.72f),fontSize=12.sp)
        }
        Text(if(selected) "🟢 开" else "⚪ 关",color=Ink,
            fontSize=14.sp,fontWeight=FontWeight.ExtraBold)
    }
}
