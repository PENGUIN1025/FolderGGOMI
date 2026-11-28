package com.aggro1025.jeopda.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.ui.ScrFixed
import com.aggro1025.jeopda.ui.folder.ThemePreview
import com.aggro1025.jeopda.ui.theme.FolderTheme

/** 테마 하나의 상태 */
data class ThemeItem(val theme: FolderTheme, val owned: Boolean, val applied: Boolean, val price: String?)

private fun t(size: TextUnit, color: Color = ScrFixed.text, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size, color = color, fontWeight = weight)

/**
 * 설정 > 폴더폰 테마. 위에서 테마를 고르면 가운데에 실제 홈 화면이 움직이는 그대로 보이고,
 * 아래 버튼으로 적용하거나 산다. 스크롤 없이 한 화면에 들어가게 미리보기 크기를 남은 높이에 맞춘다.
 */
@Composable
fun ThemeScreen(
    items: List<ThemeItem>,
    busy: Boolean,
    onBack: () -> Unit,
    onApply: (FolderTheme) -> Unit,
    onBuy: (FolderTheme) -> Unit,
    onRestore: () -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf((items.firstOrNull { it.applied && it.theme.paid } ?: items.firstOrNull { it.theme.paid } ?: items.first()).theme.id) }
    val sel = items.firstOrNull { it.theme.id == selectedId } ?: items.first()
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ScrFixed.wallTop, ScrFixed.wallBottom)))) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", style = t(26.sp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(horizontal = 8.dp))
                Text("폴더폰 테마", style = t(19.sp, weight = FontWeight.Bold))
            }
            // 테마 고르기
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { it ->
                    val on = it.theme.id == selectedId
                    val base = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    Column(
                        (if (on) base.background(Brush.verticalGradient(listOf(ScrFixed.accent, ScrFixed.accentDeep))) else base.background(Color(0x22FFFFFF)))
                            .clickable { selectedId = it.theme.id }
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(it.theme.label, style = t(15.sp, weight = FontWeight.Bold))
                        Text(
                            when {
                                it.applied -> "적용 중"
                                !it.theme.paid -> "무료"
                                it.owned -> "구매함"
                                else -> it.price ?: it.theme.fallbackPrice ?: ""
                            },
                            style = t(12.sp, if (on) ScrFixed.text else ScrFixed.dim),
                        )
                    }
                }
            }
            // 움직이는 미리보기 (남은 높이에 맞춤)
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val w = min(maxWidth, maxHeight * (384f / 832f))
                ThemePreview(sel.theme, w)
            }
            Text(sel.theme.note, style = t(13.sp, ScrFixed.dim), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            val label = when {
                busy -> "결제 창을 여는 중…"
                sel.applied -> "적용 중이에요"
                sel.owned -> "이 테마 적용하기"
                else -> "${sel.price ?: sel.theme.fallbackPrice}에 구매하기"
            }
            val enabled = !busy && !sel.applied
            val base = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            Box(
                (if (enabled) base.clickable { if (sel.owned) onApply(sel.theme) else onBuy(sel.theme) } else base)
                    .background(
                        if (enabled) Brush.verticalGradient(listOf(ScrFixed.accent, ScrFixed.accentDeep))
                        else Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))),
                    )
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = t(16.sp, if (enabled) ScrFixed.text else ScrFixed.dim, FontWeight.Bold)) }
            if (sel.theme.paid) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "한 번 사면 계속 써요 · 같은 구글 계정이면 다시 설치해도 그대로예요",
                        style = t(11.sp, ScrFixed.dim),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "구매 복원",
                        style = t(12.sp, ScrFixed.accent, FontWeight.Bold),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, ScrFixed.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onRestore)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}
