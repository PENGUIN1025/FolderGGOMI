package com.aggro1025.jeopda.ui.theme

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val ACTIVE_MS = 10_000L

/** 움직임 상태. 누르거나 화면이 켜지면 10초 동안 움직이고, 그 뒤에는 별만 천천히 반짝인다 */
class CloudCatState {
    class Meteor(var x: Float, var y: Float, val vx: Float, val vy: Float, var age: Float = 0f, val life: Float = 1.1f)
    class Part(val kind: Int, var x: Float, var y: Float, var vx: Float, var vy: Float, var rot: Float, val vr: Float, var age: Float, val life: Float, val ph: Float, val size: Float)

    val frame = mutableLongStateOf(0L)
    var t = 0f
    var energy = 1f
    var tick = 0
    private var activeUntil = 0L
    private var lastWob = 0L
    private val rnd = Random(SystemClock.uptimeMillis())
    private fun r(a: Float, b: Float) = a + (b - a) * rnd.nextFloat()

    var breath = 0f
    var headA = -4f
    var eyeDX = 0f
    var eyeDY = 0f
    var lid = 0f
    var earL = 0f
    var earR = 0f
    var jumpY = 0f
    var tramX = 0f
    var tramY = 0f
    private var jumpP = -1f
    private var earTimer = 3f
    private var earP = -1f
    private var earSide = 0
    private var blinkTimer = 2f
    private var blinkP = -1f
    private var blinkTwice = false
    private var tramShake = 0f
    private var nextMeteor = 4f
    private var lookX = 0f
    private var lookY = 0f
    private var lookUntil = 0L
    private var excitedUntil = 0L

    val meteors = ArrayList<Meteor>()
    val parts = ArrayList<Part>()
    val tailFill = Path()
    val scarfFill = Path()
    var tailPts = Pts(FloatArray(2), FloatArray(2))
    var scarfPts = Pts(FloatArray(2), FloatArray(2))

    fun wake(now: Long) { activeUntil = now + ACTIVE_MS }
    fun busy(now: Long) = now < activeUntil || energy > 0.02f || meteors.isNotEmpty() || parts.isNotEmpty() || jumpP >= 0f

    /** 화면 그림 좌표로 누른 곳 */
    fun tap(x: Float, y: Float, now: Long, still: Boolean) {
        wake(now)
        lookX = x; lookY = y; lookUntil = now + 4000
        if (still) return
        val cx = CloudCatArt.CAT_X; val cy = CloudCatArt.CAT_Y
        when {
            x in cx - 4..cx + 132 && y in cy..cy + 100 -> {
                excitedUntil = now + 2500
                earTimer = 0f
                if (jumpP < 0f) {
                    jumpP = 0f
                    repeat(4) { i -> parts += Part(2, cx + 104 + r(-8f, 8f), cy + 8, r(-10f, 10f), -34f, 0f, 0f, -i * 0.12f, 1.5f, r(0f, 6f), 1f) }
                }
            }
            x in 150f..230f && y in 268f..360f -> {
                tramShake = 0.55f
                repeat(3) { i -> parts += Part(1, 190 + r(-3f, 3f), 276f, r(-6f, 6f), -26f, 0f, 0f, -i * 0.22f, 1.3f, 0f, 1f) }
            }
            hypot(x - 86f, y - 120f) < 42f -> burst(86f, 120f, 12, 1f)
            else -> {
                burst(x, y, 6, 0.7f)
                meteor(x + 40, y - 34)
            }
        }
    }

    private fun burst(x: Float, y: Float, n: Int, size: Float) {
        repeat(n) { i ->
            val a = (i.toFloat() / n) * 2 * PI.toFloat() + r(-0.2f, 0.2f)
            val sp = r(40f, 90f) * size
            parts += Part(0, x, y, cos(a) * sp, sin(a) * sp, r(0f, 90f), r(-200f, 200f), 0f, 0.9f, 0f, r(2.5f, 4.5f) * size)
        }
    }

    private fun meteor(x: Float, y: Float) {
        meteors += Meteor(x.coerceIn(40f, 372f), y.coerceIn(18f, 200f), -190f, 105f)
    }

    fun step(dtc: Float, now: Long, still: Boolean) {
        val resting = still || now > activeUntil
        energy += ((if (resting) 0f else 1f) - energy) * (1 - kotlin.math.exp(-dtc * 1.6f))
        val e = if (still) 0f else energy
        t += dtc * (if (still) 0f else 0.35f + 0.65f * e)

        // 전차
        tramShake = max(0f, tramShake - dtc)
        tramX = if (tramShake > 0f) sin(now / 1000f * 60f) * 2.2f * (tramShake / 0.55f) else 0f
        tramY = sin(t * 3f) * 0.7f * e

        // 별똥별
        if (!still && e > 0.5f && t > nextMeteor) {
            meteor(r(250f, 350f), r(24f, 70f))
            nextMeteor = t + r(7f, 13f)
        }
        meteors.removeAll { m ->
            m.age += dtc; m.x += m.vx * dtc; m.y += m.vy * dtc
            m.age >= m.life
        }
        parts.removeAll { q ->
            q.age += dtc
            if (q.age >= 0f) {
                if (q.kind == 0) { q.vx *= 0.94f; q.vy = q.vy * 0.94f + 18 * dtc; q.rot += q.vr * dtc }
                q.x += q.vx * dtc; q.y += q.vy * dtc
            }
            q.age >= q.life
        }

        // 고양이: 숨쉬기
        breath = sin(t * 2.3f) * e
        // 바라볼 곳: 누른 곳 → 별똥별 → 오른쪽 위 하늘
        var tx = 300f; var ty = 90f
        if (now < lookUntil) { tx = lookX; ty = lookY } else meteors.firstOrNull()?.let { tx = it.x; ty = it.y }
        val hx = CloudCatArt.CAT_X + 104; val hy = CloudCatArt.CAT_Y + 34
        val ang = atan2(ty - hy, tx - hx) * 180f / PI.toFloat()
        val want = (ang * 0.32f).coerceIn(-22f, 10f) * (0.25f + 0.75f * e) - 4f
        headA += (want - headA) * (1 - kotlin.math.exp(-dtc * 5f))
        val dist = hypot(tx - hx, ty - hy).let { if (it > 0f) it else 1f }
        eyeDX += ((tx - hx) / dist * 1.4f - eyeDX) * (1 - kotlin.math.exp(-dtc * 8f))
        eyeDY += ((ty - hy) / dist * 1.2f - eyeDY) * (1 - kotlin.math.exp(-dtc * 8f))
        // 눈 깜빡임 (가끔 두 번)
        blinkTimer -= dtc
        if (blinkTimer <= 0f && blinkP < 0f) { blinkP = 0f; blinkTwice = rnd.nextFloat() < 0.25f; blinkTimer = r(2.4f, 6f) }
        lid = 0f
        if (blinkP >= 0f) {
            blinkP += dtc / 0.15f
            lid = sin(min(1f, blinkP) * PI.toFloat())
            if (blinkP >= 1f) { if (blinkTwice) { blinkTwice = false; blinkP = -0.6f } else blinkP = -1f }
        } else if (blinkP > -1f && blinkP < -0.5f) {
            blinkP += dtc / 0.15f
            if (blinkP >= -0.01f) blinkP = 0f
        }
        // 귀 쫑긋
        val excited = now < excitedUntil
        earTimer -= dtc * (if (excited) 3f else 1f)
        if (earTimer <= 0f && earP < 0f && e > 0.2f) { earP = 0f; earSide = if (rnd.nextBoolean()) 0 else 1; earTimer = r(3f, 8f) }
        var earA = 0f
        if (earP >= 0f) { earP += dtc / 0.22f; earA = sin(min(1f, earP) * PI.toFloat()) * 16f; if (earP >= 1f) earP = -1f }
        earL = if (earSide == 0) -earA else 0f
        earR = if (earSide == 1) earA else 0f
        // 점프
        jumpY = 0f
        if (jumpP >= 0f) { jumpP += dtc / 0.6f; jumpY = -20f * sin(min(1f, jumpP) * PI.toFloat()); if (jumpP >= 1f) jumpP = -1f }
        // 꼬리: 물결이 끝으로 전해지는 리본
        val tw = if (excited) 6.5f else 2.2f
        val ta = (if (excited) 0.34f else 0.22f) * (0.15f + 0.85f * e)
        ribbon(20f, 66f, 22, 84f, tailFill, { s -> (165f * (1 - s).pow(3)) * PI.toFloat() / 180f - 0.1f * s + ta * sin(t * tw - 5 * s) * s.pow(1.3f) },
            { s -> 7.2f * (1 - 0.45f * s) * sqrt(min(1f, (1 - s) * 7)) + 0.6f }).also { tailPts = it }
        // 목도리 꼬리: 바람에 펄럭
        ribbon(91f, 52f, 10, 27f, scarfFill, { s -> (186f - 18f * s) * PI.toFloat() / 180f + (0.18f + 0.4f * e) * sin(t * 8 - 6 * s) * s },
            { s -> 5.6f - 1.6f * s }).also { scarfPts = it }

        // 선 떨림: 움직이는 동안 초당 6번
        if (e > 0.05f && now - lastWob > 166) { lastWob = now; tick = (tick + 1) % 1000 }
    }

    private fun ribbon(x: Float, y: Float, n: Int, len: Float, fill: Path, theta: (Float) -> Float, width: (Float) -> Float): Pts {
        val cx = FloatArray(n + 1); val cy = FloatArray(n + 1)
        var px = x; var py = y
        for (i in 0..n) {
            val s = i.toFloat() / n
            cx[i] = px; cy[i] = py
            val th = theta(s)
            px += cos(th) * len / n; py += sin(th) * len / n
        }
        val ox = FloatArray(2 * n + 2); val oy = FloatArray(2 * n + 2)
        for (i in 0..n) {
            val a = max(0, i - 1); val b = min(n, i + 1)
            var tx = cx[b] - cx[a]; var ty = cy[b] - cy[a]
            val tl = hypot(tx, ty).let { if (it > 0f) it else 1f }
            tx /= tl; ty /= tl
            val w = width(i.toFloat() / n) / 2
            ox[i] = cx[i] - ty * w; oy[i] = cy[i] + tx * w
            ox[2 * n + 1 - i] = cx[i] + ty * w; oy[2 * n + 1 - i] = cy[i] - tx * w
        }
        fill.reset()
        fill.moveTo(ox[0], oy[0])
        for (i in 1 until ox.size) fill.lineTo(ox[i], oy[i])
        fill.close()
        return Pts(ox, oy)
    }
}

fun animationsOff(context: Context): Boolean =
    runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)

/**
 * 구름 고양이 테마의 화면 그림.
 * full = true: 홈 화면 (달, 전차, 구름 바다, 고양이, 움직임과 누르기 반응)
 * full = false: 다른 화면 (글자가 잘 보이게 어두운 하늘과 별만)
 */
@Composable
fun CloudCatScene(modifier: Modifier = Modifier, full: Boolean = true) {
    val art = remember { CloudCatArt.get() }
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val still = inspection || remember { animationsOff(context) }
    val st = remember { CloudCatState().also { it.step(0f, SystemClock.uptimeMillis(), still) } }
    if (full && !still) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(st) {
            // 화면이 꺼지거나 다른 앱이 위에 있으면 멈춘다
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                st.wake(SystemClock.uptimeMillis())
                var last = withFrameNanos { it }
                while (true) {
                    delay(if (st.busy(SystemClock.uptimeMillis())) 28L else 250L)
                    val now = withFrameNanos { it }
                    val dt = ((now - last) / 1e9f).coerceIn(0f, 0.3f)
                    last = now
                    st.step(dt, SystemClock.uptimeMillis(), false)
                    st.frame.longValue++
                }
            }
        }
    }
    val density = LocalDensity.current
    val tm = rememberTextMeasurer()
    val sign = remember(tm, density) {
        tm.measure("달빛행", TextStyle(fontSize = with(density) { 7f.toSp() }, fontWeight = FontWeight.Bold, color = CC.ink))
    }
    val tapMod = if (full) {
        Modifier.pointerInput(still) {
            detectTapGestures { o ->
                val s = size.width / CloudCatArt.WIDTH
                val offY = size.height - CloudCatArt.HEIGHT * s
                st.tap(o.x / s + CloudCatArt.LEFT, (o.y - offY) / s + CloudCatArt.TOP, SystemClock.uptimeMillis(), still)
            }
        }
    } else Modifier
    Canvas(modifier.then(tapMod)) {
        st.frame.longValue
        drawCloudCat(art, st, full, sign)
    }
}

private fun DrawScope.drawCloudCatSky(art: CloudCatArt, st: CloudCatState, full: Boolean, s: Float, offY: Float) {
    val y0 = (10f - CloudCatArt.TOP) * s + offY
    val y1 = (320f - CloudCatArt.TOP) * s + offY
    if (full) {
        drawRect(Brush.verticalGradient(0f to CC.skyTop, 0.45f to CC.skyMid, 0.75f to CC.skyLight, 1f to CC.skyLow, startY = y0, endY = y1))
    } else {
        // 메뉴·해제 등 글자가 많은 화면: 홈처럼 밝은 보랏빛 하늘 (흰 글자가 읽히는 데까지만 밝게)
        drawRect(Brush.verticalGradient(0f to CC.skyTop, 1f to Color(0xFF8A7CD8), startY = y0, endY = size.height))
    }
}

private fun DrawScope.drawCloud(c: CloudCatArt.Cloud, st: CloudCatState) {
    val dx = sin(st.t * 6.283f / c.per + c.ph) * c.amp * (0.3f + 0.7f * st.energy)
    translate(dx, 0f) {
        drawPath(c.fill, c.fillColor, c.fillAlpha)
        clipPath(c.fill) { drawPath(c.hatch.path, c.hatch.color, c.hatch.alpha) }
        c.ink.draw(this, st.tick)
    }
}

private fun DrawScope.drawCloudCat(art: CloudCatArt, st: CloudCatState, full: Boolean, sign: TextLayoutResult) {
    val s = size.width / CloudCatArt.WIDTH
    val offY = size.height - CloudCatArt.HEIGHT * s
    drawCloudCatSky(art, st, full, s, offY)
    val e = st.energy
    val tw = 0.35f + 0.65f * e
    withTransform({
        translate(-CloudCatArt.LEFT * s, offY - CloudCatArt.TOP * s)
        scale(s, s, Offset.Zero)
    }) {
        // 하늘 빗금, 별
        drawPath(art.skyLight, Color.White, 0.13f)
        drawPath(art.skyDark, Color(0xFF2B1F6E), if (full) 0.1f else 0.14f)
        art.dots.forEach { d -> drawCircle(Color.White, d.r, Offset(d.x, d.y), (0.45f + 0.45f * sin(st.t * d.sp + d.ph)).coerceIn(0f, 1f)) }
        art.sparkles.forEach { sp ->
            val k = 1 + 0.28f * sin(st.t * sp.sp + sp.ph) * tw
            withTransform({
                translate(sp.x, sp.y)
                rotate(sin(st.t * 0.7f + sp.ph) * 8f * tw, Offset.Zero)
                scale(k, k, Offset.Zero)
            }) {
                // 글자 화면에서는 반짝이가 글자를 가리지 않게 옅게
                val a = (0.75f + 0.25f * sin(st.t * sp.sp + sp.ph)) * (if (full) 1f else 0.3f)
                drawPath(sp.fill, Color(0xFFFFF7D2), a)
                sp.ink.draw(this, st.tick, alphaMul = a)
            }
        }
        if (!full) {
            art.miniPlanets.forEach { p ->
                drawPath(p.fill, p.color)
                clipPath(p.fill) { drawPath(p.hatch, p.hatchColor, 0.42f) }
                p.inks.forEach { it.draw(this, st.tick) }
            }
            return@withTransform
        }

        // 별똥별
        st.meteors.forEach { m ->
            val p = m.age / m.life
            val fade = (if (p < 0.15f) p / 0.15f else 1 - max(0f, (p - 0.6f) / 0.4f)).coerceIn(0f, 1f)
            val len = 70f * min(1f, m.age * 4)
            val sp = hypot(m.vx, m.vy)
            val dx = -m.vx / sp; val dy = -m.vy / sp; val nx = -dy; val ny = dx
            val tail = Path()
            val rx = FloatArray(13); val ry = FloatArray(13)
            for (i in 0..12) {
                val t = i / 12f
                val w = 3.2f * (1 - t).pow(1.3f) + 0.15f
                val x = m.x + dx * len * t; val y = m.y + dy * len * t
                if (i == 0) tail.moveTo(x + nx * w / 2, y + ny * w / 2) else tail.lineTo(x + nx * w / 2, y + ny * w / 2)
                rx[i] = x - nx * w / 2; ry[i] = y - ny * w / 2
            }
            for (i in 12 downTo 0) tail.lineTo(rx[i], ry[i])
            tail.close()
            drawPath(tail, Color(0xFFFFF4C2), fade)
            drawPath(tail, CC.ink, fade * 0.55f, Stroke(0.6f))
            withTransform({ translate(m.x, m.y); rotate(m.age * 200f, Offset.Zero); scale(4.5f, 4.5f, Offset.Zero) }) {
                drawPath(art.starPath, Color(0xFFFFFBE6), fade)
                drawPath(art.starPath, CC.ink, fade, Stroke(0.5f / 4.5f))
            }
        }

        // 달무리, 달, 행성
        val haloR = 48f + 4f * sin(st.t * 1.3f)
        drawCircle(
            Brush.radialGradient(0f to CC.moon.copy(alpha = 0.75f), 0.45f to CC.moon.copy(alpha = 0.75f), 1f to CC.moon.copy(alpha = 0f), center = Offset(80f, 112f), radius = haloR),
            haloR, Offset(80f, 112f), alpha = 0.75f + 0.25f * sin(st.t * 1.3f),
        )
        drawPath(art.moonFill, CC.moon)
        clipPath(art.moonFill) { drawPath(art.moonHatch, Color(0xFFD9A93A), 0.48f) }
        art.moonInk.forEach { it.draw(this, st.tick) }
        art.ringBack.draw(this, st.tick)
        drawPath(art.planetFill, CC.planet)
        clipPath(art.planetFill) { drawPath(art.planetHatch, Color(0xFFC25E63), 0.42f) }
        art.planetInk.forEach { it.draw(this, st.tick) }

        // 먼 구름 + 지평선 안개
        drawRect(
            Brush.verticalGradient(0f to Color.White.copy(alpha = 0f), 0.55f to Color.White.copy(alpha = 0.5f), 1f to Color.White.copy(alpha = 0f), startY = 258f, endY = 322f),
            Offset(-20f, 258f), Size(424f, 64f),
        )
        drawCloud(art.far, st)
        art.mists.forEach { m ->
            val x = ((m.x0 + st.t * m.v) % 560f) - 90f
            drawOval(
                Brush.radialGradient(0f to Color.White.copy(alpha = 0.38f), 1f to Color.White.copy(alpha = 0f), center = Offset(x, m.cy), radius = m.rx),
                Offset(x - m.rx, m.cy - m.ry * 2), Size(m.rx * 2, m.ry * 4),
            )
        }
        art.mid.forEach { drawCloud(it, st) }
        art.sea.forEach { drawCloud(it, st) }

        // 레일과 전차
        art.tramStatic.forEach { it.draw(this, st.tick) }
        translate(st.tramX, st.tramY) {
            drawOval(CC.ink, Offset(150f, 354.5f), Size(80f, 9f), alpha = 0.22f)
            art.tramInkBack.forEach { it.draw(this, st.tick) }
            drawPath(art.tramBody, CC.tram)
            clipPath(art.tramBody) { drawPath(art.tramHatch, Color(0xFFC9A57A), 0.42f) }
            drawRect(CC.band, Offset(155.5f, 334f), Size(69f, 8f))
            art.windows.forEach { w ->
                val fl = (0.78f + 0.22f * noise1(st.t * 4.0 + w.ph).toFloat() - (if (noise1(st.t * 1.3 + w.ph * 3) > 0.86) 0.25f else 0f)).coerceIn(0f, 1f)
                drawCircle(
                    Brush.radialGradient(0f to CC.window.copy(alpha = 0.85f), 1f to CC.window.copy(alpha = 0f), center = Offset(w.cx, w.cy), radius = 22f),
                    22f, Offset(w.cx, w.cy), alpha = 0.5f + 0.5f * fl,
                )
                drawPath(w.fill, CC.window, fl)
                w.ink.draw(this, st.tick)
            }
            drawPath(art.signFill, Color(0xFFFFF9EC))
            drawText(sign, topLeft = Offset(190f - sign.size.width / 2f, 296.5f - sign.size.height / 2f))
            drawCircle(
                Brush.radialGradient(0f to CC.window.copy(alpha = 0.85f), 1f to CC.window.copy(alpha = 0f), center = Offset(190f, 347f), radius = 9f),
                9f, Offset(190f, 347f),
            )
            drawCircle(Color(0xFFFFF3B0), 3.2f, Offset(190f, 347f))
            drawPath(art.bogie, CC.ink, 0.85f)
            art.tramInkFront.forEach { it.draw(this, st.tick) }
        }

        // 가까운 구름
        art.near.forEach { drawCloud(it, st) }

        // 고양이
        translate(CloudCatArt.CAT_X, CloudCatArt.CAT_Y + st.jumpY) { drawCat(art, st) }

        // 효과: 반짝이, 김, 하트
        st.parts.forEach { q ->
            if (q.age < 0f) return@forEach
            val p = (q.age / q.life).coerceIn(0f, 1f)
            when (q.kind) {
                0 -> withTransform({ translate(q.x, q.y); rotate(q.rot, Offset.Zero); scale(q.size, q.size, Offset.Zero) }) {
                    drawPath(art.starPath, Color(0xFFFFF3C4), 1 - p)
                    drawPath(art.starPath, CC.ink, 1 - p, Stroke(0.5f / q.size))
                }
                1 -> drawCircle(Color.White, 3f + p * 12f, Offset(q.x, q.y), 0.9f * (1 - p), Stroke(1.6f))
                else -> {
                    val k = 0.6f + 0.6f * min(1f, p * 4)
                    withTransform({ translate(q.x + sin(q.age * 5 + q.ph) * 4f, q.y); scale(k, k, Offset.Zero) }) {
                        drawPath(art.heartPath, CC.planet, 1 - p)
                        drawPath(art.heartPath, CC.ink, 1 - p, Stroke(0.8f / k))
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCat(art: CloudCatArt, st: CloudCatState) {
    // 몸 (숨쉬기)
    withTransform({ scale(1f, 1f + 0.02f * st.breath, Offset(0f, 79f)) }) {
        drawPath(art.catBody, CC.cat)
        clipPath(art.catBody) {
            drawPath(art.catGinger, CC.ginger, 0.48f)
            drawPath(art.catShade, Color(0xFFC9BBD6), 0.45f)
        }
        art.catBodyInk.forEach { it.draw(this, st.tick) }
        drawPath(st.scarfFill, CC.band)
        art.scarfInk.pts = st.scarfPts
        art.scarfInk.draw(this, st.tick, dynamic = true)
    }
    // 머리
    withTransform({
        translate(0f, -0.6f * st.breath)
        rotate(st.headA, Offset(92f, 46f))
    }) {
        rotate(st.earL, Offset(90f, 28f)) {
            drawPath(art.earLFill, CC.cat)
            art.earLInk.draw(this, st.tick)
        }
        rotate(st.earR, Offset(112f, 22f)) {
            drawPath(art.earRFill, CC.cat)
            art.earRInk.draw(this, st.tick)
        }
        drawPath(art.earLeaves, CC.planet, 0.5f)
        drawPath(art.headFill, CC.cat)
        clipPath(art.headFill) { drawPath(art.headGinger, CC.ginger, 0.5f) }
        art.headInk.forEach { it.draw(this, st.tick) }
        drawOval(CC.planet, Offset(112f, 40.6f), Size(8f, 4.8f), alpha = 0.45f)
        val ry = 2.7f * (1 - 0.88f * st.lid)
        drawOval(CC.ink, Offset(98f + st.eyeDX - 2.1f, 33f + st.eyeDY - ry), Size(4.2f, ry * 2))
        drawOval(CC.ink, Offset(111f + st.eyeDX - 2.1f, 32.5f + st.eyeDY - ry), Size(4.2f, ry * 2))
        drawPath(art.nose, Color(0xFFE58A8A))
    }
    // 목도리
    drawPath(art.bandFill, CC.band)
    art.bandInk.forEach { it.draw(this, st.tick) }
    drawCircle(CC.band, 3f, Offset(92f, 52f))
    // 꼬리 (앞쪽)
    drawPath(st.tailFill, CC.cat)
    art.tailInk.pts = st.tailPts
    art.tailInk.draw(this, st.tick, dynamic = true)
}
