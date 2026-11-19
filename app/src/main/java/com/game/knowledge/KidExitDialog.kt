package com.game.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog

/** Math Planet's illustrated modal language, without Material AlertDialog UI. */
@Composable
fun KidExitDialog(isExitApp: Boolean, onStay: () -> Unit, onConfirm: () -> Unit) {
    val compact = LocalConfiguration.current.screenHeightDp < 530
    val shape = RoundedCornerShape(29.dp)
    Dialog(onDismissRequest = onStay) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 420.dp)
                .heightIn(max = (LocalConfiguration.current.screenHeightDp - 35).coerceAtLeast(210).dp)
                .shadow(12.dp, shape).clip(shape)
                .background(Brush.verticalGradient(listOf(
                    Color(0xFFFFF8CA), Color(0xFFDAF7EC), Color(0xFFFFE9D9)
                )))
                .border(2.dp, Color.White, shape)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = if (compact) 13.dp else 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 13.dp)
        ) {
            Text("✨ 万象探险家 ✨", color = Sea, fontWeight = FontWeight.Black)
            Text(if (isExitApp) "🧳" else "🦊", fontSize = if (compact) 57.sp else 76.sp)
            Text(
                if (isExitApp) "今天先探索到这里吗？" else "还要继续探索吗？",
                fontSize = if (compact) 22.sp else 25.sp,
                color = Ink, fontWeight = FontWeight.Black, textAlign = TextAlign.Center
            )
            Text(
                if (isExitApp) "学到的新知识会留在心里，下次继续探险！"
                else "现在返回首页，会结束这一轮学习或对决，未完成的进度不会保留。",
                fontSize = 15.sp, lineHeight = 23.sp,
                color = Ink.copy(alpha = .82f), textAlign = TextAlign.Center
            )
            LargeAction("🌈 继续探险", onStay, Modifier.fillMaxWidth(), Sea)
            KidPill(
                if (isExitApp) "👋 退出应用" else "🏠 返回首页",
                onConfirm, modifier = Modifier.fillMaxWidth(), color = Color(0xFFFFDBCA),
                height = if (compact) 48.dp else 53.dp
            )
        }
    }
}
