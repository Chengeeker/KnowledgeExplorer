package com.game.knowledge

import android.content.Context
import org.json.JSONArray
import kotlin.random.Random

/** Deliberately separate from the elementary-school science / poetry bank. */
data class CantoneseWord(
    val id:String,
    val kind:String,
    val category:String,
    val hanzi:String,
    val traditional:String,
    val jyutping:String,
    val meaning:String,
    val emoji:String,
    val tip:String
)

object CantoneseBank {
    const val WORD_COUNT=180
    const val PHRASE_COUNT=42
    val categories=listOf(
        "水果乐园","点心茶楼","动物朋友","家人称呼",
        "生活小事","身边世界","交通出行","校园日常",
        "认识身体","运动游戏","岭南文化","温暖的家")
    fun load(context:Context):List<CantoneseWord> {
        val content=context.assets.open("cantonese_beginner.json")
            .bufferedReader(Charsets.UTF_8).use {it.readText()}
        val arr=JSONArray(content)
        return List(arr.length()) { index ->
            val obj=arr.getJSONObject(index)
            CantoneseWord(
                obj.getString("id"),obj.getString("kind"),
                obj.getString("category"),obj.getString("hanzi"),
                obj.getString("traditional"),obj.getString("jyutping"),
                obj.getString("meaning"),obj.getString("emoji"),
                obj.getString("tip"))
        }
    }
}

internal object CantoneseRules {
    fun validJyutping(text:String):Boolean =
        text.isNotBlank() && text.split(" ")
            .all { it.matches(Regex("[a-z]+[1-6]")) }
    /** Four distinct options from the SAME group, so children can really learn. */
    fun options(pool:List<CantoneseWord>,target:CantoneseWord,
                random:Random=Random.Default):List<CantoneseWord> {
        require(pool.any {it.id==target.id})
        val same=pool.filter { it.id!=target.id && it.category==target.category }
        val other=pool.filter { it.id!=target.id && it.category!=target.category }
        return (listOf(target)+same.shuffled(random).take(3)+
            other.shuffled(random).take((3-same.size).coerceAtLeast(0)))
            .take(4).shuffled(random)
    }
    fun groups(words:List<CantoneseWord>):Map<String,List<CantoneseWord>> =
        words.filter {it.kind=="word"}.groupBy {it.category}
    fun newRound(pool:List<CantoneseWord>,size:Int=5,
                 random:Random=Random.Default):List<CantoneseWord> =
        pool.distinctBy {it.id}.shuffled(random).take(size.coerceAtLeast(1))
}
