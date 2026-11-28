package com.aggro1025.jeopda.ui.folder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aggro1025.jeopda.ui.Lcd
import com.aggro1025.jeopda.ui.Scr
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 폴더폰 화면(디스플레이)에 뜨는 화면들 */
sealed interface Screen {
    data object Home : Screen
    data class Menu(val selected: Int = 1) : Screen
    data class UnlockMethod(val selected: Int = 0) : Screen
    /** step 0: 시간(시계), step 1: 사유 */
    data class UnlockInput(
        val method: com.aggro1025.jeopda.session.UnlockMethod,
        val minutes: Int = 30,
        val step: Int = 0,
    ) : Screen
    data class Dialer(val number: String = "") : Screen
    /** 1분 기다린 뒤 해제 (광고를 불러오지 못했을 때) */
    data class UnlockWait(val minutes: Int, val reason: String, val adFailed: Boolean = false) : Screen
    /** 폴더폰 모드에서 쓸 수 없는 앱을 열려고 했을 때 */
    data class Blocked(val label: String) : Screen
    /** 보상형 광고 2개 보기 */
    data class UnlockAd(val minutes: Int, val reason: String, val attempt: Int = 0) : Screen
    /** keys: 초성 찾기로 누른 숫자들, selected: 목록에서 고른 줄 */
    data class Contacts(val keys: String = "", val selected: Int = 0) : Screen
}

data class SoftLabels(val left: String = "", val center: String = "", val right: String = "")

private val TextShadow = Shadow(Color(0xB3000000), Offset(0f, 2f), 4f)

/** 배경화면 위 글자 (그림자로 어느 배경에서도 읽히게) */
fun scrText(size: TextUnit, color: Color = Scr.text, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size, color = color, fontWeight = weight, shadow = TextShadow)

/**
 * 전체 화면: 위쪽 약 55%는 디스플레이, 아래쪽 약 45%는 폴더폰 버튼부.
 * 디스플레이는 위 상태 표시줄, 제목, 내용 순서로 그린다. 소프트키 이름은 버튼부에 표시한다.
 * 키보드(사유 입력)가 올라오면 버튼부를 숨기고 디스플레이가 남은 공간을 쓴다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderPhone(
    title: String?,
    soft: SoftLabels,
    onKey: (FolderKey) -> Unit,
    /** 홈 화면: 테마의 큰 그림(장면)을 보여 준다 */
    scene: Boolean = false,
    /** 미리보기처럼 화면 일부에 그릴 때는 시스템 여백을 빼지 않는다 */
    applyInsets: Boolean = true,
    display: @Composable () -> Unit,
) {
    if (com.aggro1025.jeopda.ui.theme.LocalFolderTheme.current == com.aggro1025.jeopda.ui.theme.FolderTheme.CLOUD_CAT) {
        CloudCatPhone(title, soft, onKey, scene, applyInsets, display)
        return
    }
    val imeVisible = WindowInsets.isImeVisible
    Column(
        Modifier
            .fillMaxSize()
            .background(Lcd.body)
            .then(if (applyInsets) Modifier.safeDrawingPadding() else Modifier),
    ) {
        Box(
            Modifier
                .weight(if (imeVisible) 1f else 0.55f)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Lcd.bodyEdge, RoundedCornerShape(16.dp)),
        ) {
            Wallpaper()
            Column(Modifier.fillMaxSize()) {
                StatusBar()
                if (title != null) {
                    Text(
                        title,
                        style = scrText(17.sp, weight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 14.dp, top = 6.dp, end = 14.dp),
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth()) { display() }
            }
        }
        if (!imeVisible) {
            Keypad(onKey = onKey, modifier = Modifier.weight(0.45f), soft = soft)
        }
    }
}

/** 어두운 남색 그라데이션 + 은은한 곡선 무늬 */
@Composable
private fun Wallpaper() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Scr.wallTop, Scr.wallBottom)))
        // 오른쪽 아래 큰 원호 (빛 번짐)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x335AB0FF), Color(0x00000000)),
                center = Offset(w * 1.05f, h * 1.1f),
                radius = w * 1.1f,
            ),
            radius = w * 1.1f,
            center = Offset(w * 1.05f, h * 1.1f),
        )
        drawCircle(
            color = Color(0x30FFFFFF),
            radius = w * 0.95f,
            center = Offset(w * 1.15f, h * 1.12f),
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(
            color = Color(0x18FFFFFF),
            radius = w * 0.80f,
            center = Offset(w * 1.15f, h * 1.12f),
            style = Stroke(width = 1.dp.toPx()),
        )
        // 왼쪽 위 원호
        drawArc(
            color = Color(0x1AFFFFFF),
            startAngle = -20f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(-w * 0.9f, -h * 0.35f),
            size = Size(w * 1.5f, h * 0.9f),
            style = Stroke(width = 1.5.dp.toPx()),
        )
        // 아래쪽 은은한 반사광
        drawOval(
            brush = Brush.verticalGradient(
                listOf(Color(0x14FFFFFF), Color(0x00FFFFFF)),
                startY = h * 0.55f,
                endY = h,
            ),
            topLeft = Offset(-w * 0.4f, h * 0.55f),
            size = Size(w * 1.1f, h * 0.7f),
        )
    }
}

/** 실제 폴더폰처럼 화면 맨 위의 얇은 상태 표시줄 (배터리 왼쪽, 시계 오른쪽) */
@Composable
internal fun StatusBar() {
    val battery = rememberBattery()
    val now = rememberNow()
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Scr.statusBg, Color.Transparent)))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryIcon(battery.percent, battery.charging)
        Text(
            " ${battery.percent}%",
            style = scrText(12.sp, Scr.dim),
        )
        Spacer(Modifier.weight(1f))
        Text(
            SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(now)),
            style = scrText(13.sp, weight = FontWeight.Bold),
        )
    }
}

@Composable
private fun BatteryIcon(percent: Int, charging: Boolean) {
    val bars = ((percent + 19) / 25).coerceIn(0, 4)
    val color = if (percent <= 15 && !charging) Scr.warn else Scr.text
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .border(1.5.dp, color, RoundedCornerShape(2.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        ) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .width(3.5.dp)
                        .height(7.dp)
                        .background(if (i < bars) color else Color.Transparent),
                )
            }
        }
        Box(Modifier.width(2.dp).height(5.dp).background(color))
    }
}

/** 1초마다 갱신되는 현재 시각 */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L - now % 1000L)
        }
    }
    return now
}

private data class Battery(val percent: Int, val charging: Boolean)

@Composable
private fun rememberBattery(): Battery {
    val context = LocalContext.current
    var state by remember { mutableStateOf(Battery(0, false)) }
    if (LocalInspectionMode.current) return Battery(87, false) // Android Studio 미리보기용
    DisposableEffect(context) {
        fun read(i: Intent?): Battery? {
            i ?: return null
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level < 0 || scale <= 0) return null
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            return Battery(level * 100 / scale, charging)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                read(i)?.let { state = it }
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        read(sticky)?.let { state = it }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
