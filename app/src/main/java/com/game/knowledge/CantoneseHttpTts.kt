package com.game.knowledge

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val RETAINED_VOICE_ID="preset-ddfce9f5b663023a"

internal data class LegadoHttpVoice(
    val id: String,
    val name: String,
    val url: String,
    val headers: String = "",
    val mime: String = "",
    val requiresLogin: Boolean = false,
)

/**
 * Sandboxed subset of Legado HTTP TTS URL rules. Never evaluates arbitrary JS.
 * Accepts GET and POST, literal headers, speakText URL encoding, and
 * speakSpeed arithmetic. Complex login / Rhino rules need manual conversion.
 */
internal object LegadoTtsRequestCompiler {
    private val variable = Regex("\\{\\{(.*?)\\}\\}", RegexOption.DOT_MATCHES_ALL)
    private val encode = Regex(
        "^(?:(?:java\\.)?encodeURI(?:Component)?\\s*\\(\\s*){1,3}speakText\\s*\\){1,3}$",
        RegexOption.IGNORE_CASE
    )
    fun compile(rawRule: String, text: String, speed: Int): PreparedLegadoTtsRequest {
        val rule = rawRule.trim()
        val split = rule.indexOf(",{")
        val address = if(split>=0) rule.substring(0,split) else rule
        val options = if(split>=0) JSONObject(rule.substring(split+1)) else JSONObject()
        val method=options.optString("method","GET").uppercase(Locale.ROOT)
        require(method=="GET" || method=="POST") { "仅支持 GET 和 POST" }
        val url=expand(address,text,speed)
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "规则必须是 HTTP/HTTPS 地址"
        }
        val body=if(options.has("body")) {
            when(val b=options.get("body")){
                is JSONObject -> expand(b.toString(),text,speed)
                is JSONArray -> expand(b.toString(),text,speed)
                else -> expand(b.toString(),text,speed)
            }
        } else ""
        val extra=options.optJSONObject("headers") ?: JSONObject()
        return PreparedLegadoTtsRequest(url,method,body,extra)
    }
    internal fun expand(input: String,text: String,speed: Int):String =
        variable.replace(input) {match ->
            eval(match.groupValues[1].trim(),text,speed)
        }
    private fun eval(raw:String,text:String,speed:Int):String {
        val expr=raw.trim()
        if(expr=="speakText")return text
        val normalized=expr.replace(" ","")
        // The final ')' is part of encodeURI(speakText), not an extra wrapper.
        // Only unwrap parentheses if the resulting expression remains valid.
        val unwrapped=if(normalized.startsWith("(") && normalized.endsWith(")"))
            normalized.substring(1,normalized.length-1) else normalized
        val noOuter=if(encode.matches(unwrapped))unwrapped else normalized
        if(encode.matches(noOuter)) {
            var result=text
            val times=Regex("encodeURI",RegexOption.IGNORE_CASE)
                .findAll(noOuter).count().coerceIn(1,3)
            repeat(times){result=URLEncoder.encode(result,"UTF-8")}
            return result
        }
        val arithmetic=if(normalized.startsWith("String(") && normalized.endsWith(")"))
            normalized.substring(7,normalized.length-1) else normalized
        require(arithmetic.contains("speakSpeed") &&
            arithmetic.replace("speakSpeed","")
                .all{it.isDigit() || it in "()+-*/. "}) {
            "该规则包含未支持的 JavaScript 表达式，请改成可直接请求的 HTTP URL"
        }
        val value=LegadoArithmetic(arithmetic.replace("speakSpeed",speed.toString())).parse()
        require(value.isFinite() && value in -1000000.0..1000000.0){"语速表达式无效"}
        return if(value==value.toLong().toDouble())value.toLong().toString()
            else value.toString()
    }
}

internal data class PreparedLegadoTtsRequest(
    val url: String, val method: String, val body: String, val headers: JSONObject
)

private class LegadoArithmetic(private val expression:String) {
    private var i=0
    fun parse():Double {
        val result=sum()
        ws()
        require(i==expression.length){"语速表达式格式不正确"}
        return result
    }
    private fun ws() {while(i<expression.length && expression[i].isWhitespace())i++}
    private fun sum():Double {
        var value=product()
        while(true) {
            ws()
            when {
                consume('+')->value+=product()
                consume('-')->value-=product()
                else->return value
            }
        }
    }
    private fun product():Double {
        var value=atom()
        while(true){
            ws()
            when {
                consume('*')->value*=atom()
                consume('/')->value/=atom()
                else->return value
            }
        }
    }
    private fun atom():Double {
        ws()
        if(consume('-'))return -atom()
        if(consume('+'))return atom()
        if(consume('(')) {
            val v=sum()
            require(consume(')')){"表达式缺少右括号"}
            return v
        }
        val from=i
        while(i<expression.length && (expression[i].isDigit() || expression[i]=='.'))i++
        require(i>from){"表达式数字格式无效"}
        return expression.substring(from,i).toDouble()
    }
    private fun consume(ch:Char):Boolean {
        ws()
        if(i<expression.length && expression[i]==ch){i++;return true}
        return false
    }
}

internal class CantoneseHttpTts(private val app:Context) {
    private val prefs=app.getSharedPreferences("cantonese_http_tts",Context.MODE_PRIVATE)
    private val worker=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private val client=OkHttpClient.Builder()
        .connectTimeout(8,java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(25,java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true).build()
    private val cache=File(app.cacheDir,"cantonese_http_speech").apply{mkdirs()}
    var voices by mutableStateOf<List<LegadoHttpVoice>>(emptyList())
        private set
    var enabled by mutableStateOf(prefs.getBoolean("enabled",false))
        private set
    var selectedId by mutableStateOf(prefs.getString("selected","").orEmpty())
        private set
    var speedDelta by mutableIntStateOf(prefs.getInt("speedDelta",0).coerceIn(-30,30))
        private set
    var pitchDelta by mutableIntStateOf(prefs.getInt("pitchDelta",0).coerceIn(-20,20))
        private set
    var volumeDelta by mutableIntStateOf(prefs.getInt("volumeDelta",0).coerceIn(-50,20))
        private set
    var busy by mutableStateOf(false)
        private set
    var status by mutableStateOf("试听会把文字发送到在线语音服务")
        private set
    val selected:LegadoHttpVoice? get()=voices.firstOrNull{it.id==selectedId}
    val available:Boolean get()=enabled && selected!=null
    init {
        val previousSelection=prefs.getString("selected","")
        val previousRules=prefs.getString("rules",null)
        val wasEnabled=enabled
        voices=runCatching{parseRules(app.assets.open("cantonese_tts_catalog.json")
            .bufferedReader().use{it.readText()})}.getOrDefault(emptyList())
            .filter{it.id==RETAINED_VOICE_ID}
        selectedId=voices.firstOrNull()?.id.orEmpty()
        val retained=voices.firstOrNull()
        val oldRuleMatches=previousRules==null || runCatching {
            parseRules(previousRules).firstOrNull{it.id==RETAINED_VOICE_ID}
                ?.let{retained!=null && it.url==retained.url && it.headers==retained.headers}==true
        }.getOrDefault(false)
        enabled=wasEnabled && previousSelection==RETAINED_VOICE_ID && oldRuleMatches && retained!=null
        prefs.edit().remove("rules").remove("speed").putString("selected",selectedId)
            .putBoolean("enabled",enabled).putInt("speedDelta",speedDelta)
            .putInt("pitchDelta",pitchDelta).putInt("volumeDelta",volumeDelta).apply()
    }

    private fun parseRules(json:String):List<LegadoHttpVoice> {
        val list=JSONArray(json)
        return (0 until list.length()).mapNotNull {i->
            val o=list.optJSONObject(i)?:return@mapNotNull null
            val raw=o.optString("url").trim()
            if(!raw.startsWith("http://") && !raw.startsWith("https://"))return@mapNotNull null
            val id=o.optString("id").ifBlank{return@mapNotNull null}
            LegadoHttpVoice(
                id=id, name=o.optString("name").ifBlank{"未命名音色"}.take(150),
                url=raw.take(8000),
                headers=o.optString("headers",o.optString("header","")).take(4000),
                mime=o.optString("mime",o.optString("contentType","")).take(100),
                requiresLogin=o.optBoolean("requiresLogin",false) ||
                    o.optString("loginUrl").isNotBlank() ||
                    o.optString("loginUi").isNotBlank(),
            )
        }
    }
    fun toggle(value:Boolean) {
        enabled=value
        prefs.edit().putBoolean("enabled",value).apply()
    }
    fun changeSpeed(value:Int) {speedDelta=value.coerceIn(-30,30);prefs.edit().putInt("speedDelta",speedDelta).apply()}
    fun changePitch(value:Int) {pitchDelta=value.coerceIn(-20,20);prefs.edit().putInt("pitchDelta",pitchDelta).apply()}
    fun changeVolume(value:Int) {volumeDelta=value.coerceIn(-50,20);prefs.edit().putInt("volumeDelta",volumeDelta).apply()}
    private fun key(rule:LegadoHttpVoice,text:String,slow:Boolean):String {
        val bytes=MessageDigest.getInstance("SHA-256")
            .digest((rule.url+"|"+rule.headers+"|"+text+"|"+slow+"|"+
                speedDelta+"|"+pitchDelta+"|"+volumeDelta).toByteArray())
        return bytes.joinToString(""){"%02x".format(it)}.take(40)
    }
    fun cached(text:String,slow:Boolean):File? {
        val rule=selected?:return null
        val base=key(rule,text,slow)
        return listOf("mp3","wav","ogg","aac").map{File(cache,base+"."+it)}
            .firstOrNull{it.isFile && it.length()>100}
    }
    fun generate(text:String,slow:Boolean,onResult:(File?)->Unit) {
        val rule=selected
        if(!enabled || rule==null){onResult(null);return}
        cached(text,slow)?.let{onResult(it);return}
        if(busy){status="上一次语音尚在生成";onResult(null);return}
        busy=true
        status="正在请求 "+rule.name
        worker.execute{
            val result=runCatching{fetch(rule,text,slow)}
            main.post{
                busy=false
                val file=result.getOrNull()
                status=if(file!=null)"音频已就绪："+rule.name
                    else "合成失败（"+rule.name+"）：" +
                        (result.exceptionOrNull()?.let{error->
                            var cause=error
                            repeat(6){if(cause.cause!=null && cause.cause!==cause)cause=cause.cause!!}
                            cause.javaClass.simpleName+": "+(cause.message ?: "未知错误")
                        } ?: "未知错误").take(240)
                onResult(file)
            }
        }
    }
    private fun fetch(rule:LegadoHttpVoice,text:String,slow:Boolean):File {
        if(rule.requiresLogin)error("需要登录鉴权：请改为带有效凭据的 HTTP 规则")
        val options=LegadoTtsRequestCompiler.compile(rule.url,text,0)
        val url=options.url.toHttpUrl().newBuilder()
            .setQueryParameter("speedDelta",(speedDelta-if(slow)10 else 0).coerceIn(-30,30).toString())
            .setQueryParameter("pitchDelta",pitchDelta.toString())
            .setQueryParameter("volumeDelta",volumeDelta.toString())
            .build()
        val headers=JSONObject()
        if(rule.headers.isNotBlank()){
            val o=JSONObject(rule.headers)
            val it=o.keys()
            while(it.hasNext()){val k=it.next();headers.put(k,o.getString(k))}
        }
        val keys=options.headers.keys()
        while(keys.hasNext()){val k=keys.next();headers.put(k,options.headers.getString(k))}
        val builder=Request.Builder().url(url)
        val names=headers.keys()
        while(names.hasNext()){
            val k=names.next()
            builder.header(k,headers.getString(k))
        }
        if(options.method=="POST") {
            val isJson=options.body.trimStart().startsWith("{")
            val mime=if(isJson)"application/json; charset=utf-8"
                else "application/x-www-form-urlencoded; charset=utf-8"
            builder.post(options.body.toRequestBody(mime.toMediaTypeOrNull()))
        } else builder.get()
        return client.newCall(builder.build()).execute().use {response->
            if(!response.isSuccessful)error("HTTP "+response.code+
                " / "+(response.header("Content-Type") ?: "未知响应类型"))
            val contentLength=response.body?.contentLength() ?: -1
            require(contentLength<=8_000_000){"返回内容超过 8 MB"}
            val payload=response.body?.byteStream()?.use{stream->
                val output=ByteArrayOutputStream()
                val chunk=ByteArray(8192)
                while(true) {
                    val received=stream.read(chunk)
                    if(received<0)break
                    require(output.size()+received<=8_000_000){"音频超过 8 MB"}
                    output.write(chunk,0,received)
                }
                output.toByteArray()
            }?:error("服务器没有返回音频")
            require(payload.size in 101..8_000_000){"返回音频为空或超过 8 MB"}
            val head=String(payload,0,payload.size.coerceAtMost(10),Charsets.ISO_8859_1)
            val mime=response.header("Content-Type",rule.mime).orEmpty().lowercase(Locale.ROOT)
            require(!head.trimStart().startsWith("<") &&
                !head.trimStart().startsWith("{")) {
                "接口返回网页或 JSON 而非音频（"+mime.take(55)+"）"
            }
            val format=when {
                head.startsWith("RIFF")->"wav"
                head.startsWith("ID3") || (payload[0].toInt() and 255)==255 ->"mp3"
                head.startsWith("OggS")->"ogg"
                mime.startsWith("audio/mpeg") ->"mp3"
                mime.startsWith("audio/wav") ->"wav"
                mime.startsWith("audio/ogg") ->"ogg"
                mime.startsWith("audio/aac") ->"aac"
                else->error("返回的不是可识别音频，Content-Type: "+mime.take(70))
            }
            val file=File(cache,key(rule,text,slow)+"."+format)
            val temporary=File(cache,file.name+".part")
            try {
                temporary.writeBytes(payload)
                require(temporary.renameTo(file)){"音频缓存写入失败"}
            } finally {temporary.delete()}
            pruneCache()
            file
        }
    }
    private fun pruneCache() {
        val files=cache.listFiles()?.filter{it.isFile}?.sortedByDescending{it.lastModified()}?:return
        var total=0L
        files.forEach{
            total+=it.length()
            if(total>64_000_000L)it.delete()
        }
    }
    fun close(){client.dispatcher.cancelAll();worker.shutdownNow()}
}
