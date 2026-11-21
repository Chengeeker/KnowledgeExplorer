package com.game.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CantoneseSystemTtsPanel(voice: CantoneseSpeaker) {
    val engine = voice.system
    var enginesOpen by remember { mutableStateOf(false) }
    var voicesOpen by remember { mutableStateOf(false) }
    val label = engine.engines.firstOrNull { it.name == engine.enginePackage }?.label
        ?: if (engine.enginePackage.isBlank()) "系统默认" else engine.enginePackage
    val chosenVoice = engine.voices.firstOrNull { it.name == engine.voiceName }

    Column(Modifier.fillMaxWidth().background(Color(0xFFE7F4ED),
        RoundedCornerShape(16.dp)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("🔊 手机系统朗读引擎", fontWeight = FontWeight.Black,
            fontSize = 18.sp, color = Ink)
        Text("使用手机已经安装的语音引擎，例如讯飞、搜狗和谷歌；不再需要下载大模型或连接电脑。",
            fontSize = 13.sp, color = Ink)

        OutlinedButton(onClick = { enginesOpen = true }, modifier = Modifier.fillMaxWidth()) {
            Text("引擎：" + label + "  ▾")
        }
        DropdownMenu(expanded = enginesOpen, onDismissRequest = { enginesOpen = false }) {
            DropdownMenuItem(text = { Text("系统默认") }, onClick = {
                engine.chooseEngine("")
                enginesOpen = false
            })
            engine.engines.forEach { item ->
                DropdownMenuItem(text = { Text(item.label) }, onClick = {
                    engine.chooseEngine(item.name)
                    enginesOpen = false
                })
            }
        }

        OutlinedButton(onClick = { voicesOpen = true }, modifier = Modifier.fillMaxWidth(),
            enabled = engine.voices.isNotEmpty()) {
            Text("音色：" + (chosenVoice?.let { it.name + " (" + it.locale + ")" }
                ?: "自动选择粤语") + "  ▾")
        }
        DropdownMenu(expanded = voicesOpen, onDismissRequest = { voicesOpen = false }) {
            engine.voices.forEach { item ->
                DropdownMenuItem(text = {
                    Text(item.name + " · " + item.locale +
                        if (item.network) " · 需联网" else "")
                }, onClick = {
                    engine.chooseVoice(item.name)
                    voicesOpen = false
                })
            }
        }

        Text(engine.status, fontSize = 13.sp, color = Sea)
        Button(onClick = { engine.speak("你好，欢迎嚟到粤语乐园。", false, voice.volumePercent) },
            modifier = Modifier.fillMaxWidth(), enabled = engine.ready) {
            Text("🔊 试听当前发音")
        }
        OutlinedButton(onClick = { engine.refresh() }, modifier = Modifier.fillMaxWidth()) {
            Text("刷新手机已安装的引擎")
        }
        Text("只有支持粤语的音色才能保证粤语发音；其他音色可能读成普通话。部分引擎需要联网。家长示范录音始终优先；系统 TTS 音量超过 100% 可能不生效。",
            fontSize = 12.sp, color = Ink.copy(alpha = .74f))
    }
}
