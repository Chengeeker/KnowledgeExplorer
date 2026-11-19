package com.game.knowledge

import android.content.Context
import org.json.JSONArray
import kotlin.random.Random

data class KnowledgeQuestion(
    val id: String,
    val grade: Int,
    val domain: String,
    val topic: String,
    val kind: String,
    val stars: Int,
    val question: String,
    val answer: String,
    val options: List<String>,
    val explanation: String,
    val source: String
)

data class QuestionFilter(
    val grade: Int = 1, // -1: all grades, 1..6: matching curricular grade
    val domain: String = "全部",
    val topic: String = "全部",
    val kind: String = "全部"
)

/**
 * Keep every child's chosen grade strict. Adding appropriate 1st and 2nd
 * grade literature is better than silently substituting older children's work.
 */
internal object QuestionFilterChoices {
    /** Only categories with questions in the selected grade appear. */
    fun domains(all: List<KnowledgeQuestion>, filter: QuestionFilter): List<String> =
        all.asSequence().filter { filter.grade == -1 || it.grade == filter.grade }
            .map { it.domain }.distinct().sorted().toList()

    /** Topics are narrowed only after an explicit domain and grade selection. */
    fun topics(all: List<KnowledgeQuestion>, filter: QuestionFilter): List<String> =
        all.asSequence()
            .filter { filter.grade == -1 || it.grade == filter.grade }
            .filter { filter.domain == "全部" || it.domain == filter.domain }
            .map { it.topic }.distinct().sorted().toList()

    /**
     * Never broaden curricular grade silently. Unavailable domains are not
     * selectable; if only an old "kind" filter blocks this domain, reset kind.
     */
    fun selectDomain(
        all: List<KnowledgeQuestion>, filter: QuestionFilter, selected: String
    ): QuestionFilter {
        if (selected == "全部") return filter.copy(domain = "全部", topic = "全部")
        val existsInGrade = all.any { it.domain == selected &&
            (filter.grade == -1 || it.grade == filter.grade) }
        if (!existsInGrade) return filter
        val candidate = filter.copy(domain = selected, topic = "全部")
        return if (KnowledgeBank.matching(all, candidate).isEmpty())
            candidate.copy(kind = "全部") else candidate
    }

    /** Old kind filters can exclude a new topic even though the topic exists. */
    fun selectTopic(
        all: List<KnowledgeQuestion>, filter: QuestionFilter, selected: String
    ): QuestionFilter {
        val candidate = filter.copy(topic = selected)
        return if (selected != "全部" &&
            KnowledgeBank.matching(all, candidate).isEmpty() &&
            KnowledgeBank.matching(all, candidate.copy(kind = "全部")).isNotEmpty()
        ) candidate.copy(kind = "全部") else candidate
    }
}

object KnowledgeBank {
    fun load(context: Context): List<KnowledgeQuestion> {
        val text = context.assets.open("questions.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        return (0 until arr.length()).map { i ->
            val q = arr.getJSONObject(i)
            val opt = q.getJSONArray("options")
            KnowledgeQuestion(
                q.getString("id"), q.getInt("grade"), q.getString("domain"),
                q.getString("topic"), q.getString("kind"), q.getInt("stars"),
                q.getString("question"), q.getString("answer"),
                (0 until opt.length()).map { opt.getString(it) },
                q.getString("explanation"), q.optString("source", "")
            )
        }
    }
    fun matching(questions: List<KnowledgeQuestion>, filter: QuestionFilter): List<KnowledgeQuestion> =
        questions.filter { q ->
            (filter.grade == -1 || q.grade == filter.grade) &&
            (filter.domain == "全部" || q.domain == filter.domain) &&
            (filter.topic == "全部" || q.topic == filter.topic) &&
            (filter.kind == "全部" || q.kind == filter.kind)
        }
    /** Prefer unseen questions across adjacent rounds while preserving no repeats inside one. */
    fun round(
        questions: List<KnowledgeQuestion>, filter: QuestionFilter, count: Int,
        random: Random = Random.Default, avoidIds: Set<String> = emptySet()
    ): List<KnowledgeQuestion> {
        val pool = matching(questions, filter).shuffled(random)
        return (pool.filter { it.id !in avoidIds } +
            pool.filter { it.id in avoidIds }).take(count.coerceAtLeast(1))
    }
}

object DuelRules {
    fun reactionMs(level: Int, random: Random = Random.Default): Long {
        val (min, max) = when (level.coerceIn(0,3)) {
            0 -> 6000L to 8000L
            1 -> 4000L to 6000L
            2 -> 2500L to 4000L
            else -> 1500L to 2500L
        }
        return random.nextLong(min,max + 1)
    }
    fun winnerByAccuracy(left: Int, right: Int, leftMs: Long, rightMs: Long): Int = when {
        left > right -> 0
        right > left -> 1
        leftMs < rightMs -> 0
        rightMs < leftMs -> 1
        else -> -1
    }
}

fun pictureFor(question: KnowledgeQuestion): String {
    // Never inspect the answer: illustrations are shown before children reveal it.
    val haystack = question.question + question.topic
    val specific = listOf(
        "眼睛" to "👀", "鼻子" to "👃", "耳朵" to "👂", "牙齿" to "🦷",
        "蝴蝶" to "🦋", "蜻蜓" to "🪷", "青蛙" to "🐸", "蜗牛" to "🐌",
        "蚂蚁" to "🐜", "蜜蜂" to "🐝", "熊猫" to "🐼", "老虎" to "🐯",
        "狮子" to "🦁", "鲸" to "🐳", "海豚" to "🐬", "企鹅" to "🐧",
        "乌龟" to "🐢", "章鱼" to "🐙", "鱼" to "🐟", "鸟" to "🐦",
        "猴子" to "🐒", "兔" to "🐇", "猫" to "🐱", "狗" to "🐶",
        "蛇" to "🐍", "恐龙" to "🦕", "植物" to "🌿", "花" to "🌷",
        "向日葵" to "🌻", "松树" to "🌲", "竹" to "🎋", "水稻" to "🌾",
        "月亮" to "🌙", "太阳" to "☀️", "地球" to "🌍", "火星" to "🪐",
        "星" to "✨", "云" to "☁️", "彩虹" to "🌈", "雨" to "🌧️",
        "雪" to "❄️", "风" to "🌬️", "火山" to "🌋", "山" to "⛰️",
        "海洋" to "🌊", "河流" to "🏞️", "沙漠" to "🏜️",
        "中国" to "🇨🇳", "地图" to "🗺️", "地理" to "🧭",
        "足球" to "⚽", "篮球" to "🏀", "游泳" to "🏊", "音乐" to "🎵",
        "绘画" to "🎨", "汉字" to "🖌️", "诗" to "📜",
        "电脑" to "💻", "网络" to "🌐", "机器人" to "🤖", "电" to "💡",
        "交通" to "🚦", "火车" to "🚂", "汽车" to "🚗",
        "安全" to "🛟", "环保" to "♻️", "垃圾" to "🗑️",
        "数学" to "🧩", "法" to "⚖️", "历史" to "🏛️"
    )
    return specific.firstOrNull { haystack.contains(it.first) }?.second ?: when(question.domain) {
        "自然科学" -> "🔬"
        "地理" -> "🗺️"
        "传统文化", "历史文化" -> "🏮"
        "逻辑与数学" -> "🧠"
        "语文文学" -> "📚"
        "艺术体育" -> "🎨"
        "信息科技" -> "💻"
        "环境保护" -> "🌱"
        "安全与健康" -> "🩺"
        "社会与法治" -> "⚖️"
        else -> "🧭"
    }
}
