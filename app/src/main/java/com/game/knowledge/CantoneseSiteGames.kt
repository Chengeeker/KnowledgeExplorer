package com.game.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import kotlin.random.Random

internal enum class CantoneseWebsiteGame(
    val title:String,
    val intro:String,
    val tint:Color
) {
    REPLY("对话小剧场","遇到不同情况，挑出合适的粤语短句。",Color(0xFFEEE8FF)),
    LISTEN("听一听","听提问，选回答。",Color(0xFFE0F1FF)),
    TASK("生活小任务","跟着目标，完成一段生活对话。",Color(0xFFFFE9D9)),
    TIME("听懂时间","听粤语报时，选出正确时间。",Color(0xFFFFF1D8))
}

private data class CantoneseGameOption(val id:String,val text:String)

private data class CantoneseGameQuestion(
    val id:String,
    val prompt:String,
    val options:List<CantoneseGameOption>,
    val answerId:String,
    val explanation:String,
    val region:String?=null
)

private data class CantoneseSentenceQuestion(
    val id:String,
    val pieces:List<String>,
    val answer:String
)

private data class CantoneseTimeQuestion(
    val id:String,
    val prompt:String,
    val answerMinutes:Int,
    val choices:List<Int>
)

private data class CantonesePriceQuestion(
    val id:String,
    val prompt:String,
    val amountMinorUnits:Long
)

private data class CantoneseReplyScene(
    val id:String,
    val situation:String,
    val answerId:String,
    val explanation:String
)

private fun replyChoices(a:String,b:String,c:String)=listOf(
    CantoneseGameOption("a",a),CantoneseGameOption("b",b),
    CantoneseGameOption("c",c)
)

private fun replyGroup(
    region:String,
    topic:String,
    choices:List<CantoneseGameOption>,
    vararg scenes:CantoneseReplyScene
)=scenes.map {scene->
    CantoneseGameQuestion(scene.id,
        "$region · $topic\n${scene.situation}\n\n你会怎样说？",
        choices,scene.answerId,scene.explanation,region)
}

private val cantoneseReplyRegions=listOf(
    "香港生活","广州生活","马来西亚生活","新加坡生活")

private object CantoneseSiteGameBank {
    val sentences=listOf(
        CantoneseSentenceQuestion("sentence_meal",
            listOf("我哋","等阵","去","边度","食饭？"),"我哋等阵去边度食饭？"),
        CantoneseSentenceQuestion("sentence_station",
            listOf("唔该，","呢度","点去","地铁站？"),"唔该，呢度点去地铁站？"),
        CantoneseSentenceQuestion("sentence_ticket",
            listOf("我想","买","两张","车票。"),"我想买两张车票。")
    )

    val listening=listOf(
        CantoneseGameQuestion("listen_train","你想搭边班车？",listOf(
            CantoneseGameOption("a","我要搭下一班车。"),
            CantoneseGameOption("b","今日天气几好。"),
            CantoneseGameOption("c","请问有冇纸巾？"),
            CantoneseGameOption("d","我想买橙。")
        ),"a","这句回答了想乘坐哪一班车。"),
        CantoneseGameQuestion("listen_post","请问邮局点行？",listOf(
            CantoneseGameOption("a","我想饮冻柠茶。"),
            CantoneseGameOption("b","直行再转右就到。"),
            CantoneseGameOption("c","今日我带咗本书。"),
            CantoneseGameOption("d","呢个苹果几甜。")
        ),"b","这句给出了去邮局的方向。"),
        CantoneseGameQuestion("listen_water","你想唔想饮水呀？",listOf(
            CantoneseGameOption("a","好呀，唔该。"),
            CantoneseGameOption("b","我想搭地铁返学。"),
            CantoneseGameOption("c","呢度点去车站？"),
            CantoneseGameOption("d","我哋等阵去食饭。")
        ),"a","这句回应了要不要喝水。")
    )

    val replies=listOf(
        replyGroup("香港生活","店内用餐",replyChoices(
            "冻柠茶，唔该。","我要一碟肠粉，喺度食。","呢碟点心帮我打包，唔该。"),
            CantoneseReplyScene("hk_dine_tea","你在茶餐厅想点一杯冻柠茶。","a","这句是在礼貌地点饮品。"),
            CantoneseReplyScene("hk_dine_rice","你在粉面店想点肠粉，并打算在店里吃。","b","这句说明了想点的食物和堂食安排。"),
            CantoneseReplyScene("hk_dine_takeaway","你在茶楼买点心，准备带走。","c","这句告诉店员把点心打包。")),
        replyGroup("香港生活","香港交通",replyChoices(
            "搭市区巴士可唔可以畀现金？","呢架巴士去唔去中环？","可唔可以用八达通？"),
            CantoneseReplyScene("hk_transit_cash","你准备搭市区巴士，想先问车上能不能用现金。","a","这句是在询问巴士能否收现金。"),
            CantoneseReplyScene("hk_transit_central","你在巴士站等车，想确认这架巴士去不去中环。","b","这句是在确认巴士的目的地。"),
            CantoneseReplyScene("hk_transit_octopus","你准备上巴士，想确认能不能用八达通付车费。","c","这句是在询问能否用八达通付款。")),
        replyGroup("香港生活","问路",replyChoices(
            "唔该，最近嘅地铁站点去？","我想去中环，应该点转车？","直行到前面再转左就到。"),
            CantoneseReplyScene("hk_route_mtr","你想问最近的地铁站应该怎么走。","a","这句是在询问前往地铁站的路线。"),
            CantoneseReplyScene("hk_route_transfer","你要去中环，想问应该在哪一站转车。","b","这句是在询问换乘路线。"),
            CantoneseReplyScene("hk_route_explain","朋友问你地铁站怎么走，你知道要直行再左转。","c","这句给出了清楚的行走方向。")),
        replyGroup("香港生活","购物",replyChoices(
            "呢盒饼干几多钱？","我想要两个，唔该。","可唔可以睇下其他款？"),
            CantoneseReplyScene("hk_shop_price","你在便利店看中一盒饼干，想问价钱。","a","这句是在询问商品价格。"),
            CantoneseReplyScene("hk_shop_two","你决定买两个面包。","b","这句说明了要买的数量。"),
            CantoneseReplyScene("hk_shop_style","你想看看这件衣服的其他款式。","c","这句是在请求看看别的款式。")),
        replyGroup("香港生活","日常沟通",replyChoices(
            "我仲未到，到了再话你知。","唔该等我一阵，我就到。","我今日有啲唔舒服，想休息下。"),
            CantoneseReplyScene("hk_daily_notarrived","朋友问你到了没有，你还在路上。","a","这句说明你还没到，并答应到达后告知。"),
            CantoneseReplyScene("hk_daily_wait","你快到约定地点了，请朋友稍等一会儿。","b","这句礼貌地请对方等一会儿。"),
            CantoneseReplyScene("hk_daily_rest","你今天身体不舒服，想先休息。","c","这句说明身体状况和休息打算。")),

        replyGroup("广州生活","店内用餐",replyChoices(
            "唔该，来一碗艇仔粥。","我要一份肠粉，喺度食。","呢几样点心帮我打包，唔该。"),
            CantoneseReplyScene("gz_dine_congee","你在茶楼想点一碗艇仔粥。","a","这句是在礼貌地点艇仔粥。"),
            CantoneseReplyScene("gz_dine_rice","你想点肠粉并在店里吃。","b","这句说明了想点的食物和堂食安排。"),
            CantoneseReplyScene("gz_dine_takeaway","你选了几样点心，打算带回家吃。","c","这句告诉店员把点心打包。")),
        replyGroup("广州生活","公共交通",replyChoices(
            "去北京路应该喺边个站落？","呢班地铁去唔去体育西路？","唔该，最近嘅公交站喺边度？"),
            CantoneseReplyScene("gz_transit_beijing","你准备坐地铁去北京路，想问在哪一站下车。","a","这句是在询问下车站点。"),
            CantoneseReplyScene("gz_transit_tiyu","你在站台等车，想确认列车会不会到体育西路。","b","这句是在确认地铁线路和目的地。"),
            CantoneseReplyScene("gz_transit_bus","你想找附近的公交站。","c","这句是在询问公交站的位置。")),
        replyGroup("广州生活","问路",replyChoices(
            "唔该，去北京路点行？","直行过咗前面个路口就到。","呢度行去地铁站要几耐？"),
            CantoneseReplyScene("gz_route_beijing","你想向路人问去北京路的路线。","a","这句是在询问去北京路怎么走。"),
            CantoneseReplyScene("gz_route_explain","朋友问附近的路，你知道直行过路口就到了。","b","这句给出了行走方向。"),
            CantoneseReplyScene("gz_route_time","你想知道从这里步行到地铁站需要多久。","c","这句是在询问步行时间。")),
        replyGroup("广州生活","购物",replyChoices(
            "呢个橙几多钱？","我想买两个，唔该。","有冇其他颜色？"),
            CantoneseReplyScene("gz_shop_price","你在水果店看中一个橙，想问价钱。","a","这句是在询问商品价格。"),
            CantoneseReplyScene("gz_shop_two","你决定买两个橙。","b","这句说明了要买的数量。"),
            CantoneseReplyScene("gz_shop_color","你看中一件衣服，想问有没有别的颜色。","c","这句是在询问其他颜色。")),
        replyGroup("广州生活","日常沟通",replyChoices(
            "我到天河啦，你喺边度？","唔该等我一阵，我就到。","我有啲唔舒服，想早点返屋企。"),
            CantoneseReplyScene("gz_daily_arrived","朋友问你在哪里，你已经到天河。","a","这句告诉对方你已到达并反问对方位置。"),
            CantoneseReplyScene("gz_daily_wait","你还差一会儿到约定地点，请朋友稍等。","b","这句礼貌地请对方等一会儿。"),
            CantoneseReplyScene("gz_daily_rest","你觉得不舒服，想早点回家休息。","c","这句说明身体状况和回家打算。")),

        replyGroup("马来西亚生活","店内用餐",replyChoices(
            "唔该，来一份鸡饭。","我想要杯白咖啡，唔该。","呢碟帮我打包，唔该。"),
            CantoneseReplyScene("my_dine_rice","你在怡保的餐店想点一份鸡饭。","a","这句是在礼貌地点鸡饭。"),
            CantoneseReplyScene("my_dine_coffee","你在咖啡店想点一杯白咖啡。","b","这句是在点饮品。"),
            CantoneseReplyScene("my_dine_takeaway","你吃不完这份饭，想带走。","c","这句请店员把食物打包。")),
        replyGroup("马来西亚生活","公共交通",replyChoices(
            "附近嘅巴士站喺边度？","呢班巴士去唔去市中心？","去火车站应该点转车？"),
            CantoneseReplyScene("my_transit_stop","你在陌生街区，想找最近的巴士站。","a","这句是在询问巴士站的位置。"),
            CantoneseReplyScene("my_transit_center","你在巴士站等车，想确认这班车去不去市中心。","b","这句是在确认巴士的目的地。"),
            CantoneseReplyScene("my_transit_train","你准备转车去火车站，想问怎么换乘。","c","这句是在询问换乘路线。")),
        replyGroup("马来西亚生活","问路",replyChoices(
            "唔该，去附近嘅巴士站点行？","沿住呢条路直行就到。","行去火车站大概要几耐？"),
            CantoneseReplyScene("my_route_stop","你想问去最近的巴士站应该怎么走。","a","这句是在询问去巴士站的路线。"),
            CantoneseReplyScene("my_route_explain","有人问路，你知道沿这条路直走就能到。","b","这句给出了行走方向。"),
            CantoneseReplyScene("my_route_time","你想知道步行到火车站大约要多久。","c","这句是在询问步行时间。")),
        replyGroup("马来西亚生活","购物",replyChoices(
            "呢个芒果几多钱？","我想买两个，唔该。","有冇大一啲嘅？"),
            CantoneseReplyScene("my_shop_price","你在市场挑中一个芒果，想问价钱。","a","这句是在询问商品价格。"),
            CantoneseReplyScene("my_shop_two","你决定买两个芒果。","b","这句说明了要买的数量。"),
            CantoneseReplyScene("my_shop_size","你想买大一点的同款商品。","c","这句是在询问有没有更大的尺寸。")),
        replyGroup("马来西亚生活","日常沟通",replyChoices(
            "我仲喺怡保，未到你嗰边。","你到咗同我讲声。","我今日唔舒服，想返去休息。"),
            CantoneseReplyScene("my_daily_notarrived","朋友问你到集合地点没有，你还在怡保。","a","这句说明你还没到对方那里。"),
            CantoneseReplyScene("my_daily_message","你先到了约定地点，请朋友到达后告诉你。","b","这句请对方到达时通知你。"),
            CantoneseReplyScene("my_daily_rest","你身体不舒服，想回去休息。","c","这句说明身体状况和休息打算。")),

        replyGroup("新加坡生活","店内用餐",replyChoices(
            "唔该，来一份海南鸡饭。","我想要一份咖椰吐司，唔该。","呢份帮我打包带走，唔该。"),
            CantoneseReplyScene("sg_dine_rice","你在食阁想点一份海南鸡饭。","a","这句是在礼貌地点海南鸡饭。"),
            CantoneseReplyScene("sg_dine_toast","你在咖啡店想点一份咖椰吐司。","b","这句是在点餐。"),
            CantoneseReplyScene("sg_dine_takeaway","你赶时间，想把这份餐点带走。","c","这句告诉店员要打包带走。")),
        replyGroup("新加坡生活","公共交通",replyChoices(
            "我应该喺边个 MRT 站转车？","呢班巴士去唔去乌节路？","去樟宜机场应该点搭车？"),
            CantoneseReplyScene("sg_transit_change","你要换乘地铁，想问应该在哪个 MRT 站转车。","a","这句是在询问地铁换乘站。"),
            CantoneseReplyScene("sg_transit_orchard","你在巴士站等车，想确认这班车去不去乌节路。","b","这句是在确认巴士的目的地。"),
            CantoneseReplyScene("sg_transit_airport","你想从市区前往樟宜机场，想问怎么搭车。","c","这句是在询问去机场的交通方式。")),
        replyGroup("新加坡生活","问路",replyChoices(
            "唔该，去滨海湾点行？","最近嘅 MRT 站喺边度？","直行过咗路口就见到。"),
            CantoneseReplyScene("sg_route_bay","你想问去滨海湾应该怎么走。","a","这句是在询问去滨海湾的路线。"),
            CantoneseReplyScene("sg_route_mrt","你想找到最近的 MRT 站。","b","这句是在询问地铁站的位置。"),
            CantoneseReplyScene("sg_route_explain","朋友问附近的路，你知道直行过路口就看到了。","c","这句给出了行走方向。")),
        replyGroup("新加坡生活","购物",replyChoices(
            "呢个几多钱？","我想买两个，唔该。","有冇大一号嘅？"),
            CantoneseReplyScene("sg_shop_price","你在商店看中一个水壶，想问价钱。","a","这句是在询问商品价格。"),
            CantoneseReplyScene("sg_shop_two","你决定买两个同款杯子。","b","这句说明了要买的数量。"),
            CantoneseReplyScene("sg_shop_size","你想买大一号的同款衣服。","c","这句是在询问有没有更大的尺寸。")),
        replyGroup("新加坡生活","日常沟通",replyChoices(
            "我仲未到，到了再话你知。","我喺 MRT 出口等紧你。","我会迟少少，麻烦你等我一阵。"),
            CantoneseReplyScene("sg_daily_notarrived","朋友问你到了没有，你还在路上。","a","这句说明你还没到，并答应到达后告知。"),
            CantoneseReplyScene("sg_daily_waiting","你已到 MRT 出口，告诉朋友你在这里等。","b","这句告诉对方你正在出口等候。"),
            CantoneseReplyScene("sg_daily_late","你会晚一点到，想请朋友等你一会儿。","c","这句礼貌地说明会迟到并请对方稍等。"))
    ).flatten()

    init {
        check(replies.size==60 && replies.map {it.id}.distinct().size==60)
        check(cantoneseReplyRegions.all {region->replies.count {it.region==region}==15})
        check(replies.all {question->
            question.options.size==3 && question.options.map {it.id}.distinct().size==3 &&
                question.options.any {it.id==question.answerId}
        })
    }

    val task=listOf(
        CantoneseGameQuestion("task_where","你想去边度呀？",listOf(
            CantoneseGameOption("a","我想去地铁站，唔该。"),
            CantoneseGameOption("b","我想饮冻柠茶。"),
            CantoneseGameOption("c","呢个几多钱？"),
            CantoneseGameOption("d","我今日带咗本书。")
        ),"a","先告诉对方你要去地铁站。"),
        CantoneseGameQuestion("task_transport","你想搭地铁定巴士？",listOf(
            CantoneseGameOption("a","我唔识睇书。"),
            CantoneseGameOption("b","我想搭地铁。"),
            CantoneseGameOption("c","有冇热水？"),
            CantoneseGameOption("d","呢份点心好香。")
        ),"b","选择地铁作为出行方式。"),
        CantoneseGameQuestion("task_thanks","直行到前面就见到入口啦。",listOf(
            CantoneseGameOption("a","好嘅，唔该晒！"),
            CantoneseGameOption("b","我想买两件。"),
            CantoneseGameOption("c","佢喺屋企。"),
            CantoneseGameOption("d","今日星期几？")
        ),"a","感谢对方并确认收到方向。")
    )

    val times=listOf(
        CantoneseTimeQuestion("time_1435","而家系下昼两点三十五分。",875,
            listOf(875,865,935,885)),
        CantoneseTimeQuestion("time_1915","电影夜晚七点十五分开始。",1155,
            listOf(1115,1155,1175,1215)),
        CantoneseTimeQuestion("time_1005","巴士十点零五分开。",605,
            listOf(505,605,610,705))
    )

    val prices=listOf(
        CantonesePriceQuestion("price_noodles","呢碗面廿三蚊。",2300),
        CantonesePriceQuestion("price_drink","杯饮品十五蚊五毫。",1550),
        CantonesePriceQuestion("price_biscuit","呢包饼干八蚊九毫。",890)
    )

    fun questions(mode:CantoneseWebsiteGame):List<CantoneseGameQuestion> = when(mode) {
        CantoneseWebsiteGame.REPLY -> replies
        CantoneseWebsiteGame.LISTEN -> listening
        CantoneseWebsiteGame.TASK -> task
        CantoneseWebsiteGame.TIME -> times.map {question->
            CantoneseGameQuestion(question.id,question.prompt,
                question.choices.map {CantoneseGameOption(it.toString(),formatTime(it))},
                question.answerMinutes.toString(),"正确时间是 ${formatTime(question.answerMinutes)}。")
        }
    }
}

@Composable
internal fun CantoneseWebsiteChoiceGameScreen(
    mode:CantoneseWebsiteGame,
    slow:Boolean,
    voice:CantoneseSpeaker,
    onCompleted:()->Unit,
    tabletLandscape:Boolean=false
) {
    val feedback=LocalKidFeedback.current
    var replyRegion by remember(mode){mutableStateOf(cantoneseReplyRegions.first())}
    var batch by remember(mode,replyRegion){mutableIntStateOf(0)}
    val source=remember(mode,replyRegion) {
        CantoneseSiteGameBank.questions(mode).filter {
            mode!=CantoneseWebsiteGame.REPLY || it.region==replyRegion
        }
    }
    val round=remember(mode,batch,replyRegion,source) {
        when {
            mode==CantoneseWebsiteGame.TASK -> source
            mode==CantoneseWebsiteGame.REPLY -> {
                source.drop((batch*3)%source.size).take(3)
            }
            else -> source.shuffled(Random(batch+41))
        }
    }
    var index by remember(mode,batch,replyRegion){mutableIntStateOf(0)}
    var correct by remember(mode,batch,replyRegion){mutableIntStateOf(0)}
    var selected by remember(mode,batch,replyRegion,index){mutableStateOf<String?>(null)}
    var mistakes by remember(mode,batch,replyRegion,index){mutableIntStateOf(0)}
    var audioUnavailable by remember(mode,batch,replyRegion,index){mutableStateOf(false)}
    val current=round.getOrNull(index)
    LaunchedEffect(mode,current?.id,batch,replyRegion){voice.stop()}
    StoryCard(if(tabletLandscape)Modifier.fillMaxSize() else Modifier.fillMaxWidth(),mode.tint) {
        Column(Modifier.fillMaxWidth().then(if(tabletLandscape)
            Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("${mode.title} · ${mode.intro}",color=Ink,fontSize=21.sp,
                fontWeight=FontWeight.Black)
            if(mode==CantoneseWebsiteGame.REPLY) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    cantoneseReplyRegions.forEach {region->
                        KidPill(if(region==replyRegion)"✓ $region" else region,{
                            if(region!=replyRegion)replyRegion=region
                        },color=if(region==replyRegion)Color(0xFFDDF5E5) else Color.White)
                    }
                }
                Text("第 ${batch%5+1}/5 组",color=Sea,fontWeight=FontWeight.Bold)
            }
            if(mode==CantoneseWebsiteGame.TASK)
                Text("任务目标：在车站问路，确认去地铁入口的方向。",
                    color=Ink,fontSize=16.sp,fontWeight=FontWeight.Bold)
            if(current==null) {
                Text("🌟 这一轮完成啦！",color=Ink,fontSize=22.sp,
                    fontWeight=FontWeight.Black)
                Text("第一次就答对了 $correct / ${round.size} 题。",
                    color=Ink,fontSize=16.sp)
            } else {
                Text(if(mode==CantoneseWebsiteGame.TASK)
                    "第 ${index+1}/${round.size} 次对话" else "第 ${index+1}/${round.size} 题",
                    color=Sea,fontWeight=FontWeight.Bold)
                if(mode==CantoneseWebsiteGame.REPLY || mode==CantoneseWebsiteGame.TASK ||
                    !voice.ready || audioUnavailable || selected!=null)
                    Text(current.prompt,color=Ink,fontSize=18.sp,fontWeight=FontWeight.Bold)
                if(voice.ready) {
                    KidPill(if(mode==CantoneseWebsiteGame.REPLY) "🔊 听粤语题目"
                        else "🔊 播放粤语提示",{
                        voice.stop()
                        if(!voice.speakText(current.prompt,slow)) audioUnavailable=true
                    },Modifier.fillMaxWidth())
                } else {
                    Text("暂时没有可用的粤语语音；先看文字练习。",
                        color=Color(0xFF934A2D),fontSize=13.sp)
                }
                if(audioUnavailable)
                    Text("语音暂不可用，可继续阅读文字作答。",color=Color(0xFF934A2D),
                        fontSize=13.sp)
                val options=remember(current.id,batch) {
                    if(mode==CantoneseWebsiteGame.REPLY) current.options
                    else current.options.shuffled(Random(current.id.hashCode()+batch))
                }
                options.chunked(if(mode==CantoneseWebsiteGame.REPLY)1 else 2).forEach {pair->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                        pair.forEach {option->
                            val won=selected==current.answerId
                            val chosen=selected==option.id
                            val shade=when {
                                won && option.id==current.answerId -> Color(0xFFB9E8BD)
                                chosen -> Color(0xFFFFDDD0)
                                else -> Color.White
                            }
                            Column(Modifier.weight(1f).heightIn(min=62.dp)
                                .clip(RoundedCornerShape(17.dp)).background(shade)
                                .clickable(enabled=!won) {
                                    voice.stop()
                                    if(mode==CantoneseWebsiteGame.REPLY &&
                                        !voice.speakText(option.text,slow)) audioUnavailable=true
                                    if(option.id==current.answerId) {
                                        if(mistakes==0)correct++
                                        selected=option.id
                                        feedback?.success()
                                    } else {
                                        selected=option.id
                                        mistakes++
                                        feedback?.error()
                                    }
                                }.padding(11.dp),horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.Center) {
                                if(mode==CantoneseWebsiteGame.REPLY)
                                    Text(option.id.uppercase(),color=Sea,
                                        fontSize=12.sp,fontWeight=FontWeight.Bold)
                                Text(option.text,color=Ink,fontSize=16.sp,
                                    fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                            }
                        }
                        if(pair.size==1 && mode!=CantoneseWebsiteGame.REPLY)
                            Spacer(Modifier.weight(1f))
                    }
                }
                if(selected==current.answerId)
                    Text("✅ 答对啦！${current.explanation}",color=Ink,fontSize=14.sp)
                else if(mistakes>0)
                    Text(if(mode==CantoneseWebsiteGame.REPLY)
                        "差一点，再看看情境并试试其他回答。"
                        else "差一点，再听一遍并试试其他回答。",
                        color=Ink,fontSize=14.sp)
            }
        }
        if(current==null) {
            LargeAction("🔁 再玩一轮",{
                voice.stop();batch++;feedback?.tap()
            },Modifier.fillMaxWidth())
        } else if(selected==current.answerId) {
            LargeAction(if(index==round.lastIndex)"🏅 完成这一轮" else "➡ 下一题",{
                voice.stop()
                if(index==round.lastIndex)onCompleted()
                index++;feedback?.tap()
            },Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun CantoneseSentenceGameScreen(
    slow:Boolean,
    voice:CantoneseSpeaker,
    onCompleted:()->Unit,
    tabletLandscape:Boolean=false
) {
    val feedback=LocalKidFeedback.current
    var batch by remember{mutableIntStateOf(0)}
    val round=remember(batch){CantoneseSiteGameBank.sentences.shuffled(Random(batch+83))}
    var index by remember(batch){mutableIntStateOf(0)}
    var correct by remember(batch){mutableIntStateOf(0)}
    var selected by remember(batch,index){mutableStateOf(emptyList<Int>())}
    var checked by remember(batch,index){mutableStateOf<Boolean?>(null)}
    var mistakes by remember(batch,index){mutableIntStateOf(0)}
    val current=round.getOrNull(index)
    var revealSentence by remember(current?.id,batch){mutableStateOf(false)}
    val tileOrder=remember(current?.id,batch){current?.pieces?.indices?.shuffled(Random(batch+107)).orEmpty()}
    LaunchedEffect(current?.id,batch){voice.stop()}
    StoryCard(if(tabletLandscape)Modifier.fillMaxSize() else Modifier.fillMaxWidth(),Color(0xFFE4F6EC)) {
        Column(Modifier.fillMaxWidth().then(if(tabletLandscape)
            Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("句子拼拼乐 · 把词语排成一句粤语。",color=Ink,fontSize=21.sp,
                fontWeight=FontWeight.Black)
            if(current==null) {
                Text("🌟 这一轮完成啦！",color=Ink,fontSize=22.sp,fontWeight=FontWeight.Black)
                Text("第一次就拼对了 $correct / ${round.size} 题。",color=Ink,fontSize=16.sp)
            } else {
                Text("第 ${index+1}/${round.size} 题",color=Sea,fontWeight=FontWeight.Bold)
                Text("点选下面的词块，排成一句自然的粤语。",color=Ink,fontSize=16.sp)
                if(voice.ready)
                    KidPill("🔊 听示范句",{
                        if(!voice.speakText(current.answer,slow)) {
                            revealSentence=true
                            feedback?.error()
                        }
                    },Modifier.fillMaxWidth())
                else Text("粤语语音暂不可用，先看目标句：${current.answer}",
                    color=Color(0xFF934A2D),fontSize=14.sp)
                if(revealSentence)
                    Text("示范句：${current.answer}",color=Color(0xFF934A2D),fontSize=14.sp)
                Text("已选词块（点一下可放回）：",color=Ink,fontSize=14.sp,
                    fontWeight=FontWeight.Bold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    selected.forEach {pieceIndex->
                        KidPill(current.pieces[pieceIndex],{
                            selected=selected.filterNot {it==pieceIndex};checked=null
                        })
                    }
                }
                Text("可选词块：",color=Ink,fontSize=14.sp,fontWeight=FontWeight.Bold)
                tileOrder.filterNot(selected::contains).chunked(3).forEach {row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        row.forEach {pieceIndex->
                            KidPill(current.pieces[pieceIndex],{
                                selected=selected+pieceIndex;checked=null
                            },Modifier.weight(1f))
                        }
                        repeat(3-row.size){Spacer(Modifier.weight(1f))}
                    }
                }
                when(checked) {
                    true -> Text("✅ 拼对啦！${current.answer}",color=Ink,fontSize=15.sp,
                        fontWeight=FontWeight.Bold)
                    false -> Text("词块顺序还不对，点已选词块放回后再试。",
                        color=Color(0xFF934A2D),fontSize=14.sp)
                    null -> Unit
                }
            }
        }
        if(current==null) {
            LargeAction("🔁 再拼一轮",{batch++;feedback?.tap()},Modifier.fillMaxWidth())
        } else if(checked==true) {
            LargeAction(if(index==round.lastIndex)"🏅 完成这一轮" else "➡ 下一题",{
                voice.stop()
                if(index==round.lastIndex)onCompleted()
                index++;feedback?.tap()
            },Modifier.fillMaxWidth())
        } else {
            LargeAction("检查句子",{
                if(selected.size==current.pieces.size) {
                    val answer=selected.joinToString(""){current.pieces[it]}
                    checked=answer==current.answer
                    if(checked==true) {
                        if(mistakes==0)correct++
                        feedback?.success()
                    } else {
                        mistakes++;feedback?.error()
                    }
                }
            },Modifier.fillMaxWidth(),enabled=selected.size==current.pieces.size)
        }
    }
}

@Composable
internal fun CantonesePriceGameScreen(
    slow:Boolean,
    voice:CantoneseSpeaker,
    onCompleted:()->Unit,
    tabletLandscape:Boolean=false
) {
    val feedback=LocalKidFeedback.current
    var batch by remember{mutableIntStateOf(0)}
    val round=remember(batch){CantoneseSiteGameBank.prices.shuffled(Random(batch+131))}
    var index by remember(batch){mutableIntStateOf(0)}
    var correct by remember(batch){mutableIntStateOf(0)}
    var amount by remember(batch,index){mutableStateOf("")}
    var message by remember(batch,index){mutableStateOf<String?>(null)}
    var mistakes by remember(batch,index){mutableIntStateOf(0)}
    val current=round.getOrNull(index)
    LaunchedEffect(current?.id,batch){voice.stop()}
    StoryCard(if(tabletLandscape)Modifier.fillMaxSize() else Modifier.fillMaxWidth(),Color(0xFFE5F8EC)) {
        Column(Modifier.fillMaxWidth().then(if(tabletLandscape)
            Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("听听多少钱 · 听价格，填金额。",color=Ink,fontSize=21.sp,
                fontWeight=FontWeight.Black)
            if(current==null) {
                Text("🌟 这一轮完成啦！",color=Ink,fontSize=22.sp,fontWeight=FontWeight.Black)
                Text("第一次就答对了 $correct / ${round.size} 题。",color=Ink,fontSize=16.sp)
            } else {
                Text("第 ${index+1}/${round.size} 题",color=Sea,fontWeight=FontWeight.Bold)
                Text("听清楚价格后，输入金额（例如 23 或 23.50）。",
                    color=Ink,fontSize=16.sp)
                if(voice.ready) {
                    KidPill("🔊 播放价格",{
                        if(!voice.speakText(current.prompt,slow)) {
                            message="语音暂不可用：${current.prompt}"
                        }
                    },Modifier.fillMaxWidth())
                } else {
                    Text("粤语语音暂不可用，先看文字练习：${current.prompt}",
                        color=Color(0xFF934A2D),fontSize=14.sp)
                }
                if(message?.startsWith("语音暂不可用")==true)
                    Text(message!!,color=Color(0xFF934A2D),fontSize=14.sp)
                OutlinedTextField(value=amount,onValueChange={value->
                    if(value.length<=10 && value.matches(Regex("[0-9]*([.,][0-9]{0,2})?")))
                        amount=value
                    message=null
                },modifier=Modifier.fillMaxWidth(),singleLine=true,
                    label={Text("金额")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
                message?.let {Text(it,color=if(it.startsWith("✅"))Sea else Color(0xFF934A2D),
                    fontSize=14.sp,fontWeight=FontWeight.Bold)}
            }
        }
        if(current==null) {
            LargeAction("🔁 再听一轮",{batch++;feedback?.tap()},Modifier.fillMaxWidth())
        } else if(message?.startsWith("✅")==true) {
            LargeAction(if(index==round.lastIndex)"🏅 完成这一轮" else "➡ 下一题",{
                voice.stop()
                if(index==round.lastIndex)onCompleted()
                index++;feedback?.tap()
            },Modifier.fillMaxWidth())
        } else {
            LargeAction("核对金额",{
                val entered=amountToMinorUnits(amount)
                when {
                    entered==null -> message="请输入有效金额，例如 23 或 23.50。"
                    entered==current.amountMinorUnits -> {
                        if(mistakes==0)correct++
                        message="✅ 答对啦！价格是 ${formatAmount(entered)} 蚊。"
                        feedback?.success()
                    }
                    else -> {
                        mistakes++
                        message="差一点，再听一次或修改金额。"
                        feedback?.error()
                    }
                }
            },Modifier.fillMaxWidth(),enabled=amount.isNotBlank())
        }
    }
}

private fun amountToMinorUnits(value:String):Long? {
    val normalized=value.trim().replace(',','.')
    if(!normalized.matches(Regex("[0-9]+(\\.[0-9]{1,2})?")))return null
    return runCatching {BigDecimal(normalized).movePointRight(2).longValueExact()}.getOrNull()
}

private fun formatAmount(minorUnits:Long):String =
    "${minorUnits/100}.${(minorUnits%100).toString().padStart(2,'0')}"

private fun formatTime(minutes:Int):String =
    "%02d:%02d".format(minutes/60,minutes%60)
