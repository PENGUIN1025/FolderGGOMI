package com.aggro1025.jeopda.ui.folder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.ui.theme.CC
import com.aggro1025.jeopda.ui.theme.CloudCatScene
import com.aggro1025.jeopda.ui.theme.FolderTheme
import com.aggro1025.jeopda.ui.theme.InkStroke
import com.aggro1025.jeopda.ui.theme.LocalFolderTheme
import com.aggro1025.jeopda.ui.theme.Pts
import com.aggro1025.jeopda.ui.theme.densify
import com.aggro1025.jeopda.ui.theme.ellipsePts
import com.aggro1025.jeopda.ui.theme.leaf
import com.aggro1025.jeopda.ui.theme.sample
import kotlin.math.PI
import kotlin.math.min
import kotlin.random.Random

/*
 * 구름 고양이 테마의 폴더폰: 흰 종이 몸체 + 가위로 오려 테이프로 붙인 밤하늘 화면 + 펜으로 그린 버튼부.
 * 버튼 동작과 배치는 기본 테마와 같다. 버튼부 선은 떨리지 않게 고정해 배터리를 아낀다.
 */

/** 가위로 오린 종이 가장자리 (dp 좌표). 같은 크기면 늘 같은 모양 */
private fun cutPoints(w: Float, h: Float): Pts {
    val rnd = Random(11)
    val corners = listOf(1.5f to 1.5f, w - 1.5f to 0.8f, w - 0.8f to h - 1.5f, 0.8f to h - 0.8f)
    val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
    for (k in corners.indices) {
        val (ax, ay) = corners[k]
        val (bx, by) = corners[(k + 1) % corners.size]
        val len = kotlin.math.hypot(bx - ax, by - ay)
        val n = maxOf(2, (len / 22f).toInt())
        val nx = -(by - ay) / len; val ny = (bx - ax) / len
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val j = if (i == 0) 0f else (rnd.nextFloat() - 0.5f) * 1.4f
            xs += ax + (bx - ax) * t + nx * j
            ys += ay + (by - ay) * t + ny * j
        }
    }
    xs += xs[0]; ys += ys[0]
    return Pts(xs.toFloatArray(), ys.toFloatArray())
}

private fun Pts.toPath(scale: Float = 1f) = Path().apply {
    moveTo(x[0] * scale, y[0] * scale)
    for (i in 1 until n) lineTo(x[i] * scale, y[i] * scale)
    close()
}

private class CutShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val d = density.density
        return Outline.Generic(cutPoints(size.width / d, size.height / d).toPath(d))
    }
}

private fun inkText(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = CC.ink) =
    TextStyle(fontSize = size, color = color, fontWeight = weight)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CloudCatPhone(
    title: String?,
    soft: SoftLabels,
    onKey: (FolderKey) -> Unit,
    scene: Boolean,
    applyInsets: Boolean,
    display: @Composable () -> Unit,
) {
    val imeVisible = WindowInsets.isImeVisible
    Column(
        Modifier
            .fillMaxSize()
            .drawBehind { drawPaper() }
            .then(if (applyInsets) Modifier.safeDrawingPadding() else Modifier),
    ) {
        Box(
            Modifier
                .weight(if (imeVisible) 1f else 0.55f)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 2.dp),
        ) {
            val shape = remember { CutShape() }
            // 종이 그림자
            Box(Modifier.fillMaxSize().drawBehind {
                translate(2.5.dp.toPx(), 3.5.dp.toPx()) {
                    drawPath(cutPoints(size.width / density, size.height / density).toPath(density), CC.ink, 0.12f)
                }
            })
            Box(Modifier.fillMaxSize().clip(shape)) {
                CloudCatScene(Modifier.fillMaxSize(), full = scene)
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
            // 테두리 선과 테이프
            Canvas(Modifier.fillMaxSize()) { drawPanelFrame() }
        }
        if (!imeVisible) {
            Hinge(Modifier.fillMaxWidth().height(14.dp))
            InkKeypad(onKey, Modifier.weight(0.45f), soft)
        }
    }
}

/** 흰 종이 + 아주 옅은 종이 결 */
private fun DrawScope.drawPaper() {
    drawRect(CC.paper)
    val rnd = Random(3)
    val step = 9.dp.toPx()
    var y = 0f
    while (y < size.height) {
        var x = rnd.nextFloat() * step
        while (x < size.width) {
            drawCircle(CC.ink, 0.5.dp.toPx() * (0.5f + rnd.nextFloat()), Offset(x, y + rnd.nextFloat() * step), alpha = 0.035f)
            x += step * (0.6f + rnd.nextFloat())
        }
        y += step
    }
}

private val frameCache = HashMap<Long, Pair<InkStroke, List<Pair<Path, InkStroke>>>>()

private fun DrawScope.drawPanelFrame() {
    val d = density
    val w = size.width / d; val h = size.height / d
    val key = (w.toInt().toLong() shl 20) or h.toInt().toLong()
    val (outline, tapes) = frameCache.getOrPut(key) {
        val rnd = Random(5)
        val o = InkStroke(cutPoints(w, h), closed = true, w = 1.2f, rnd = rnd)
        val tp = listOf(Triple(w / 2, 1f, -3f) to (64f to 15f), Triple(w - 8f, h - 6f, 38f) to (44f to 13f)).map { (pos, sz) ->
            val (tw, th) = sz
            val sb = StringBuilder("M${-tw / 2},${-th / 2}L${tw / 2},${-th / 2}")
            for (i in 1..6) sb.append("L${tw / 2 - (if (i % 2 == 1) 1.6f else 0f)},${-th / 2 + th * i / 6}")
            sb.append("L${-tw / 2},${th / 2}")
            for (i in 5 downTo 1) sb.append("L${-tw / 2 + (if (i % 2 == 1) 1.6f else 0f)},${-th / 2 + th * i / 6}")
            sb.append("Z")
            val path = com.aggro1025.jeopda.ui.theme.pathOf(sb.toString())
            val ink = InkStroke(sample(path, 1.5f), closed = true, w = 0.55f, alpha = 0.55f, rnd = rnd)
            // 위치·회전은 그릴 때 적용하므로 path는 원점 기준, 좌표는 따로 보관
            Path().apply { addPath(path) } to ink
        }
        o to tp
    }
    scale(d, d, Offset.Zero) {
        outline.draw(this, 0)
        val spots = listOf(Triple(w / 2, 1f, -3f), Triple(w - 8f, h - 6f, 38f))
        tapes.forEachIndexed { i, (path, ink) ->
            val (cx, cy, rot) = spots[i]
            translate(cx, cy) {
                rotate(rot, Offset.Zero) {
                    drawPath(path, Color(0xFFF3DDA9), 0.8f)
                    ink.draw(this, 0)
                }
            }
        }
    }
}

/** 화면과 버튼부를 잇는 힌지 + 폰줄 랜턴 */
@Composable
private fun Hinge(modifier: Modifier) {
    Canvas(modifier) {
        val d = density
        val w = size.width / d
        scale(d, d, Offset.Zero) {
            val l = 30f; val r = w - 30f
            val rr = RoundRect(l, 2f, r, 13f, CornerRadius(6f))
            val p = Path().apply { addRoundRect(rr) }
            drawPath(p, Color(0xFFF1ECF6))
            clipPath(p) {
                val lv = Path(); val rnd = Random(9)
                repeat(30) { lv.leaf(l + rnd.nextFloat() * (r - l), 9f + rnd.nextFloat() * 4f, 5f + rnd.nextFloat() * 4f, -0.3f, 0.55f) }
                drawPath(lv, Color(0xFFB7ACCB), 0.5f)
            }
            InkStroke(sample(p, 3f), closed = true, w = 1f, rnd = Random(4)).draw(this, 0)
            // 랜턴 (오른쪽 끝에 매달림)
            val lx = w - 14f
            InkStroke(densify(lx, 7f, lx + 0.6f, 18f, lx, 26f), w = 0.7f, rnd = Random(6)).draw(this, 0)
            drawCircle(
                Brush.radialGradient(0f to CC.window.copy(alpha = 0.8f), 1f to CC.window.copy(alpha = 0f), center = Offset(lx, 34f), radius = 15f),
                15f, Offset(lx, 34f),
            )
            val lan = Path().apply {
                moveTo(lx - 5f, 27f); lineTo(lx + 5f, 27f)
                quadraticTo(lx + 7f, 34f, lx + 5f, 41f); lineTo(lx - 5f, 41f)
                quadraticTo(lx - 7f, 34f, lx - 5f, 27f); close()
            }
            drawPath(lan, CC.window)
            drawRect(CC.band, Offset(lx - 4.5f, 25f), Size(9f, 2.5f))
            drawRect(CC.band, Offset(lx - 4.5f, 40.5f), Size(9f, 2.5f))
            InkStroke(sample(lan, 1f), closed = true, w = 0.7f, rnd = Random(8)).draw(this, 0)
        }
    }
}

// ---------------- 펜으로 그린 버튼부 ----------------

@Composable
internal fun InkKeypad(onKey: (FolderKey) -> Unit, modifier: Modifier = Modifier, soft: SoftLabels = SoftLabels()) {
    Column(modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, bottom = 6.dp, top = 10.dp)) {
        Row(Modifier.fillMaxWidth().weight(0.36f)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                KeyArea(FolderKey.SOFT_LEFT, onKey, Modifier.weight(1f), pressedColor = CC.pressed) { p -> InkSoftBar(soft.left, p) }
                KeyArea(FolderKey.CALL, onKey, Modifier.weight(1f), pressedColor = CC.pressed) { Handset(call = true) }
            }
            Box(Modifier.weight(1.2f).fillMaxHeight(), contentAlignment = Alignment.Center) { InkDPad(onKey, soft.center) }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                KeyArea(FolderKey.SOFT_RIGHT, onKey, Modifier.weight(1f), pressedColor = CC.pressed) { p -> InkSoftBar(soft.right, p) }
                KeyArea(FolderKey.END, onKey, Modifier.weight(1f), pressedColor = CC.pressed) { Handset(call = false) }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val d = density
            scale(d, d, Offset.Zero) {
                InkStroke(densify(8f, 4f, size.width / d - 8f, 3.6f), w = 0.8f, alpha = 0.45f, taper = 1.2f, rnd = Random(2)).draw(this, 0)
            }
        }
        val rows = listOf(
            listOf(FolderKey.D1, FolderKey.D2, FolderKey.D3),
            listOf(FolderKey.D4, FolderKey.D5, FolderKey.D6),
            listOf(FolderKey.D7, FolderKey.D8, FolderKey.D9),
            listOf(FolderKey.STAR, FolderKey.D0, FolderKey.HASH),
        )
        Column(Modifier.fillMaxWidth().weight(0.64f)) {
            rows.forEachIndexed { r, row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    row.forEachIndexed { c, k ->
                        KeyArea(k, onKey, Modifier.weight(1f).fillMaxHeight(), pressedColor = Color.Transparent) { pressed ->
                            InkKey(k, pressed, seed = r * 3 + c)
                        }
                    }
                }
            }
        }
    }
}

private val JAMO_INK = mapOf(1 to "ㅣ", 2 to "ㆍ", 3 to "ㅡ", 4 to "ㄱㅋ", 5 to "ㄴㄹ", 6 to "ㄷㅌ", 7 to "ㅂㅍ", 8 to "ㅅㅎ", 9 to "ㅈㅊ", 0 to "ㅇㅁ")

@Composable
private fun BoxScope.InkKey(k: FolderKey, pressed: Boolean, seed: Int) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp, vertical = 4.dp)
            .drawWithCache {
                val d = density
                val w = size.width / d; val h = size.height / d
                val rr = RoundRect(0.5f, 0.5f, w - 0.5f, h - 0.5f, CornerRadius(min(14f, h / 2.6f)))
                val shape = Path().apply { addRoundRect(rr) }
                val rnd = Random(100 + seed)
                val ink = InkStroke(sample(shape, 3f), closed = true, w = 0.95f, rnd = rnd)
                val lv = Path()
                repeat(9) { lv.leaf(6f + rnd.nextFloat() * (w - 12f), h - 9f + rnd.nextFloat() * 9f, 5f + rnd.nextFloat() * 4f, -0.45f, 0.4f + rnd.nextFloat() * 0.3f) }
                onDrawBehind {
                    scale(d, d, Offset.Zero) {
                        translate(1.5f, 2.5f) { drawPath(shape, CC.ink, 0.1f) }
                        val dy = if (pressed) 1.2f else 0f
                        translate(0f, dy) {
                            drawPath(shape, if (pressed) CC.pressed else Color.White)
                            clipPath(shape) { drawPath(lv, CC.keyShade, 0.5f) }
                            ink.draw(this, 0)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = if (pressed) 2.dp else 0.dp)) {
            Text(k.char.toString(), style = inkText(23.sp, FontWeight.SemiBold))
            k.digit?.let { JAMO_INK[it] }?.let {
                Text(it, style = inkText(11.sp, color = CC.ink.copy(alpha = 0.6f)), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun InkSoftBar(label: String, pressed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = inkText(15.sp, FontWeight.Bold), maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.size(54.dp, 8.dp)) {
            val d = density
            scale(d, d, Offset.Zero) {
                InkStroke(densify(2f, 4f, 52f, 4f), w = if (pressed) 4f else 3.2f, color = CC.band, taper = 1.5f, rnd = Random(12))
                    .draw(this, 0, alphaMul = if (label.isEmpty()) 0.3f else 1f)
            }
        }
    }
}

@Composable
private fun Handset(call: Boolean) {
    Canvas(Modifier.size(40.dp)) {
        val d = density
        scale(d, d, Offset.Zero) {
            val c = if (call) CC.call else CC.band
            val rnd = Random(if (call) 21 else 22)
            val parts = if (call) listOf("M10,8Q11,26 30,28" to 3.6f, "M6,11L14,4" to 3f, "M27,32L34,24" to 3f)
            else listOf("M6,25Q20,14 34,25" to 3.6f, "M3,21L8,29" to 3f, "M37,21L32,29" to 3f)
            parts.forEach { (p, w) -> InkStroke(sample(p, 1.5f), w = w, color = c, taper = 0.6f, rnd = rnd).draw(this, 0) }
        }
    }
}

@Composable
private fun InkDPad(onKey: (FolderKey) -> Unit, centerLabel: String) {
    Box(
        Modifier
            .fillMaxHeight(0.92f)
            .aspectRatio(1f)
            .drawWithCache {
                val d = density
                val s = size.width / d
                val cx = s / 2; val r = s / 2 - 1.5f
                val ring = Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - r, cx - r, cx + r, cx + r)) }
                val rnd = Random(31)
                val ringInk = InkStroke(ellipsePts(cx, cx, r, r, 0f, 0f, (2 * PI).toFloat(), 90), closed = true, w = 1.1f, rnd = rnd)
                val ok = r * 0.44f
                val okPath = Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - ok, cx - ok, cx + ok, cx + ok)) }
                val okInk = InkStroke(ellipsePts(cx, cx, ok, ok, 0f, 0f, (2 * PI).toFloat(), 60), closed = true, w = 1f, rnd = rnd)
                val shade = Path()
                repeat(30) {
                    val a = 0.1f + rnd.nextFloat() * 2.5f; val rr = r - 8f + rnd.nextFloat() * 8f
                    shade.leaf(cx + kotlin.math.cos(a) * rr, cx + kotlin.math.sin(a) * rr, 5f + rnd.nextFloat() * 3f, a + 1.5f, 0.6f)
                }
                val okShade = Path()
                repeat(12) {
                    val a = rnd.nextFloat() * 2.4f; val rr = ok * 0.3f + rnd.nextFloat() * ok * 0.7f
                    okShade.leaf(cx + kotlin.math.cos(a) * rr, cx + kotlin.math.sin(a) * rr, 4f + rnd.nextFloat() * 3f, -0.7f, 0.6f)
                }
                val a = r * 0.82f
                val chev = listOf(
                    densify(cx - 5.5f, cx - a + 5.5f, cx, cx - a, cx + 5.5f, cx - a + 5.5f),
                    densify(cx - 5.5f, cx + a - 5.5f, cx, cx + a, cx + 5.5f, cx + a - 5.5f),
                    densify(cx - a + 5.5f, cx - 5.5f, cx - a, cx, cx - a + 5.5f, cx + 5.5f),
                    densify(cx + a - 5.5f, cx - 5.5f, cx + a, cx, cx + a - 5.5f, cx + 5.5f),
                ).map { InkStroke(it, w = 1f, rnd = rnd) }
                onDrawBehind {
                    scale(d, d, Offset.Zero) {
                        drawPath(ring, Color(0xFFFBF8FF))
                        clipPath(ring) { drawPath(shade, Color(0xFFBBB0D0), 0.5f) }
                        ringInk.draw(this, 0)
                        chev.forEach { it.draw(this, 0) }
                        drawCircle(
                            Brush.radialGradient(0f to CC.window.copy(alpha = 0.6f), 1f to CC.window.copy(alpha = 0f), center = Offset(cx, cx), radius = ok * 1.45f),
                            ok * 1.45f, Offset(cx, cx),
                        )
                        drawPath(okPath, CC.window, 0.92f)
                        clipPath(okPath) { drawPath(okShade, Color(0xFFC98F22), 0.4f) }
                        okInk.draw(this, 0)
                    }
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Gap(); KeyArea(FolderKey.UP, onKey, Modifier.weight(1f), pressedColor = CC.pressed.copy(alpha = 0.7f)) {}; Gap()
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                KeyArea(FolderKey.LEFT, onKey, Modifier.weight(1f), pressedColor = CC.pressed.copy(alpha = 0.7f)) {}
                Gap()
                KeyArea(FolderKey.RIGHT, onKey, Modifier.weight(1f), pressedColor = CC.pressed.copy(alpha = 0.7f)) {}
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Gap(); KeyArea(FolderKey.DOWN, onKey, Modifier.weight(1f), pressedColor = CC.pressed.copy(alpha = 0.7f)) {}; Gap()
            }
        }
        KeyArea(
            FolderKey.OK, onKey,
            Modifier.align(Alignment.Center).fillMaxSize(0.42f).clip(CircleShape),
            shape = CircleShape,
            pressedColor = Color(0x33FFFFFF),
        ) { Text(centerLabel.ifEmpty { "확인" }, style = inkText(14.sp, FontWeight.Bold), maxLines = 1) }
    }
}

@Composable
private fun RowScope.Gap() = Box(Modifier.weight(1f))

/**
 * 설정의 테마 미리보기: 실제 홈 화면(384×832dp)을 줄여서 그린다. 그림은 실제처럼 움직이고, 화면을 누르면 반응한다.
 */
@Composable
fun ThemePreview(theme: FolderTheme, width: Dp, modifier: Modifier = Modifier) {
    val k = width / 384.dp
    Box(
        modifier
            .size(width, width * (832f / 384f))
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0x66FFFFFF), RoundedCornerShape(16.dp)),
    ) {
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(384.dp, 832.dp)
                .graphicsLayer {
                    scaleX = k; scaleY = k
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            CompositionLocalProvider(LocalFolderTheme provides theme) {
                FolderPhone(null, SoftLabels("해제", "메뉴", "설정"), onKey = {}, scene = true, applyInsets = false) {
                    HomeDisplay(3, notifBlockOn = true, onFixNotif = {})
                }
            }
        }
    }
}
