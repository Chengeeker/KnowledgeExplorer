package com.game.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CantoneseHttpTtsPanel(speaker:CantoneseSpeaker) {
    val voice=speaker.http
    var preview by remember { mutableStateOf("你好，欢迎嚟到粤语乐园。") }
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFE7F4ED),
            androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(13.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ) {
        Text("🎙️ 在线朗读音色",fontSize=18.sp,fontWeight=FontWeight.Black,color=Ink)
        Text("固定使用 G 讯飞小美（粤语）。",fontSize=13.sp,color=Ink)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("使用在线 TTS",color=Ink,fontWeight=FontWeight.Bold)
                Text("关闭后使用家长录音或手机系统 TTS",fontSize=12.sp,color=Sea)
            }
            Switch(checked=voice.enabled,onCheckedChange=voice::toggle)
        }
        Text("语速调节：${voice.speedDelta}（-30 慢，0 标准，+30 快）",fontSize=13.sp,color=Ink)
        Slider(value=voice.speedDelta.toFloat(),onValueChange={voice.changeSpeed(it.toInt())},
            valueRange=-30f..30f,steps=11,enabled=!voice.busy,modifier=Modifier.fillMaxWidth())
        Text("音调调节：${voice.pitchDelta}（-20～+20）",fontSize=13.sp,color=Ink)
        Slider(value=voice.pitchDelta.toFloat(),onValueChange={voice.changePitch(it.toInt())},
            valueRange=-20f..20f,steps=7,enabled=!voice.busy,modifier=Modifier.fillMaxWidth())
        Text("音量调节：${voice.volumeDelta}（-50～+20）",fontSize=13.sp,color=Ink)
        Slider(value=voice.volumeDelta.toFloat(),onValueChange={voice.changeVolume(it.toInt())},
            valueRange=-50f..20f,steps=13,enabled=!voice.busy,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=preview,onValueChange={preview=it.take(120)},
            label={Text("试听文本")},minLines=1,maxLines=3,modifier=Modifier.fillMaxWidth())
        if(voice.busy) Text("正在请求语音……",fontSize=12.sp,color=Sea)
        Button(onClick={speaker.previewOnline(preview)},
            enabled=voice.enabled && voice.selected!=null && preview.isNotBlank() && !voice.busy,
            modifier=Modifier.fillMaxWidth()) {Text("🔊 试听在线音色")}
        Text(voice.status,fontSize=12.sp,color=Sea)
        if(voice.status.startsWith("合成失败")) {
            val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
            OutlinedButton(onClick={
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(voice.status))
            },modifier=Modifier.fillMaxWidth()) {Text("复制合成失败原因")}
        }
        Text("在线试听会通过 HTTP 把文本发送到在线语音服务，请勿输入个人隐私。",
            fontSize=12.sp,color=Ink.copy(alpha=.72f))
    }
}
