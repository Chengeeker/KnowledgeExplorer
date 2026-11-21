package com.game.knowledge

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
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

/**
 * Legado HTTP TTS rules as editable data. No Rhino JavaScript, keys, or
 * hidden credential harvesting: only explicit HTTP GET/POST request templates.
 */
@Composable
internal fun CantoneseHttpTtsPanel(speaker:CantoneseSpeaker) {
    val voice=speaker.http
    var selectOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var editRule by remember { mutableStateOf<LegadoHttpVoice?>(null) }
    var editOpen by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf("你好，欢迎嚟到粤语乐园。") }
    val picker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ){uri->if(uri!=null)voice.importFile(uri)}
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFE7F4ED),
            androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(13.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ) {
        Text("🎙️ 在线朗读引擎与音色",fontSize=18.sp,fontWeight=FontWeight.Black,color=Ink)
        Text("已收录 "+voice.voices.size+" 条社区 HTTP TTS 音色规则，包含讯飞小美粤语、思必驰、搜狗、百度及谷歌。直接选择试听即可，不需要额外导入。",
            fontSize=13.sp,color=Ink)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("使用在线 TTS",color=Ink,fontWeight=FontWeight.Bold)
                Text("关闭后仅播放家长录音或手机系统 TTS",fontSize=12.sp,color=Sea)
            }
            Switch(checked=voice.enabled,onCheckedChange=voice::toggle)
        }
        OutlinedButton(onClick={selectOpen=true},modifier=Modifier.fillMaxWidth()) {
            Text("选引擎 / 音色："+(voice.selected?.name ?: "尚未选择")+"  ▾",
                maxLines=2)
        }
        if(voice.selected!=null) {
            Text("来源："+voice.selected!!.origin,fontSize=11.sp,color=Sea)
            if(voice.selected!!.requiresLogin) {
                Text("该规则需登录，自动登录脚本未导入；请手动配置有效的请求参数。",
                    fontSize=12.sp,color=Color(0xFF985423))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={
                    editRule=voice.selected;editOpen=true
                },modifier=Modifier.weight(1f)) {Text("编辑接口地址")}
                OutlinedButton(onClick={voice.remove(voice.selectedId)},modifier=Modifier.weight(1f)) {
                    Text("删除规则")
                }
            }
        }
        OutlinedButton(onClick={editRule=null;editOpen=true},
            modifier=Modifier.fillMaxWidth()) {Text("＋ 新建自定义 HTTP 音色")}
        OutlinedButton(onClick={showImport=!showImport},
            modifier=Modifier.fillMaxWidth()) {
            Text(if(showImport)"收起高级导入设置" else "高级：导入更多 Legado 在线朗读规则")
        }
        if(showImport) {
        Text("这里是导入你已有的 Legado 规则 JSON 文件或网络直链，不是《阅读》里的「导出 JSON」菜单；预置音色无需导入。",
            fontSize=12.sp,color=Sea)
        Text("从已有的 JSON 文件导入（可选）",fontWeight=FontWeight.Bold,
            fontSize=14.sp,color=Ink)
        OutlinedButton(onClick={
            picker.launch(arrayOf("application/json","text/plain","*/*"))
        },modifier=Modifier.fillMaxWidth()) {Text("📁 从文件导入 JSON")}
        OutlinedTextField(value=importUrl,onValueChange={importUrl=it.take(700)},
            label={Text("或粘贴规则 JSON 的 HTTPS 直链")},
            placeholder={Text("https://example.com/httpTTS.json")},
            singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedButton(onClick={
            runCatching{voice.importUrl(importUrl.trim())}
                .onFailure{voice.importJsonError(it.message ?: "输入的地址无效")}
        },enabled=importUrl.startsWith("https://"),
            modifier=Modifier.fillMaxWidth()) {Text("从网络导入规则")}
        }
        Text("语速参数："+voice.speed+"（5 慢 / 50 快）",fontSize=13.sp,color=Ink)
        Slider(value=voice.speed.toFloat(),onValueChange={voice.changeSpeed(it.toInt())},
            valueRange=5f..50f,steps=44,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=preview,onValueChange={preview=it.take(120)},
            label={Text("试听文本")},minLines=1,maxLines=3,
            modifier=Modifier.fillMaxWidth())
        if(voice.busy) Text("正在下载语音……",fontSize=12.sp,color=Sea)
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
        Text("社区规则不是官方永久免费 API。部分旧链接失效、需登录，或者会把试听文字传给不受本应用控制的第三方服务器。暂不执行 Legado 的 JS 登录脚本；敏感密钥请勿使用来源不明的接口。",
            fontSize=12.sp,color=Ink.copy(alpha=.72f))
    }
    if(selectOpen) {
        val matches=remember(query,voice.voices) {
            voice.voices.filter {
                query.isBlank() || it.name.contains(query,ignoreCase=true) ||
                    it.origin.contains(query,ignoreCase=true) ||
                    it.url.contains(query,ignoreCase=true)
            }
        }
        AlertDialog(
            onDismissRequest={selectOpen=false},
            title={Text("选择在线朗读音色")},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value=query,onValueChange={query=it},
                        placeholder={Text("搜索：粤语 / 讯飞 / 思必驰 / 搜狗 / 百度")},
                        singleLine=true,modifier=Modifier.fillMaxWidth())
                    Text("找到 "+matches.size+" / "+voice.voices.size+" 条",fontSize=12.sp,color=Sea)
                    LazyColumn(Modifier.heightIn(max=370.dp),
                        verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        items(matches,key={it.id}) { item ->
                            OutlinedButton(onClick={
                                voice.choose(item.id);selectOpen=false
                            },modifier=Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(item.name,color=Ink,maxLines=2,fontSize=13.sp)
                                    Text(item.origin+
                                        if(item.requiresLogin)" · 需登录" else "",
                                        fontSize=11.sp,color=Sea)
                                }
                            }
                        }
                    }
                }
            },confirmButton={
                Button(onClick={selectOpen=false}){Text("关闭")}
            }
        )
    }
    if(editOpen) {
        CantoneseHttpEditor(
            initial=editRule,
            onDismiss={editOpen=false},
            onSave={id,name,url,headers,mime->
                runCatching{voice.saveRule(id,name,url,headers,mime)}
                    .onSuccess{editOpen=false}
                    .onFailure{voice.importJsonError(it.message ?: "规则格式错误")}
            }
        )
    }
}

@Composable
private fun CantoneseHttpEditor(
    initial:LegadoHttpVoice?,
    onDismiss:()->Unit,
    onSave:(String?,String,String,String,String)->Unit,
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var url by remember(initial?.id) { mutableStateOf(initial?.url.orEmpty()) }
    var headers by remember(initial?.id) { mutableStateOf(initial?.headers.orEmpty()) }
    var mime by remember(initial?.id) { mutableStateOf(initial?.mime.orEmpty()) }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(if(initial==null)"新增在线朗读音色" else "编辑在线朗读音色")},
        text={
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value=name,onValueChange={name=it.take(150)},
                    label={Text("音色名称")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(value=url,onValueChange={url=it.take(8000)},
                    label={Text("Legado URL / HTTP 接口地址")},
                    minLines=4,maxLines=8,modifier=Modifier.fillMaxWidth())
                Text("支持直接 GET，或 地址,{\"method\":\"POST\",\"body\":\"...\"}；变量支持 {{speakText}}、{{encodeURIComponent(speakText)}}、{{speakSpeed}} 和常见语速公式。",
                    fontSize=11.sp,color=Sea)
                OutlinedTextField(value=headers,onValueChange={headers=it.take(4000)},
                    label={Text("HTTP 请求头 JSON（选填）")},
                    placeholder={Text("{\"Authorization\":\"Bearer 自己的密钥\"}")},
                    minLines=1,maxLines=4,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(value=mime,onValueChange={mime=it.take(100)},
                    label={Text("音频 MIME（选填）")},
                    placeholder={Text("audio/mpeg")},modifier=Modifier.fillMaxWidth())
            }
        },
        confirmButton={
            Button(onClick={
                onSave(initial?.id,name,url,headers,mime)
            },enabled=name.isNotBlank() && url.isNotBlank()) {Text("保存并选择")}
        },dismissButton={OutlinedButton(onClick=onDismiss){Text("取消")}}
    )
}
